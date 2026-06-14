package DIV.attributelib.core;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;

/**
 * バニラ属性のハード上限({@code RangedAttribute.maxValue})をリフレクションで引き上げる。
 *
 * <p>Mojang は最大体力 1024・防御力 30 のような上限を NMS にハードコードしており、
 * Bukkit API にもデータパックにもそれを変える口が無い。{@code AttributeInstance.getValue()} は
 * 毎回 {@code maxValue} を見てクランプするため、ここを書き換えれば既存エンティティも
 * 以降は新上限で再評価される(再生成不要)。</p>
 *
 * <p><b>堅牢性</b>: マッピング差異・モジュールアクセス制限・将来の改名で落ちないよう、
 * フィールド名に依存せず「継承階層で最大値を保持する {@code double} フィールド」を
 * {@code maxValue} とみなし(RangedAttribute は min ≤ default ≤ max なので最大値が常に max)、
 * いかなる失敗も握りつぶして上限据え置き + 警告ログに留める。</p>
 */
public final class VanillaCaps {

    private VanillaCaps() {
    }

    /**
     * config の {@code caps:} セクション(属性キー → 新しい上限)を一括適用する。
     * キーは属性のミニクラフトキー({@code max-health} / {@code max_health} / {@code armor} など)。
     */
    public static void raiseFromConfig(Plugin plugin, Map<String, Object> caps) {
        if (caps == null || caps.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : caps.entrySet()) {
            String name = entry.getKey();
            double newMax = toDouble(entry.getValue());
            if (!Double.isFinite(newMax)) {
                plugin.getLogger().warning("caps." + name + " の値が数値ではありません: " + entry.getValue());
                continue;
            }
            Attribute attribute = resolve(name);
            if (attribute == null) {
                plugin.getLogger().warning("caps の属性キーを解決できません(無視): " + name);
                continue;
            }
            raise(plugin, attribute, newMax);
        }
    }

    /**
     * 指定属性のハード上限を {@code newMax} へ引き上げる。現上限以下なら何もしない。
     *
     * @return 実際に引き上げたら true(不要・失敗なら false)
     */
    public static boolean raise(Plugin plugin, Attribute attribute, double newMax) {
        try {
            Object nms = toNms(attribute);
            if (nms == null) {
                plugin.getLogger().warning("属性の NMS 表現を取得できませんでした(上限据え置き): " + attribute.getKey());
                return false;
            }
            Field field = maxValueField(nms);
            if (field == null) {
                plugin.getLogger().warning("maxValue フィールドを特定できませんでした(上限据え置き): " + attribute.getKey());
                return false;
            }
            double current = field.getDouble(nms);
            if (newMax <= current) {
                return false; // 既に十分高い
            }
            field.setDouble(nms, newMax);
            plugin.getLogger().info("属性上限を解放: " + attribute.getKey()
                    + " " + trim(current) + " → " + trim(newMax));
            return true;
        } catch (Throwable t) {
            // NMS 実装差異・モジュールアクセス等で絶対に落とさない
            plugin.getLogger().warning("属性上限の解放に失敗(" + attribute.getKey() + ", 据え置き): " + t);
            return false;
        }
    }

    private static @Nullable Attribute resolve(String name) {
        String path = name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        int colon = path.indexOf(':');
        NamespacedKey key = colon >= 0 ? NamespacedKey.fromString(path) : NamespacedKey.minecraft(path);
        if (key == null) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(key);
    }

    /** CraftAttribute 経由で NMS の RangedAttribute インスタンスを取り出す(Holder なら展開)。 */
    private static @Nullable Object toNms(Attribute attribute) throws Exception {
        String pkg = Bukkit.getServer().getClass().getPackageName(); // バージョン付き/無し両対応
        Class<?> craftAttribute = Class.forName(pkg + ".attribute.CraftAttribute");
        // 既知メソッド名を優先し、ダメなら Attribute 1引数の static メソッドを総当たり
        for (String method : new String[]{"bukkitToMinecraftHolder", "bukkitToMinecraft"}) {
            try {
                Method m = craftAttribute.getMethod(method, Attribute.class);
                Object unwrapped = unwrapHolder(m.invoke(null, attribute));
                if (unwrapped != null) {
                    return unwrapped;
                }
            } catch (NoSuchMethodException ignored) {
                // 次の候補へ
            }
        }
        for (Method m : craftAttribute.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1) {
                continue;
            }
            if (!m.getParameterTypes()[0].isAssignableFrom(Attribute.class)) {
                continue;
            }
            Object unwrapped = unwrapHolder(m.invoke(null, attribute));
            if (unwrapped != null) {
                return unwrapped;
            }
        }
        return null;
    }

    /** Holder(value() を持つ)なら中身を返す。そうでなければそのまま。 */
    private static @Nullable Object unwrapHolder(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            Method value = obj.getClass().getMethod("value");
            Object inner = value.invoke(obj);
            return inner != null ? inner : obj;
        } catch (Exception e) {
            return obj; // Holder ではない
        }
    }

    /**
     * 継承階層の中で最大値を保持する非 static の {@code double} フィールドを返す。
     * RangedAttribute は min ≤ default ≤ max を満たすため、最大値のフィールドが maxValue。
     */
    private static @Nullable Field maxValueField(Object nms) {
        Field best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Class<?> c = nms.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != double.class || Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    double v = f.getDouble(nms);
                    if (v > bestValue) {
                        bestValue = v;
                        best = f;
                    }
                } catch (Exception ignored) {
                    // アクセス不可フィールドは無視
                }
            }
        }
        return best;
    }

    private static double toDouble(@Nullable Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private static String trim(double v) {
        return v == Math.rint(v) && !Double.isInfinite(v) ? Long.toString((long) v) : Double.toString(v);
    }
}
