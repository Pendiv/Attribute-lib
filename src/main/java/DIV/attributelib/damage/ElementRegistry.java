package DIV.attributelib.damage;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.DamageElement;
import DIV.attributelib.core.AttributeEngine;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ダメージ元素のレジストリ(内部中枢)。利用側は
 * {@link DIV.attributelib.api.DamageElements} ファサードを使うこと。
 */
public final class ElementRegistry {

    private final AttributeEngine engine;
    private final Map<NamespacedKey, DamageElement> elements = new LinkedHashMap<>();
    private final Map<NamespacedKey, DamageElement> byDamageType = new HashMap<>();

    public ElementRegistry(AttributeEngine engine) {
        this.engine = engine;
    }

    /**
     * 元素を登録し、{@code <id>_damage}(与ダメ倍率、既定1.0)・
     * {@code <id>_resist}(軽減割合、既定0.0)・{@code <id>_damage_flat}(実数値加算、既定0.0)の
     * 属性を自動登録する。
     */
    public DamageElement register(Plugin owner, String id) {
        return register(owner, id, null, null);
    }

    /** 表示名付き登録。属性ペアはどちらも百分率表示になる。 */
    public DamageElement register(Plugin owner, String id,
                                  net.kyori.adventure.text.Component damageDisplayName,
                                  net.kyori.adventure.text.Component resistDisplayName) {
        AttributeEngine.checkMainThread();
        NamespacedKey key = new NamespacedKey(owner, id);
        if (elements.containsKey(key)) {
            throw new IllegalStateException("元素が二重登録されました: " + key);
        }
        AttributeType damage = engine.register(owner, id + "_damage", 1.0, 0.0, Double.MAX_VALUE,
                damageDisplayName, true);
        AttributeType resist = engine.register(owner, id + "_resist", 0.0, -Double.MAX_VALUE, 1.0,
                resistDisplayName, true);
        AttributeType flat = engine.register(owner, id + "_damage_flat", 0.0, 0.0, Double.MAX_VALUE,
                damageDisplayName, false);
        DamageElement element = new DamageElement(key, damage, resist, flat);
        elements.put(key, element);
        return element;
    }

    /** ダメージタイプ → 元素の対応を追加・上書きする。 */
    public void mapDamageType(NamespacedKey damageTypeKey, DamageElement element) {
        AttributeEngine.checkMainThread();
        byDamageType.put(damageTypeKey, element);
    }

    /** ダメージタイプに対応する元素。未対応なら null(=無属性)。 */
    public DamageElement elementOf(NamespacedKey damageTypeKey) {
        return byDamageType.get(damageTypeKey);
    }

    /** 登録済み元素を ID で引く。未登録なら null。 */
    public DamageElement byKey(NamespacedKey key) {
        return elements.get(key);
    }

    /** 登録済み元素の一覧(登録順)。 */
    public Collection<DamageElement> registered() {
        return Collections.unmodifiableCollection(elements.values());
    }
}
