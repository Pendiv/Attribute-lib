package DIV.attributelib.listener;

import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.damage.DamageTypes;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;

/**
 * エンティティ状態キャッシュの掃除。
 * {@link EntityRemoveEvent} は死亡・デスポーン・/kill・チャンクアンロード・退出を網羅する。
 * 永続データは書き込み透過で常に PDC に反映済みのため、ここでの保存処理は不要
 * (= 「remove イベント中に保存しようとして間に合わない」系のデータ消失が起きない)。
 */
public final class CleanupListener implements Listener {

    private final AttributeEngine engine;

    public CleanupListener(AttributeEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        engine.evict(event.getEntity().getUniqueId());
    }

    /** /reload でデータパックのタグが変わりうるため、ダメージタイプ系キャッシュを破棄する。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        DamageTypes.invalidateCaches();
    }
}
