package net.logiench.shardCore.core.stats.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.logiench.logienchlibv2.api.minecraft.text.ChatColor;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.AttributeKey;
import net.logiench.shardLib.api.attribute.data.AttributeFormula;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AttributeEnum implements AttributeKey, Comparable<AttributeEnum> {
	private final String id;
	private final Component displayName;
	private final String displaySuffix;
	private final Double defaultValue;
	private final AttributeScalingFunction scalingFunction;
	private final AttributeFormulaAndDependencies formulaAndDependencies;

	public AttributeEnum(@NotNull String id, @NotNull Component displayName, @NotNull String displaySuffix, @Nullable Double defaultValue,
						 @Nullable AttributeScalingFunction scalingFunction, @Nullable AttributeFormula formula, @Nullable List<String> dependencies) {
		this.id = id;
		this.displayName = displayName;
		this.displaySuffix = displaySuffix;
		this.defaultValue = defaultValue;
		this.scalingFunction = scalingFunction;
		this.formulaAndDependencies = new AttributeFormulaAndDependencies(formula, dependencies == null ? List.of() : dependencies);
	}

	public AttributeEnum(@NotNull String id, @NotNull Component displayName, @NotNull String displaySuffix, @Nullable Double defaultValue) {
		this(id, displayName, displaySuffix, defaultValue, null, null, null);
	}

	public AttributeEnum(@NotNull String id, @NotNull String displayName, @NotNull String displaySuffix, @Nullable Double defaultValue) {
		this(id, ComponentUtil.text(displayName), displaySuffix, defaultValue);
	}

	/**
	 * defaultValueは0になります
	 */
	public AttributeEnum(@NotNull String id, @NotNull Component displayName, @NotNull String displaySuffix) {
		this(id, displayName, displaySuffix, 0d);
	}

	/**
	 * defaultValueは0になります
	 */
	public AttributeEnum(@NotNull String id, @NotNull String displayName, @NotNull String displaySuffix) {
		this(id, displayName, displaySuffix, 0d);
	}


	public AttributeEnum override(@Nullable AttributeScalingFunction scalingFunction, @Nullable AttributeFormula formula, @Nullable List<String> dependencies) {
		return new AttributeEnum(this.id, this.displayName, this.displaySuffix, this.defaultValue, scalingFunction, formula, dependencies);
	}

	public AttributeEnum override(@Nullable AttributeFormula formula, @Nullable List<String> dependencies) {
		return new AttributeEnum(this.id, this.displayName, this.displaySuffix, this.defaultValue, null, formula, dependencies);
	}

	/**
	 * 値がnullの場合は前の値が維持されます。
	 * 明示的に指定するとその値で定義は上書きされます。
	 */
	public AttributeEnum override(@Nullable Double defaultValue, @Nullable AttributeScalingFunction scalingFunction, @Nullable AttributeFormula formula, @Nullable List<String> dependencies) {
		return new AttributeEnum(this.id, this.displayName, this.displaySuffix, defaultValue, scalingFunction, formula, dependencies);
	}

	/**
	 * 値がnullの場合は前の値が維持されます。
	 * 明示的に指定するとその値で定義は上書きされます。
	 */
	public AttributeEnum override(@Nullable Double defaultValue, @Nullable AttributeFormula formula, @Nullable List<String> dependencies) {
		return new AttributeEnum(this.id, this.displayName, this.displaySuffix, defaultValue, null, formula, dependencies);
	}


	@Override
	public int compareTo(@NotNull AttributeEnum o) {
		return id.compareTo(o.id);
	}

	@Override
	@NotNull
	public String getId() {
		return id;
	}

	@NotNull
	public Component getDisplayName() {
		return displayName;
	}

	@NotNull
	public String getDisplaySuffix() {
		return displaySuffix;
	}

	public Component toDisplay(@Nullable TextColor plusColor, @Nullable TextColor zeroColor, @Nullable TextColor minusColor, double value) {
		Component formatted = Component.text(getDisplaySuffix().formatted(value))
			.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
		if (value == 0) {
			if (zeroColor != null) {
				formatted = formatted.color(zeroColor);
			}
		} else if (value > 0) {
			if (plusColor != null) {
				formatted = formatted.color(plusColor);
			}
		} else {
			if (minusColor != null) {
				formatted = formatted.color(minusColor);
			}
		}
		return getDisplayName().append(formatted);
	}

	public Component toDisplay(double value) {
		return toDisplay(ChatColor.GREEN, ChatColor.GRAY, ChatColor.RED, value);
	}

	/**
	 * 指定された値をもとにステータスのスケーリング計算を行います。
	 *
	 * @param value レベル1でのステータス
	 * @param level 計算後のステータスのレベル
	 * @return 指定されたレベルでのステータス
	 */
	public double getScalingValue(double value, double level) {
		return scalingFunction == null ? value : scalingFunction.get(value, level);
	}

	/**
	 * 指定された式でステータスが自動計算されるようにします。
	 */
	@Nullable
	public AttributeFormulaAndDependencies getFormula() {
		return formulaAndDependencies;
	}

	public AttributeDefinition toAttributeDefinition() {
		if (formulaAndDependencies == null) {
			return new AttributeDefinition(id, List.of(), null, defaultValue);
		}
		return new AttributeDefinition(id, formulaAndDependencies.dependencies, formulaAndDependencies.formula, defaultValue);
	}

	/**
	 *
	 * @param formula      計算式
	 * @param dependencies 計算で使用するステータス
	 */
	public record AttributeFormulaAndDependencies(@Nullable AttributeFormula formula,
												  @NotNull List<String> dependencies) {
	}
}
