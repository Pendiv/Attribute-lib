package DIV.attributelib.api;

import org.bukkit.NamespacedKey;

/**
 * ダメージ元素(物理・魔法・炎・雷など)。登録すると与ダメ倍率と軽減割合の
 * 属性ペアが自動で作られ、ダメージパイプラインに組み込まれる。
 *
 * @param key             元素ID(登録元プラグインの名前空間)
 * @param damageAttribute 攻撃側の与ダメ倍率属性({@code <id>_damage}、既定 1.0)
 * @param resistAttribute 防御側の軽減割合属性({@code <id>_resist}、既定 0.0、1.0 で無効化、負で脆弱)
 * @param flatAttribute   攻撃側の実数値加算属性({@code <id>_damage_flat}、既定 0.0)。
 *                        base に上乗せされ、会心・%・防具計算が乗る
 */
public record DamageElement(
        NamespacedKey key,
        AttributeType damageAttribute,
        AttributeType resistAttribute,
        AttributeType flatAttribute
) {
}
