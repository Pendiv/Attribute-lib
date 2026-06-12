package net.logiench.shardCore.core.menu.util;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.util.ClickMenuCreator;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ForceOpenConfirmationMenu extends SimpleMenu {
	// ターゲットを抽象クラス（またはインターフェース）に変更
	private final AbstractMenu targetMenu;

	public ForceOpenConfirmationMenu(Player player, AbstractMenu targetMenu) {
		super(player);
		this.targetMenu = targetMenu;
	}


	@Override
	protected void initMenu() {
		menu.addAllListener(new ClickMenuCreator()
			.addEvent(11, ev -> {
				back();
			}).addEvent(15, ev -> {
				// 強制的に開く
				targetMenu.open(true);
			})
			).setItem(11, SuperItemStack.init(Material.RED_CONCRETE).setName("戻る"))
			.setItem(15, SuperItemStack.init(Material.GREEN_CONCRETE).setName("進む"));
	}

	@Override
	public Component getTitle() {
		return Component.text("確認");
	}

	@Override
	public int getSize() {
		return 27;
	}
}
