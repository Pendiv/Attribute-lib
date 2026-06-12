package DIV.attributelib.core;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ModifierHandle;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * エンティティ1体分の属性状態。
 *
 * <ul>
 *   <li><b>書き込み透過</b>: 永続データ(base / persistent モディファイア)は変更のたびに
 *       PDC へ即書き込む。アンロード時の保存処理が不要になり、保存漏れによる
 *       データ消失が構造的に起きない。</li>
 *   <li><b>遅延期限切れ</b>: 時限モディファイアは読み取り時にワールドの gameTime と比較して
 *       除去する。スケジューラを使わないため、再起動・アンロードを跨いでも正確。</li>
 *   <li><b>遅延再計算</b>: 最終値は属性ごとにキャッシュし、変更時にその属性だけ無効化する。</li>
 * </ul>
 *
 * スレッド安全性はない(attributelib 全体がメインスレッド専用)。
 */
final class EntityAttributes {

    private final LivingEntity entity;
    private final NamespacedKey baseKey;
    private final NamespacedKey modifiersKey;
    private final Logger logger;

    private final Map<NamespacedKey, Double> base = new LinkedHashMap<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private final Map<NamespacedKey, Double> finalCache = new HashMap<>();

    EntityAttributes(LivingEntity entity, NamespacedKey baseKey, NamespacedKey modifiersKey, Logger logger) {
        this.entity = entity;
        this.baseKey = baseKey;
        this.modifiersKey = modifiersKey;
        this.logger = logger;
        load();
    }

    LivingEntity entity() {
        return entity;
    }

    // ---- 読み取り ----

    double get(AttributeType type) {
        sweepExpired();
        Double cached = finalCache.get(type.key());
        if (cached != null) {
            return cached;
        }
        double baseValue = base.getOrDefault(type.key(), type.defaultValue());
        double result = Calculator.compute(type, baseValue, modifiersFor(type.key()));
        finalCache.put(type.key(), result);
        return result;
    }

    double getBase(AttributeType type) {
        return base.getOrDefault(type.key(), type.defaultValue());
    }

    /** デバッグ用の読み取りビュー(コピー)。 */
    Map<NamespacedKey, Double> baseView() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(base));
    }

    /** デバッグ用の読み取りビュー(コピー)。期限切れ掃除後の有効分のみ。 */
    List<Modifier> modifierView() {
        sweepExpired();
        return List.copyOf(modifiers);
    }

    // ---- 変更 ----

    void setBase(AttributeType type, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("base が有限値ではありません: " + value);
        }
        base.put(type.key(), value);
        finalCache.remove(type.key());
        saveBase();
    }

    void resetBase(AttributeType type) {
        if (base.remove(type.key()) != null) {
            finalCache.remove(type.key());
            saveBase();
        }
    }

    ModifierHandle add(Modifier modifier) {
        modifiers.add(modifier);
        finalCache.remove(modifier.attribute());
        if (modifier.persistent()) {
            saveModifiers();
        }
        return new Handle(modifier);
    }

    void removeBySource(String sourceId) {
        boolean persistentRemoved = false;
        Iterator<Modifier> it = modifiers.iterator();
        while (it.hasNext()) {
            Modifier m = it.next();
            if (m.sourceId().equals(sourceId)) {
                it.remove();
                finalCache.remove(m.attribute());
                persistentRemoved |= m.persistent();
            }
        }
        if (persistentRemoved) {
            saveModifiers();
        }
    }

    // ---- 内部処理 ----

    private long now() {
        return entity.getWorld().getGameTime();
    }

    private void sweepExpired() {
        long now = now();
        boolean persistentRemoved = false;
        Iterator<Modifier> it = modifiers.iterator();
        while (it.hasNext()) {
            Modifier m = it.next();
            if (m.isExpired(now)) {
                it.remove();
                finalCache.remove(m.attribute());
                persistentRemoved |= m.persistent();
            }
        }
        if (persistentRemoved) {
            saveModifiers();
        }
    }

    private List<Modifier> modifiersFor(NamespacedKey attribute) {
        List<Modifier> result = new ArrayList<>();
        for (Modifier m : modifiers) {
            if (m.attribute().equals(attribute)) {
                result.add(m);
            }
        }
        return result;
    }

    private boolean removeInstance(Modifier modifier) {
        Iterator<Modifier> it = modifiers.iterator();
        while (it.hasNext()) {
            if (it.next() == modifier) {
                it.remove();
                finalCache.remove(modifier.attribute());
                return true;
            }
        }
        return false;
    }

    private boolean containsInstance(Modifier modifier) {
        for (Modifier m : modifiers) {
            if (m == modifier) {
                return true;
            }
        }
        return false;
    }

    // ---- PDC 入出力 ----

    private void load() {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        boolean needRewrite = false;

        List<String> baseLines = pdc.get(baseKey, PersistentDataType.LIST.strings());
        if (baseLines != null) {
            for (String line : baseLines) {
                Codec.BaseEntry entry = Codec.parseBase(line);
                if (entry == null) {
                    logger.warning("壊れた base 行を破棄します (" + entity.getUniqueId() + "): " + line);
                    needRewrite = true;
                    continue;
                }
                base.put(entry.attribute(), entry.value());
            }
        }

        List<String> modifierLines = pdc.get(modifiersKey, PersistentDataType.LIST.strings());
        if (modifierLines != null) {
            long now = now();
            for (String line : modifierLines) {
                Modifier m = Codec.parseModifier(line);
                if (m == null) {
                    logger.warning("壊れた modifier 行を破棄します (" + entity.getUniqueId() + "): " + line);
                    needRewrite = true;
                    continue;
                }
                if (m.isExpired(now)) {
                    needRewrite = true;
                    continue;
                }
                modifiers.add(m);
            }
        }

        if (needRewrite) {
            saveBase();
            saveModifiers();
        }
    }

    private void saveBase() {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (base.isEmpty()) {
            pdc.remove(baseKey);
            return;
        }
        List<String> lines = new ArrayList<>(base.size());
        base.forEach((key, value) -> lines.add(Codec.formatBase(key, value)));
        pdc.set(baseKey, PersistentDataType.LIST.strings(), lines);
    }

    private void saveModifiers() {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        List<String> lines = new ArrayList<>();
        for (Modifier m : modifiers) {
            if (m.persistent()) {
                lines.add(Codec.formatModifier(m));
            }
        }
        if (lines.isEmpty()) {
            pdc.remove(modifiersKey);
        } else {
            pdc.set(modifiersKey, PersistentDataType.LIST.strings(), lines);
        }
    }

    // ---- チケット ----

    private final class Handle implements ModifierHandle {

        private final Modifier modifier;
        private boolean removed;

        private Handle(Modifier modifier) {
            this.modifier = modifier;
        }

        @Override
        public void remove() {
            AttributeEngine.checkMainThread();
            if (removed) {
                return;
            }
            removed = true;
            if (removeInstance(modifier) && modifier.persistent()) {
                saveModifiers();
            }
        }

        @Override
        public boolean isActive() {
            AttributeEngine.checkMainThread();
            return !removed && containsInstance(modifier) && !modifier.isExpired(now());
        }
    }
}
