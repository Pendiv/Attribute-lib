package DIV.attributelib.damage;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import io.papermc.paper.registry.tag.Tag;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;

import java.util.ArrayList;
import java.util.List;

/**
 * attributelib が同梱するカスタムダメージタイプの解決。
 * ダメージタイプ → 元素の対応は {@link ElementRegistry} が持つ。
 */
public final class DamageTypes {

    /** 物理貫通。バニラ防具を素通りし、物理軽減%は効く。 */
    public static final NamespacedKey PHYSICAL_PIERCE = new NamespacedKey("attributelib", "physical_pierce");
    /** 魔法。防具素通り、魔法軽減%が効く。 */
    public static final NamespacedKey MAGIC = new NamespacedKey("attributelib", "magic");
    /** 確定ダメージ。全防御・無敵フレーム素通り、パイプラインも触れない。 */
    public static final NamespacedKey TRUE_DAMAGE = new NamespacedKey("attributelib", "true_damage");
    /** 宣誓ダメージの汎用キャリア(防具素通り)。元素は宣誓マーカーで決まる。 */
    public static final NamespacedKey ELEMENTAL = new NamespacedKey("attributelib", "elemental");

    /** ダメージタイプキー → bypasses_armor 判定のキャッシュ(メインスレッド専用)。 */
    private static final java.util.Map<NamespacedKey, Boolean> BYPASSES_ARMOR_CACHE = new java.util.HashMap<>();

    /** 解決済み DamageType のキャッシュ。ダメージタイプは起動時固定(リロード不可)なので安全。 */
    private static final java.util.Map<NamespacedKey, DamageType> RESOLVE_CACHE = new java.util.HashMap<>();

    private DamageTypes() {
    }

    /** このダメージが確定ダメージ(パイプライン不干渉)か。 */
    public static boolean isTrueDamage(DamageSource source) {
        return TRUE_DAMAGE.equals(source.getDamageType().getKey());
    }

    /** レジストリから DamageType を引く。データパック未読込(初回起動)なら null。 */
    public static DamageType resolve(NamespacedKey key) {
        DamageType cached = RESOLVE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        DamageType resolved = registry().get(key);
        if (resolved != null) {
            // null(未登録)はキャッシュしない: 初回起動時の「再起動後に有効」を妨げないため
            RESOLVE_CACHE.put(key, resolved);
        }
        return resolved;
    }

    /** 同梱タイプのうちレジストリに存在しないものの一覧(空なら配備完了)。 */
    public static List<NamespacedKey> missingTypes() {
        List<NamespacedKey> missing = new ArrayList<>();
        for (NamespacedKey key : List.of(PHYSICAL_PIERCE, MAGIC, TRUE_DAMAGE, ELEMENTAL)) {
            if (resolve(key) == null) {
                missing.add(key);
            }
        }
        return missing;
    }

    /**
     * このダメージがバニラ防具計算を素通りしたか(bypasses_armor タグ)。
     * タグ照会はレジストリアクセスを伴うため、ダメージタイプキーごとにキャッシュする
     * (タグはデータパックリロードまで不変。リロード時は {@link #invalidateCaches})。
     */
    public static boolean bypassesArmor(DamageSource source) {
        NamespacedKey typeKey = source.getDamageType().getKey();
        Boolean cached = BYPASSES_ARMOR_CACHE.get(typeKey);
        if (cached != null) {
            return cached;
        }
        Tag<DamageType> tag = registry().getTag(DamageTypeTagKeys.BYPASSES_ARMOR);
        boolean result = tag.contains(TypedKey.create(RegistryKey.DAMAGE_TYPE, Key.key(typeKey.toString())));
        BYPASSES_ARMOR_CACHE.put(typeKey, result);
        return result;
    }

    /** データパックリロード(/reload)でタグが変わりうるため、キャッシュを破棄する。 */
    public static void invalidateCaches() {
        BYPASSES_ARMOR_CACHE.clear();
    }

    private static Registry<DamageType> registry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
    }
}
