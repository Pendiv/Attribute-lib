package net.logiench.logienchlibv2.api.minecraft.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

//　「不正またはサポートされていないエスケープシーケンス」という謎エラーは無視してください。動くので
@SuppressWarnings("unused")
public final class ComponentUtil {
	public static final TextComponent NOT_ITALIC = Component.empty().style(Style.style(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE)));
	public static final char defaultTarget = '§';
	private static final Pattern TO_STRING_PATTERN = Pattern.compile("^\\[|]$");

	/**
	 * Componentの内部にある文字列を抽出し、Stringとして返します。
	 * 色データは反映されません。
	 *
	 * @param component 対象のComponent
	 * @return 抽出した文字列
	 *
	 * @deprecated string()を使用してください
	 */
	@Contract(pure = true, value = "null -> null; !null -> !null")
	@Deprecated
	public static String toString(@Nullable Component component) {
		return string(component);
	}

	/**
	 * Componentのリスト全てに{@link #toString(Component)}を実行します。
	 * 色データは反映されません。
	 *
	 * @param components 対象のComponentのリスト
	 * @return 編集不可な抽出した文字列のリスト
	 *
	 * @deprecated notNullString()を使用してください
	 */
	@NotNull
	@Contract(pure = true, value = "_ -> !null")
	@Unmodifiable
	@Deprecated
	public static List<String> toNotNullString(@Nullable List<Component> components) {
		return notNullString(components);
	}

	/**
	 * Componentのリスト全てに{@link #toString(Component)}を実行します。
	 * 色データは反映されません。
	 *
	 * @param components 対象のComponentのリスト
	 * @return 編集不可な抽出した文字列のリスト
	 *
	 * @deprecated string()を使用してください
	 */
	@Contract(pure = true, value = "null -> null; !null -> !null")
	@Unmodifiable
	@Deprecated
	public static List<String> toString(@Nullable List<Component> components) {
		return string(components);
	}

	/**
	 * 文字列から<code>target</code>を旧色コード指定方式<code>§</code>として{@link ChatColor}をもとにComponentに変換します。
	 * <p>色指定方法、<code>&</code>を<code>target</code>として指定した場合</p>
	 * <ul>
	 *    <li>&a {@link ChatColor#GREEN}</li>
	 *    <li>&d {@link ChatColor#LIGHT_PURPLE}</li>
	 *    <li>&(#000000) <code>TextColor.fromHexString("000000")</code></li>
	 *    <li>&(80,90,100) <code>TextColor.color(80, 90, 100)</code></li>
	 *    <li>&(80, 90, 100) <code>TextColor.color(80, 90, 100)</code></li>
	 * </ul>
	 *
	 * @param target §の役割を持たせる文字
	 * @param string Componentに変換する文字列
	 * @return 変換後の文字列
	 *
	 * @deprecated text()を使用してください
	 */
	@Deprecated
	public static Component toComponent(char target, String string) {
		return text(target, string);
	}

	/**
	 * 文字列から旧色コード指定方式を用いて、{@link ChatColor}をもとにComponentに変換します。
	 * <p>色指定方法 <code>指定後の色</code></p>
	 * <ul>
	 *    <li>§a {@link ChatColor#GREEN}</li>
	 *    <li>§d {@link ChatColor#LIGHT_PURPLE}</li>
	 *    <li>§(#000000) <code>TextColor.fromHexString("000000")</code></li>
	 *    <li>§(80,90,100) <code>TextColor.color(80, 90, 100)</code></li>
	 *    <li>§(80, 90, 100) <code>TextColor.color(80, 90, 100)</code></li>
	 * </ul>
	 *
	 * @param string Componentに変換する文字列
	 * @return 変換後の文字列
	 *
	 * @deprecated text()を使用してください
	 */
	@Deprecated
	public static Component toComponent(@Nullable String string) {
		return text(string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#toComponent(char, String)}で作成したComponentを、
	 * {@link TextComponent#appendNewline()}で1つに連結させます。
	 *
	 * @see ComponentUtil#toComponent(char, String)
	 * @deprecated textLine()を使用してください
	 */
	@Deprecated
	public static Component toComponentLine(char target, @NotNull String... string) {
		return textLine(target, string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#toComponent(String)}で作成したComponentを、
	 * {@link TextComponent#appendNewline()}で1つに連結させます。
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @deprecated textLine()を使用してください
	 */
	@Deprecated
	public static Component toComponentLine(@NotNull String... string) {
		return textLine(defaultTarget, string);
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#toComponentLine(char, String...)}に入力します。
	 *
	 * @see ComponentUtil#toComponent(char, String)
	 * @see ComponentUtil#toComponentLine(char, String...)
	 * @deprecated textLine()を使用してください
	 */
	@Deprecated
	public static Component toComponentLine(char target, String string) {
		return textLine(target, string.split("\\n"));
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#toComponentLine(String...)}に入力します。
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @see ComponentUtil#toComponentLine(String...)
	 * @deprecated textLine()を使用してください
	 */
	@Deprecated
	public static Component toComponentLine(String string) {
		return textLine(defaultTarget, string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#toComponent(char, String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#toComponent(char, String)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(char target, @NotNull String... string) {
		return textList(target, string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#toComponent(String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(@NotNull String... string) {
		return textList(defaultTarget, string);
	}

	/**
	 * リストの各要素ごとに{@link ComponentUtil#toComponent(char, String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#toComponent(char, String)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(char target, List<String> strings) {
		return textList(target, strings);
	}

	/**
	 * リストの各要素ごとに{@link ComponentUtil#toComponent(String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(List<String> strings) {
		return textList(defaultTarget, strings);
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#toComponentList(String...)}に入力します。
	 *
	 * @return 編集不可なリスト
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @see ComponentUtil#toComponentList(String...)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(char target, String string) {
		return textList(target, string);
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#toComponentList(String...)}に入力します。
	 *
	 * @return 編集不可なリスト
	 *
	 * @see ComponentUtil#toComponent(String)
	 * @see ComponentUtil#toComponentList(String...)
	 * @deprecated textList()を使用してください
	 */
	@Unmodifiable
	@Deprecated
	public static List<Component> toComponentList(String string) {
		return textList(defaultTarget, string);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Componentの内部にある文字列を抽出し、Stringとして返します。
	 * 色データは反映されません。
	 *
	 * @param component 対象のComponent
	 * @return 抽出した文字列
	 */
	@Contract(pure = true, value = "null -> null; !null -> !null")
	public static String string(@Nullable Component component) {
		if (component == null) {
			return null;
		}
		return TO_STRING_PATTERN.matcher(PlainTextComponentSerializer.plainText().serialize(component))
			.replaceAll("");
	}

	/**
	 * Componentの内部にある文字列を抽出し、Stringとして返します。
	 * 色データは反映されません。
	 *
	 * @param component 対象のComponent
	 * @return 抽出した文字列
	 */
	@NotNull
	@Contract(pure = true)
	public static String notNullString(@Nullable Component component) {
		String string = string(component);
		if (string == null) {
			return Strings.EMPTY;
		}
		return string;
	}

	/**
	 * Componentのリスト全てに{@link #string(Component)}を実行します。
	 * 色データは反映されません。
	 *
	 * @param components 対象のComponentのリスト
	 * @return 編集不可な抽出した文字列のリスト
	 */
	@Contract(pure = true, value = "null -> null; !null -> !null")
	@Unmodifiable
	public static List<String> string(@Nullable List<Component> components) {
		if (components == null) {
			return null;
		}
		return components.stream().map(ComponentUtil::string).toList();
	}

	/**
	 * Componentのリスト全てに{@link #string(Component)}を実行します。
	 * 色データは反映されません。
	 *
	 * @param components 対象のComponentのリスト
	 * @return 編集不可な抽出した文字列のリスト
	 */
	@NotNull
	@Contract(pure = true)
	@Unmodifiable
	public static List<String> notNullString(@Nullable List<Component> components) {
		List<String> list = string(components);
		if (list == null) {
			return List.of();
		}
		return list;
	}

	/**
	 * 文字列から<code>target</code>を旧色コード指定方式<code>§</code>として{@link ChatColor}をもとにComponentに変換します。
	 * <p>色指定方法、<code>&</code>を<code>target</code>として指定した場合</p>
	 * <ul>
	 *    <li>&a {@link ChatColor#GREEN}</li>
	 *    <li>&d {@link ChatColor#LIGHT_PURPLE}</li>
	 *    <li>&(#000000) <code>TextColor.fromHexString("000000")</code></li>
	 *    <li>&(80,90,100) <code>TextColor.color(80, 90, 100)</code></li>
	 *    <li>&(80, 90, 100) <code>TextColor.color(80, 90, 100)</code></li>
	 * </ul>
	 *
	 * @param target §の役割を持たせる文字
	 * @param string Componentに変換する文字列
	 * @return 変換後の文字列
	 */
	public static Component text(char target, String string) {
		return ComponentFactory.text(target, string);
	}

	/**
	 * 文字列から旧色コード指定方式を用いて、{@link ChatColor}をもとにComponentに変換します。
	 * <p>色指定方法 <code>指定後の色</code></p>
	 * <ul>
	 *    <li>§a {@link ChatColor#GREEN}</li>
	 *    <li>§d {@link ChatColor#LIGHT_PURPLE}</li>
	 *    <li>§(#000000) <code>TextColor.fromHexString("000000")</code></li>
	 *    <li>§(80,90,100) <code>TextColor.color(80, 90, 100)</code></li>
	 *    <li>§(80, 90, 100) <code>TextColor.color(80, 90, 100)</code></li>
	 * </ul>
	 *
	 * @param string Componentに変換する文字列
	 * @return 変換後の文字列
	 */
	public static Component text(@Nullable String string) {
		return ComponentFactory.text(string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#text(char, String)}で作成したComponentを、
	 * {@link TextComponent#appendNewline()}で1つに連結させます。
	 *
	 * @see ComponentUtil#text(char, String)
	 */
	public static Component textLine(char target, @NotNull String... strings) {
		if (strings == null) {
			return null;
		}
		TextComponent.Builder builder = Component.text();
		for (int i = 0; i < strings.length; i++) {
			if (i != 0) {
				builder.appendNewline();
			}
			builder.append(text(target, strings[i]));
		}
		return builder.build();
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#text(String)}で作成したComponentを、
	 * {@link TextComponent#appendNewline()}で1つに連結させます。
	 *
	 * @see ComponentUtil#text(String)
	 */
	public static Component textLine(@NotNull String... strings) {
		return textLine(defaultTarget, strings);
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#textLine(char, String...)}に入力します。
	 *
	 * @see ComponentUtil#text(char, String)
	 * @see ComponentUtil#textLine(char, String...)
	 */
	public static Component textLine(char target, String string) {
		if (string == null) {
			return null;
		}
		return textLine(target, string.split("\\n"));
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#textLine(String...)}に入力します。
	 *
	 * @see ComponentUtil#text(String)
	 * @see ComponentUtil#textLine(String...)
	 */
	public static Component textLine(String string) {
		return textLine(defaultTarget, string);
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#text(char, String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#text(char, String)
	 */
	@Unmodifiable
	public static List<Component> textList(char target, @NotNull String... strings) {
		return Stream.of(strings).map(s -> text(target, s)).toList();
	}

	/**
	 * 配列の各要素ごとに{@link ComponentUtil#text(String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#text(String)
	 */
	@Unmodifiable
	public static List<Component> textList(@NotNull String... strings) {
		return textList(defaultTarget, strings);
	}

	/**
	 * リストの各要素ごとに{@link ComponentUtil#text(char, String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#text(char, String)
	 */
	@Unmodifiable
	public static List<Component> textList(char target, List<String> strings) {
		return strings.stream().map(s -> text(target, s)).toList();
	}

	/**
	 * リストの各要素ごとに{@link ComponentUtil#text(String)}で作成したComponentを、
	 * リストにまとめます。
	 *
	 * @return 編集不可なList
	 *
	 * @see ComponentUtil#text(String)
	 */
	@Unmodifiable
	public static List<Component> textList(List<String> strings) {
		return textList(defaultTarget, strings);
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#textList(String...)}に入力します。
	 *
	 * @return 編集不可なリスト
	 *
	 * @see ComponentUtil#text(String)
	 * @see ComponentUtil#textList(String...)
	 */
	@Unmodifiable
	public static List<Component> textList(char target, String string) {
		if (string == null) {
			return null;
		}
		return textList(target, string.split("\\n"));
	}

	/**
	 * <code>\n</code>(改行コード)ごとに分割し、{@link ComponentUtil#textList(String...)}に入力します。
	 *
	 * @return 編集不可なリスト
	 *
	 * @see ComponentUtil#text(String)
	 * @see ComponentUtil#textList(String...)
	 */
	@Unmodifiable
	public static List<Component> textList(String string) {
		return textList(defaultTarget, string);
	}
}
