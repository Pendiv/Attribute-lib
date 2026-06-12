package net.logiench.shardCore.core.menu;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;

public enum BaseMenuItem {

	;

	private final SuperItemStack item;

	BaseMenuItem(SuperItemStack item) {
		this.item = item.setShowOnly();
	}

	public SuperItemStack getItem() {
		return item.clone();
	}
}
