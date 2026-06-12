package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import DIV.attributelib.damage.ElementRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

/**
 * ダメージ元素の登録・参照 API。
 *
 * <pre>{@code
 * // 新元素の登録(void_damage / void_resist 属性が自動で生える)
 * DamageElement VOID = DamageElements.register(this, "void");
 *
 * // 自前のデータパックダメージタイプを元素に紐付け(被弾時に自動判定される)
 * DamageElements.mapDamageType(new NamespacedKey("myplugin", "void_burst"), VOID);
 *
 * // 紐付け済みタイプが無くても「宣誓」で撃てる
 * DamageLib.deal(VOID, attacker, victim, 8);
 *
 * // 以後は普通の属性として扱える
 * Attributes.add(player, VOID.resistAttribute(), "myplugin:ring", Operation.ADD, 0.3);
 * }</pre>
 *
 * <p>attributelib 同梱の標準元素(使うかは利用側の自由。初期値で存在するだけ):</p>
 * <ul>
 *   <li>{@link #PHYSICAL} — バニラの近接・投射ダメージに紐付け済み</li>
 *   <li>{@link #MAGIC} — バニラの magic / indirect_magic に紐付け済み</li>
 *   <li>{@link #FIRE} — バニラの炎系(in_fire / on_fire / lava / fireball 等)に紐付け済み</li>
 *   <li>{@link #LIGHTNING} — バニラの雷(lightning_bolt)に紐付け済み</li>
 * </ul>
 */
public final class DamageElements {

    /** 物理。physical_damage / physical_resist。 */
    public static DamageElement PHYSICAL;
    /** 魔法。magic_damage / magic_resist。 */
    public static DamageElement MAGIC;
    /** 炎。fire_damage / fire_resist。バニラの炎ダメージにも軽減が効く。 */
    public static DamageElement FIRE;
    /** 雷。lightning_damage / lightning_resist。バニラの雷ダメージにも軽減が効く。 */
    public static DamageElement LIGHTNING;

    private DamageElements() {
    }

    private static ElementRegistry registry() {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.elements();
    }

    /**
     * 元素を登録する。{@code <id>_damage}(与ダメ倍率)と {@code <id>_resist}(軽減割合)の
     * 属性ペアが自動登録され、ダメージパイプラインで他の元素と同様に扱われる。
     */
    public static DamageElement register(Plugin plugin, String id) {
        return registry().register(plugin, id);
    }

    /** 登録と同時にダメージタイプを紐付けるショートカット。 */
    public static DamageElement register(Plugin plugin, String id, NamespacedKey... damageTypeKeys) {
        DamageElement element = registry().register(plugin, id);
        for (NamespacedKey key : damageTypeKeys) {
            registry().mapDamageType(key, element);
        }
        return element;
    }

    /** ダメージタイプ → 元素の対応を追加・上書きする(バニラ・他プラグインのタイプも可)。 */
    public static void mapDamageType(NamespacedKey damageTypeKey, DamageElement element) {
        registry().mapDamageType(damageTypeKey, element);
    }

    /** 登録済み元素を ID で引く。未登録なら null。 */
    public static DamageElement byKey(NamespacedKey key) {
        return registry().byKey(key);
    }

    /** 登録済み元素の一覧(登録順)。 */
    public static Collection<DamageElement> registered() {
        return registry().registered();
    }

    /** attributelib 内部用。onEnable で1回だけ呼ばれる。 */
    public static void initBuiltins(DamageElement physical, DamageElement magic,
                                    DamageElement fire, DamageElement lightning) {
        if (PHYSICAL != null) {
            throw new IllegalStateException("標準元素は初期化済みです");
        }
        PHYSICAL = physical;
        MAGIC = magic;
        FIRE = fire;
        LIGHTNING = lightning;
    }
}
