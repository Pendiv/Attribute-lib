package net.logiench.shardCore;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

public class ShardCoreBootstrap implements PluginBootstrap {
	@Override
	public void bootstrap(BootstrapContext context) {
		/*context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
			event.registry().register(
				// The key of the registry
				// Plugins should use their own namespace instead of minecraft or papermc
				EnchantmentKeys.create(Key.key("papermc:pointy")),
				b -> b.description(Component.text("Pointy"))
					.supportedItems(event.getOrCreateTag(ItemTypeTagKeys.SWORDS))
					.anvilCost(1)
					.maxLevel(25)
					.weight(10)
					.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
					.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
					.activeSlots(EquipmentSlotGroup.ANY)
			);
		}));


		context.getLifecycleManager().registerEventHandler(RegistryEvents.DAMAGE_TYPE.compose()
			.newHandler(event ->
				event.registry().register(DamageTypeKeys.create(Key.key("logiench:test")), builder -> {
					builder.damageScaling(DamageScaling.NEVER)
						.messageId("test")
						.exhaustion(3.12345f)
						.damageEffect(DamageEffect.POKING)
						.deathMessageType(DeathMessageType.DEFAULT);
				})
			));


		context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(),
			e -> e.registry().register(
				DialogKeys.create(Key.key("papermc:praise_paperchan")),
				builder -> builder
					.base(DialogBase.builder(Component.text("Accept our rules!", NamedTextColor.LIGHT_PURPLE))
						.canCloseWithEscape(false)
						.body(List.of(
							DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!"))
						))
						.build()
					)
					.type(DialogType.confirmation(
						ActionButton.builder(Component.text("Paper-chan is cute!", TextColor.color(0xEDC7FF)))
							.tooltip(Component.text("Click to agree!"))
							.action(DialogAction.customClick(Key.key("papermc:paperchan/agree"), null))
							.build(),
						ActionButton.builder(Component.text("I hate Paper-chan!", TextColor.color(0xFF8B8E)))
							.tooltip(Component.text("Click this if you are a bad person!"))
							.action(DialogAction.customClick(Key.key("papermc:paperchan/disagree"), null))
							.build()
					))
			)
		);*/
	}
}
