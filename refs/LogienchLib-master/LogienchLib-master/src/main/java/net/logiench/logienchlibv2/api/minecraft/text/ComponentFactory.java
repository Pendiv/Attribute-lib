package net.logiench.logienchlibv2.api.minecraft.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentFactory {
	// Minecraft内部側で上限があるが、詳細な数値は不明。とりあえず確実に安全な数値に設定してある。上限を超えるとStackOverflowが出る
	private static final int MAX_DEPTH = 32;

	private static final Pattern DEFAULT_MAIN_PATTERN = Pattern.compile(
		ComponentUtil.defaultTarget + "\\((\\d{1,3}\\s*,\\s*){2}\\d{1,3}\\)" +
			"|" + ComponentUtil.defaultTarget + "\\(#[A-Fa-f\\d]{6}\\)" +
			"|" + ComponentUtil.defaultTarget + "[\\da-u~_]"
	);
	private static final Pattern DEFAULT_REPLACE_PATTERN = Pattern.compile(
		"^" + ComponentUtil.defaultTarget + "\\(?|\\)?$"
	);

	static Component text(String text) {
		Component component = checkText(ComponentUtil.defaultTarget, text);
		if (component != null) {
			return component;
		}
		return new ComponentFactory(
			ComponentUtil.defaultTarget, text,
			DEFAULT_MAIN_PATTERN,
			DEFAULT_REPLACE_PATTERN
		).getComponent();
	}

	static Component text(char target, String text) {
		Component component = checkText(target, text);
		if (component != null) {
			return component;
		}
		return new ComponentFactory(
			target, text,
			Pattern.compile("\\" + target + "\\((\\d{1,3}\\s*,\\s*){2}\\d{1,3}\\)|" + "\\" + target + "\\(#[A-Fa-f\\d]{6}\\)|" + "\\" + target + "[\\da-u~_]"),
			null
		).getComponent();
	}

	private static Component checkText(char target, String text) {
		if (text == null || text.isEmpty()) {
			return ComponentUtil.NOT_ITALIC;
		}
		if (text.indexOf(target) == -1) {
			return ComponentUtil.NOT_ITALIC.append(nonSectionComponent(text));
		}
		return null;
	}

	private final char target;
	private final String text;
	private final Pattern mainPattern;
	private Pattern replacePattern;
	private final TextComponent.Builder builder;
	private final Component component;

	private ComponentFactory(char target, String text, Pattern mainPattern, Pattern replacePattern) {
		this.target = target;
		this.text = text;
		this.mainPattern = mainPattern;
		this.replacePattern = replacePattern;
		this.builder = ComponentUtil.NOT_ITALIC.toBuilder();
		applyStyle();
		this.component = builder.build();
	}

	private void applyStyle() {
		if (replacePattern == null) {
			this.replacePattern = Pattern.compile("^\\" + target + "\\(?|\\)?$");
		}
		Stack<Component> stack = new Stack<>();
		StyleData data = new StyleData();
		Matcher matcher = mainPattern.matcher(text);
		int lastIndex = 0;

		while (matcher.find()) {
			String textSegment = text.substring(lastIndex, matcher.start());
			if (!textSegment.isEmpty()) {
				addComponent(stack, data, nonSectionComponent(textSegment));
				data.nextStyle(); // スタイルを次の状態に移動
			}

			String replaceMatch = replacePattern.matcher(matcher.group())
				.replaceAll("");

			if (!testAndDoPattern1(data, replaceMatch)) {
				if (!testAndDoPattern2(data, replaceMatch)) {
					testAndDoPattern3(data, replaceMatch);
				}
			}

			lastIndex = matcher.end();
		}
		if (lastIndex < text.length()) {
			String remainingText = text.substring(lastIndex);
			if (!remainingText.isEmpty()) {
				addComponent(stack, data, nonSectionComponent(remainingText));
			}
		}
		if (!stack.isEmpty()) {
			builder.append(toComponent(stack));
		}
	}

	private boolean testAndDoPattern1(StyleData data, String match) {
		if (match.length() != 1) {
			return false;
		}
		char ch = match.charAt(0);
		ChatColor color = ChatColor.getByChar(ch);
		if (color == null) {
			ChatColor.ChatDecoration decoration = ChatColor.ChatDecoration.getByChar(ch);
			if (decoration != null) {
				if (decoration.decoration() != null) {
					data.addDecoration(decoration.decoration());
				}
				if (decoration.isReset()) {
					data.resetStyle();
				}
			}
		} else {
			data.setColor(color);
		}

		return true;
	}

	private boolean testAndDoPattern2(StyleData data, String match) {
		if (!match.startsWith("#")) {
			return false;
		}

		data.setColor(TextColor.fromHexString(match.toUpperCase()));

		return true;
	}

	private void testAndDoPattern3(StyleData data, String match) {
		Integer[] rgb = Arrays.stream(match.split("\\s*,\\s*"))
			.map(Integer::valueOf)
			.toArray(Integer[]::new);
		data.setColor(TextColor.color(rgb[0], rgb[1], rgb[2]));
	}

	private void addComponent(Stack<Component> stack, StyleData data, Component component) {
		// 前回の差分だけのスタイルデータを入れる
		boolean hasDifference = false;
		if (!Objects.equals(data.color, data.lastColor)) {
			component = component.color(data.color);
			hasDifference = true;
		}
		if (!data.decorationDeff.isEmpty()) {
			component = component.decorations(data.decorationDeff);
			hasDifference = true;
		}

		if (!hasDifference && !stack.isEmpty()) {
			Component last = stack.pop();
			// 前回と差分がないのでもうここで入れちゃう
			stack.add(last.append(component));
			// 差分がないということはこれ以上の処理は必要ない
			return;
		}

		if (data.isResetStyle || stack.size() >= MAX_DEPTH) {
			if (!stack.isEmpty()) {
				builder.append(toComponent(stack));
			}
		}

		stack.add(component);
	}

	private Component toComponent(Stack<Component> stack) {
		if (stack.isEmpty()) {
			throw new RuntimeException("Stack is empty");
		}
		Component component = stack.pop();
		while (!stack.isEmpty()) {
			component = stack.pop().append(component);
		}
		return component;
	}

	public Component getComponent() {
		return component;
	}

	private static Component nonSectionComponent(String text) {
		return Component.text(text.replaceAll("§", "&"));
	}

	private static class StyleData {
		boolean isResetStyle = false;
		private final Decorations decorations = new Decorations();
		private TextColor color = null;
		private TextColor lastColor = null;
		private final Map<TextDecoration, TextDecoration.State> decorationDeff = new HashMap<>();

		private void setColor(TextColor color) {
			this.color = color;
		}

		private void addDecoration(TextDecoration decoration) {
			if (decorations.addDecoration(decoration)) {
				decorationDeff.put(decoration, TextDecoration.State.TRUE);
			}
		}

		private void nextStyle() {
			decorationDeff.clear();
			this.lastColor = color;
			this.isResetStyle = false;
		}

		private void resetStyle() {
			decorations.reset();
			this.color = null;
			this.isResetStyle = true;
		}
	}

	private static class Decorations implements Cloneable {
		private Map<TextDecoration, TextDecoration.State> stateMap = new HashMap<>() {{
			for (TextDecoration decoration : TextDecoration.values()) {
				put(decoration, TextDecoration.State.FALSE);
			}
		}};

		public void reset() {
			stateMap.replaceAll((a, b) -> TextDecoration.State.FALSE);
		}

		/// @return FALSEからTRUEへ変化した場合true
		public boolean addDecoration(TextDecoration decoration) {
			// 置き換え前のものがFALSEと等しかったら
			return TextDecoration.State.FALSE.equals(stateMap.replace(decoration, TextDecoration.State.TRUE));
		}

		@Override
		public Decorations clone() {
			try {
				Decorations decorations = (Decorations) super.clone();
				decorations.stateMap = Map.copyOf(stateMap);
				return decorations;
			} catch (CloneNotSupportedException ignored) {
			}
			return null;
		}
	}
}
