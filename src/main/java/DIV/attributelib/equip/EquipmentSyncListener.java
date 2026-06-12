package DIV.attributelib.equip;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.ItemAttributes;
import DIV.attributelib.api.ItemModifier;
import DIV.attributelib.core.AttributeEngine;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * 装備同期(層3)。アイテムに書かれたカスタム属性を、装備中だけエンティティへ反映する。
 *
 * <ul>
 *   <li>反映はスロットごとの sourceId({@code attributelib:equip/<slot>})の
 *       REPLACE 運用(全剥がし → 付け直し)。</li>
 *   <li>モディファイアは全て <b>transient</b>(エンティティ PDC に書かない)。
 *       アンロードで消え、ロード時に装備から再生成されるため、
 *       外したのに残る「ゴースト強化」が構造的に起きない(企画書 §8-4)。</li>
 *   <li>バニラ属性分はアイテムの attribute_modifiers コンポーネントが担当するため
 *       ここでは扱わない(同期はバニラが行う)。</li>
 * </ul>
 */
public final class EquipmentSyncListener implements Listener {

    private final AttributeEngine engine;

    public EquipmentSyncListener(AttributeEngine engine) {
        this.engine = engine;
    }

    /** スロットに対応する装備モディファイアの sourceId。 */
    public static String sourceFor(EquipmentSlot slot) {
        return "attributelib:equip/" + slot.name().toLowerCase(Locale.ROOT);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        event.getEquipmentChanges().forEach((slot, change) -> {
            // 旧装備・新装備のどちらにもカスタム属性が無ければエンジンに触れない。
            // 判定は meta を複製しない PDC ビューの has のみ(装備付き mob の湧きでも発火するため)
            if (!ItemAttributes.hasModifiers(change.oldItem())
                    && !ItemAttributes.hasModifiers(change.newItem())) {
                return;
            }
            sync(event.getEntity(), slot, change.newItem());
        });
    }

    /**
     * チャンクロード等でワールドに追加された時の再適用。
     * transient はアンロードで消えているので、剥がしは不要で付け直しのみ。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAddToWorld(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // canUseEquipmentSlot で非対応スロットを除外(例外を出させない)
            if (!living.canUseEquipmentSlot(slot)) {
                continue;
            }
            ItemStack item = equipment.getItem(slot);
            if (ItemAttributes.hasModifiers(item)) {
                sync(living, slot, item);
            }
        }
    }

    private void sync(LivingEntity entity, EquipmentSlot slot, ItemStack item) {
        String source = sourceFor(slot);
        engine.removeBySource(entity, source);
        for (ItemModifier modifier : ItemAttributes.get(item)) {
            if (!modifier.slot().test(slot)) {
                continue;
            }
            AttributeType type = engine.byKey(modifier.attribute());
            if (type == null) {
                continue; // 未登録属性(提供元プラグインが抜けた等)は黙ってスキップ
            }
            engine.add(entity, type, source, modifier.operation(), modifier.value(), 0, false);
        }
    }

}
