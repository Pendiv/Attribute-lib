package DIV.attributelib.damage;

import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.core.AttributeEngine;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * 回復倍率(heal_multiplier)の適用リスナー。
 * EnhancedMobs の HealMultiplier(回復擬似属性)の吸収先。0 で回復不可。
 *
 * <p>注意(実機確認済み): Paper 26.1.2 の {@code Damageable#heal()} API は
 * EntityRegainHealthEvent を<b>発火しない</b>ため、この倍率はかからない。
 * 倍率が効くのは自然回復・ポーション・金リンゴ等のバニラ回復経路のみ。
 * プラグインからの回復に倍率を効かせたい場合は heal() ではなく
 * イベントを発火させる経路を使うこと。</p>
 */
public final class HealListener implements Listener {

    private final AttributeEngine engine;

    public HealListener(AttributeEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        // 自然回復は高頻度で発火するため、データの無いエンティティは状態を生成せず素通し
        if (!engine.hasData(living)) {
            return;
        }
        double multiplier = engine.get(living, StandardAttributes.HEAL_MULTIPLIER);
        if (multiplier == 1.0) {
            return;
        }
        double amount = event.getAmount() * multiplier;
        if (amount <= 0) {
            event.setCancelled(true);
            return;
        }
        event.setAmount(amount);
    }
}
