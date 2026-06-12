package net.logiench.shardCore.core.item.system.gem;

import com.google.inject.Inject;
import net.logiench.shardCore.core.item.base.gem.GemTrigger;
import net.logiench.shardCore.core.item.system.gem.context.AttackGemContext;
import net.logiench.shardCore.core.player.system._PlayerCharacterManager;
import net.logiench.shardCore.core.player.system.item.PlayerEquipmentGemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class GemEventListener implements Listener {

	private final _PlayerCharacterManager playerCharacterManager;
	private final PlayerEquipmentGemManager equipmentGemManager;

	@Inject
	private GemEventListener(_PlayerCharacterManager playerCharacterManager, PlayerEquipmentGemManager equipmentGemManager) {
		this.playerCharacterManager = playerCharacterManager;
		this.equipmentGemManager = equipmentGemManager;
	}

	@EventHandler
	public void onPlayerAttack(EntityDamageByEntityEvent ev) {
		if (!(ev.getDamageSource().getCausingEntity() instanceof Player source)) {
			return;
		}
		playerCharacterManager.onCharacter(source, character ->
			equipmentGemManager.doGemActions(GemTrigger.ON_ATTACK, source.getUniqueId(), new AttackGemContext(character))
		);
	}
}
