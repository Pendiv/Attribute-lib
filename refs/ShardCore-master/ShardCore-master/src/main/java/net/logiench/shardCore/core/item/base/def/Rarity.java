package net.logiench.shardCore.core.item.base.def;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Rarity {
	COMMON(NamedTextColor.GRAY, "Common", 2),
	RARE(NamedTextColor.GREEN, "Rare", 3),
	LEGENDARY(NamedTextColor.AQUA, "Legendary", 4),
	MYTHIC(NamedTextColor.LIGHT_PURPLE, "Mythic", 5),
	;

	@Getter
	private final TextColor color;
	@Getter
	private final Component component;
	@Getter
	private final int subStatsCount;

	Rarity(TextColor color, String component, int subStatsCount) {
		this.color = color;
		this.component = Component.text(component, color).decoration(TextDecoration.ITALIC, false);
		this.subStatsCount = subStatsCount;
	}
}
