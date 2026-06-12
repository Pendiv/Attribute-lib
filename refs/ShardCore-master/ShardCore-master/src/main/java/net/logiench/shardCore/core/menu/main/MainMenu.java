package net.logiench.shardCore.core.menu.main;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.util.ClickMenuCreator;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.menu.main.skillTree.SkillTreeMainMenu;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import net.logiench.shardCore.data.skill.tree.SkillTreeDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MainMenu extends SimpleMenu {
	private static final Component TITLE = Component.text("Main Menu");

	public MainMenu(Player player) {
		super(player);
	}

	@Override
	protected void initMenu() {
		menu.addAllListener(new ClickMenuCreator()
				.addEvent(0, ev -> {
					System.out.println("TEST: 0");
				})
				.addEvent(1, ev -> {
					openNext(() -> new SkillTreeMainMenu(player, new SkillTreeDefinition(new String[]{
						" ───",
						"│ ──",
						"│─ ─",
						"│── ",
					}, Map.of())));
				})
				.addEvent(2, ev -> {
					openNext(AppraisalMenu::new);
				})
				.addEvent(3, ev -> openNext(_EquipmentMenu::new))
			).setItem(0, SuperItemStack.init(Material.DIAMOND).setName("TEST"))
			.setItem(1, SuperItemStack.init(Material.PAPER).setName("SKILL_TREE"))
			.setItem(2, SuperItemStack.init(Material.CHEST).setName("Appraisal"))
			.setItem(3, SuperItemStack.init(Material.IRON_CHESTPLATE).setName("Equipment"))
			.addClickListener(ev -> {
				ev.setCancelled(true);
				Inventory clickedInventory = ev.getClickedInventory();
				if (clickedInventory == null || clickedInventory.getType() != InventoryType.PLAYER) {
					return;
				}
				ItemStack clickedItem = clickedInventory.getItem(ev.getSlot());
				ItemLoader loader = ItemLoader.of(clickedItem);
				if (loader == null) {
					return;
				}
				System.out.println(loader.getId());
			});
	}

	@Override
	public Component getTitle() {
		return TITLE;
		//		return Component.text("\uE000\uE001\uE002", Style.style().font(Key.key("shardcore","custom_ui")).color(NamedTextColor.WHITE).build()).append(Component.text("TEST", Style.style().color(NamedTextColor.GRAY)/*.decorate(TextDecoration.UNDERLINED)*/.font(Style.DEFAULT_FONT).build()));
	}

	@Override
	public int getSize() {
		return 54;
	}
}
