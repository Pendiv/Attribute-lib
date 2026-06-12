package DIV.attributelib.api;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import java.util.function.Predicate;

/**
 * モディファイアの適用条件。「夜間のみ」「水中のみ」など、成立している間だけ
 * モディファイアが効く。
 *
 * <p>述語(test)は保存できないため、PDC にはキーだけが保存され、評価時に
 * レジストリから引き直される。提供元プラグインが抜けて未解決になった条件は
 * 「不成立」として扱われる(効果が消える側に倒す)。</p>
 *
 * @param key         条件ID(登録元プラグインの名前空間)
 * @param displayName lore 表示などで使う表示名
 * @param test        成立判定(メインスレッドで呼ばれる。重い処理を書かないこと)
 */
public record Condition(
        NamespacedKey key,
        Component displayName,
        Predicate<LivingEntity> test
) {
    public Condition {
        if (key == null || test == null) {
            throw new IllegalArgumentException("key / test は必須です");
        }
        if (displayName == null) {
            displayName = Component.text(key.getKey());
        }
    }
}
