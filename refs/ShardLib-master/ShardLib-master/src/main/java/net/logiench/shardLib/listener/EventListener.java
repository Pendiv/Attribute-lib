package net.logiench.shardLib.listener;

import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import net.logiench.shardLib.util.ConfigLoader;
import net.logiench.shardLib.util.loader.InitializeRunStrategy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;

public class EventListener implements Listener {
	private final ConfigLoader loader;
	private boolean canSaveData = true;

	public EventListener(ConfigLoader loader) {
		this.loader = loader;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onServerLoad(ServerLoadEvent ev) {
		loader.run(new InitializeRunStrategy());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onWorldSave(WorldSaveEvent ev) {
		// 一番最初のワールドが保存された場合のみ更新
		if (canSaveData) {
			loader.playerCharacterManager().saveAllPlayers();
			loader.mobCharacterManager().saveAllCharacters();
			canSaveData = false;
			Delay.on(() -> {
				canSaveData = true;
			}, 20 * 30); // 30秒後に再び保存できるように
		}
	}

	// --------- DEBUG START ---------

	/*@EventHandler
	private void onChat(AsyncChatEvent ev) {
		Player p = ev.getPlayer();
		switch (ComponentUtil.toString(ev.message())) {
			case "1" -> {
				System.out.println(ShardLib.getGson().toJson(ShardLibProvider.get().getRegister().player().attributes().getAll()));
			}
			case "2" -> {
				PlayerAttributeAPI attributeAPI = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				attributeAPI.recalculateStats();
				System.out.println(ShardLib.getGson().toJson(attributeAPI.getFinalAttributes()));
			}
			case "3" -> {
				PlayerAttributeAPI attributeAPI = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				System.out.println(ShardLib.getGson().toJson(attributeAPI.getModifiers().stream().map(m -> {
					if (m instanceof AttributeModifier a) {
						return "[" + a.getSourceId() + ", " + a.getOperation() + ", " + a.getStackingRule() + ", " + a.getDurationTicks() + ", " + a.getTargetAttributeId() + ": " + a.getValue() + "]";
					} else if (m instanceof AttributeValueProvider a) {
						return "[" + a.getSourceId() + ", " + a.getOperation() + ", " + a.getDurationTicks() + ", " + a.getTargetAttributeId() + ": " + a.getValue(new CalculationContext(p, ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow())) + "]";
					}
					return "[ null ]";
				}).toList()));
			}
			case "clear" -> {
				PlayerAttributeAPI attributeAPI = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				attributeAPI.removeAll();
			}
			case "s" -> {
				Task.on(() -> {
					*//*Entity entity = ShardLibProvider.get().getMobAPI().spawnEntity(p.getLocation(), EntityType.HUSK, "test").get();
					ShardLibProvider.get().getMobAPI().getCharacterAPI(entity).ifPresent(character -> {
						character.getAttributeAPI().recalculateStats();
					});
					ShardLibProvider.get().getMobAPI().getCharacterAPI(entity).ifPresent(character -> {
						System.out.println("isShardEntity!");

					});*//*


					PlayerAttributeAPI attributeAPI = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
					attributeAPI.setBaseAttribute("test", 25);
					attributeAPI.addModifier(new AttributeModifier("pl2", "max_health", ModifierOperation.MULTIPLY, StackingRule.DENY, 1.25));
					attributeAPI.addModifier(new AttributeModifier("pl1", "test", ModifierOperation.SUBTRACT, StackingRule.DENY, 5));
					attributeAPI.recalculateStats();
					System.out.println(ShardLib.getGson().toJson(attributeAPI.getBaseAttributes()));
					System.out.println(ShardLib.getGson().toJson(attributeAPI.getFinalAttributes()));
				});
			}
		}
	}

	@EventHandler
	private void onClick(PlayerInteractEvent ev) {
		if (ev.getHand() == null || !ev.getHand().equals(EquipmentSlot.HAND)) {
			return;
		}
		Player p = ev.getPlayer();
		*//*ShardLibProvider.get().getItemAPI().getItemData(SuperItemStack.safeInit(ev.getItem())).ifPresent(item -> {
			System.out.println(ShardLib.getGson().toJson(item.getBaseStats()));
		});*//*
	}*/

	// --------- DEBUG END ---------
}
