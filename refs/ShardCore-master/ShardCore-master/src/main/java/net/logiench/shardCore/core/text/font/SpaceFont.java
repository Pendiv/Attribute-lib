package net.logiench.shardCore.core.text.font;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public enum SpaceFont implements CustomFont {
	// マイナススペース
	SPACE_m1('\uE000', -1),
	SPACE_m2('\uE001', -2),
	SPACE_m4('\uE002', -4),
	SPACE_m8('\uE003', -8),
	SPACE_m16('\uE004', -16),
	SPACE_m32('\uE005', -32),
	SPACE_m64('\uE006', -64),
	SPACE_m128('\uE007', -128),
	SPACE_m256('\uE008', -256),
	SPACE_m512('\uE009', -512),
	SPACE_m1024('\uE010', -1024),

	/// 幅256のUIに使うスペース
	SPACE_m48('\uE100', -48),
	SPACE_m169('\uE101', -169),

	// プラススペース
	SPACE_p1('\uE200', 1),
	SPACE_p2('\uE201', 2),
	SPACE_p4('\uE202', 4),
	SPACE_p8('\uE203', 8),
	SPACE_p16('\uE204', 16),
	SPACE_p32('\uE205', 32),
	SPACE_p64('\uE206', 64),
	SPACE_p128('\uE207', 128),
	SPACE_p256('\uE208', 256),
	SPACE_p512('\uE209', 512),
	SPACE_p1024('\uE210', 1024),
	;

	private static final List<SpaceFont> MINUS_ASC_SORTED_SPACE = Stream.of(
			SPACE_m1, SPACE_m2, SPACE_m4, SPACE_m8, SPACE_m16, SPACE_m32,
			SPACE_m64, SPACE_m128, SPACE_m256, SPACE_m512, SPACE_m1024,
			SPACE_m48, SPACE_m169
		)
		.sorted(Comparator.comparingInt(SpaceFont::getWidth)).toList();

	private static final List<SpaceFont> PLUS_DESC_SORTED_SPACE = Stream.of(
			SPACE_p1, SPACE_p2, SPACE_p4, SPACE_p8, SPACE_p16, SPACE_p32,
			SPACE_p64, SPACE_p128, SPACE_p256, SPACE_p512, SPACE_p1024
		)
		.sorted(Comparator.comparingInt(SpaceFont::getWidth).reversed()).toList();


	private final char unicode;
	@Getter
	private final int width;
	private final Component font;

	SpaceFont(char unicode, int width) {
		this.unicode = unicode;
		this.width = width;
		this.font = CustomFont.getFont(unicode, SPACE_FONT);
	}

	@Override
	public Key getFontKey() {
		return SPACE_FONT;
	}

	@Override
	public char getUnicode() {
		return unicode;
	}

	@Override
	public Component getFont() {
		return font;
	}

	@Unmodifiable
	public static Component getSpace(int width) {
		Component text;
		if (width == 0) {
			text = Component.empty();
		} else {
			int currentWidth = width;
			StringBuilder sb = new StringBuilder();

			if (currentWidth > 0) {
				// プラスの場合
				for (SpaceFont font : PLUS_DESC_SORTED_SPACE) {
					int fontWidth = font.getWidth();
					if (fontWidth <= currentWidth) {
						currentWidth += fontWidth;
						sb.append(font.unicode);
					}
				}
				while (currentWidth > 0) {
					sb.append(SpaceFont.SPACE_p1.unicode);
					currentWidth--;
				}
			} else {
				// マイナスの場合
				for (SpaceFont font : MINUS_ASC_SORTED_SPACE) {
					int fontWidth = font.getWidth();
					if (fontWidth >= currentWidth) {
						currentWidth -= fontWidth;
						sb.append(font.unicode);
					}
				}
				while (currentWidth < 0) {
					sb.append(SpaceFont.SPACE_m1.unicode);
					currentWidth++;
				}
			}
			text = Component.text(sb.toString());
		}

		return text.style(Style.style().font(SPACE_FONT).build());
	}
}

