package net.logiench.logienchlibv2.api.minecraft.text;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import net.kyori.examination.ExaminableProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ChatColor implements TextColor {
	public static final ChatColor BLACK = new ChatColor('0', NamedTextColor.BLACK);
	public static final ChatColor DARK_BLUE = new ChatColor('1', NamedTextColor.DARK_BLUE);
	public static final ChatColor DARK_GREEN = new ChatColor('2', NamedTextColor.DARK_GREEN);
	public static final ChatColor DARK_AQUA = new ChatColor('3', NamedTextColor.DARK_AQUA);
	public static final ChatColor DARK_RED = new ChatColor('4', NamedTextColor.DARK_RED);
	public static final ChatColor DARK_PURPLE = new ChatColor('5', NamedTextColor.DARK_PURPLE);
	public static final ChatColor GOLD = new ChatColor('6', NamedTextColor.GOLD);
	public static final ChatColor GRAY = new ChatColor('7', NamedTextColor.GRAY);
	public static final ChatColor DARK_GRAY = new ChatColor('8', NamedTextColor.DARK_GRAY);
	public static final ChatColor BLUE = new ChatColor('9', NamedTextColor.BLUE);
	public static final ChatColor GREEN = new ChatColor('a', NamedTextColor.GREEN);
	public static final ChatColor AQUA = new ChatColor('b', NamedTextColor.AQUA);
	public static final ChatColor RED = new ChatColor('c', NamedTextColor.RED);
	public static final ChatColor LIGHT_PURPLE = new ChatColor('d', NamedTextColor.LIGHT_PURPLE);
	public static final ChatColor YELLOW = new ChatColor('e', NamedTextColor.YELLOW);
	public static final ChatColor WHITE = new ChatColor('f', NamedTextColor.WHITE);
	public static final ChatColor MINECOIN_GOLD = new ChatColor('g', TextColor.color(221, 214, 5));
	public static final ChatColor MATERIAL_QUARTZ = new ChatColor('h', TextColor.color(227, 212, 209));
	public static final ChatColor MATERIAL_IRON = new ChatColor('i', TextColor.color(206, 202, 202));
	public static final ChatColor MATERIAL_NETHERITE = new ChatColor('j', TextColor.color(68, 58, 59));
	public static final ChatColor MATERIAL_REDSTONE = new ChatColor('m', TextColor.color(151, 22, 7));
	public static final ChatColor MATERIAL_COPPER = new ChatColor('n', TextColor.color(180, 104, 77));
	public static final ChatColor MATERIAL_GOLD = new ChatColor('p', TextColor.color(222, 177, 45));
	public static final ChatColor MATERIAL_EMERALD = new ChatColor('q', TextColor.color(17, 160, 54));
	public static final ChatColor MATERIAL_DIAMOND = new ChatColor('s', TextColor.color(44, 186, 168));
	public static final ChatColor MATERIAL_LAPIS = new ChatColor('t', TextColor.color(33, 73, 123));
	public static final ChatColor MATERIAL_AMETHYST = new ChatColor('u', TextColor.color(154, 92, 198));

	private static final ChatColor[] COLORS = new ChatColor[]{
		BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA,
		DARK_RED, DARK_PURPLE, GOLD, GRAY,
		DARK_GRAY, BLUE, GREEN, AQUA,
		RED, LIGHT_PURPLE, YELLOW, WHITE,
		MINECOIN_GOLD, MATERIAL_QUARTZ, MATERIAL_IRON, MATERIAL_NETHERITE,
		MATERIAL_REDSTONE, MATERIAL_COPPER, MATERIAL_GOLD, MATERIAL_EMERALD,
		MATERIAL_DIAMOND, MATERIAL_LAPIS, MATERIAL_AMETHYST,
	};

	private final char code;
	private final TextColor color;

	private ChatColor(char colorCode, TextColor color) {
		this.code = colorCode;
		this.color = color;
	}

	public char code() {
		return code;
	}

	public TextColor color() {
		return color;
	}

	@Override
	public String toString() {
		return "§" + code;
	}

	@Override
	public int value() {
		return color.value();
	}

	@Override
	public @NotNull HSVLike asHSV() {
		return color.asHSV();
	}

	@Override
	public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
		return Stream.concat(
			Stream.of(ExaminableProperty.of("name", color.toString())),
			TextColor.super.examinableProperties()
		);
	}

	@Nullable
	public static ChatColor getByChar(char code) {
		for (ChatColor color : values()) {
			if (color.code() == code) {
				return color;
			}
		}
		return null;
	}

	public static ChatColor[] values() {
		return COLORS;
	}

	enum ChatDecoration {
		ITALIC('o', false, TextDecoration.ITALIC),
		BOLD('l', false, TextDecoration.BOLD),
		STRIKETHROUGH('~', false, TextDecoration.STRIKETHROUGH),
		UNDERLINE('_', false, TextDecoration.UNDERLINED),
		OBFUSCATED('k', false, TextDecoration.OBFUSCATED),
		RESET('r', true, null);

		private final char code;
		private final boolean reset;
		private final TextDecoration decoration;

		ChatDecoration(char colorCode, boolean colorReset, @Nullable TextDecoration decoration) {
			this.code = colorCode;
			this.reset = colorReset;
			this.decoration = decoration;
		}

		public char code() {
			return code;
		}

		public boolean isReset() {
			return reset;
		}

		@Nullable
		public TextDecoration decoration() {
			return decoration;
		}

		@Nullable
		public static ChatDecoration getByChar(char code) {
			for (ChatDecoration color : values()) {
				if (color.code() == code) {
					return color;
				}
			}
			return null;
		}
	}
}
