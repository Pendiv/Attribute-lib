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

    private DamageTypes() {
    }

    /** このダメージが確定ダメージ(パイプライン不干渉)か。 */
    public static boolean isTrueDamage(DamageSource source) {
        return TRUE_DAMAGE.equals(source.getDamageType().getKey());
    }

    /** レジストリから DamageType を引く。データパック未読込(初回起動)なら null。 */
    public static DamageType resolve(NamespacedKey key) {
        return registry().get(key);
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

    /** このダメージがバニラ防具計算を素通りしたか(bypasses_armor タグ)。 */
    public static boolean bypassesArmor(DamageSource source) {
        Tag<DamageType> tag = registry().getTag(DamageTypeTagKeys.BYPASSES_ARMOR);
        TypedKey<DamageType> key = TypedKey.create(RegistryKey.DAMAGE_TYPE,
                Key.key(source.getDamageType().getKey().toString()));
        return tag.contains(key);
    }

    private static Registry<DamageType> registry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE);
    }
}
