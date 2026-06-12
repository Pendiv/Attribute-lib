package net.logiench.shardCore.core.menu.main.skillTree;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public enum MoveDirection {
	UP(0, -1, 47, "UP"),
	DOWN(0, 1, 48, "DOWN"),
	LEFT(-1, 0, 50, "LEFT"),
	RIGHT(1, 0, 51, "RIGHT"),
	;

	private final int slot;
	private final int x;
	private final int y;
	@NotNull
	private final ItemStack item;

	MoveDirection(int x, int y, int slot, String name) {
		this.slot = slot;
		this.x = x;
		this.y = y;
		item = SuperItemStack.init(Material.ARROW).setName(name).build();
	}

	public int moveX(int x) {
		return x + this.x;
	}

	public int moveY(int y) {
		return y + this.y;
	}
}

