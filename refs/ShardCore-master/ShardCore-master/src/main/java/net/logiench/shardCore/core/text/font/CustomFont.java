package net.logiench.shardCore.core.text.font;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

public interface CustomFont {
	Key SPACE_FONT = Key.key("shardcore", "space");
	Key CUSTOM_UI_FONT = Key.key("shardcore", "custom_ui");


	Key getFontKey();

	char getUnicode();

	Component getFont();

	static Component getFont(char unicode, Key key) {
		return Component.text(unicode, Style.style().font(key).color(NamedTextColor.WHITE).build());
	}
}
