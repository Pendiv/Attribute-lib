package net.logiench.shardCore.core.menu.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InventoryMenu;
import net.logiench.logienchlibv2.api.minecraft.player.EditionCheck;
import net.logiench.shardCore.core.text.font.CustomUIFont;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleMenu extends AbstractMenu {
	protected final InventoryMenu menu;
	protected final boolean isJavaEdition;

	/**
	 * @param player 表示する対象のプレイヤー
	 */
	public SimpleMenu(@NotNull Player player) {
		super(player);
		this.isJavaEdition = EditionCheck.isJavaPlayer(player);

		TextComponent.Builder title = Component.text();
		if (isJavaEdition) {
			CustomUIFont font = getCustomUI();
			if (font != null) {
				title.append(font.getOffsetFont());
			}
		}
		title.append(getTitle());
		this.menu = new InventoryMenu(title.build(), getSize());
	}

	/**
	 * プレイヤーにメニューを表示します
	 */
	@Override
	protected void doOpen() {
		menu.send(player);
	}

	@Nullable
	public CustomUIFont getCustomUI() {
		return null;
	}

	public abstract int getSize();
}
