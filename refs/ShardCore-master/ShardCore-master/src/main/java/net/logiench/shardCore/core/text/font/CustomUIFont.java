package net.logiench.shardCore.core.text.font;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public enum CustomUIFont implements CustomFont {
	APPRAISAL('\uE001', 27),
	;

	private final char unicode;
	@Getter
	private final int inventorySize;
	private final Component font;
	@Getter
	private final Component offsetFont;

	CustomUIFont(char unicode, int inventorySize, int width) {
		this.unicode = unicode;
		this.inventorySize = inventorySize;
		this.font = CustomFont.getFont(unicode, CUSTOM_UI_FONT);

		// (width - 160) / 2 とすることで自動で幅を計算する
		int offsetWidth = (width - 160) / 2;
		this.offsetFont = Component.empty()
			.append(SpaceFont.getSpace(-offsetWidth))
			.append(font)
			// 端からテキスト開始位置までを2倍にして、インベントリのUIとテキスト開始位置の差である8px分を引く
			.append(SpaceFont.getSpace(-width + (offsetWidth * 2 - 9)));
	}

	CustomUIFont(char unicode, int inventorySize) {
		this(unicode, inventorySize, 256); // 256はこのテクスチャでデフォルトの幅
	}

	@Override
	public Key getFontKey() {
		return CUSTOM_UI_FONT;
	}

	@Override
	public char getUnicode() {
		return unicode;
	}

	@Override
	public Component getFont() {
		return font;
	}
}
