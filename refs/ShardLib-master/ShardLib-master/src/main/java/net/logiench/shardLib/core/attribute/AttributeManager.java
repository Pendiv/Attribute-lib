package net.logiench.shardLib.core.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.data.*;
import net.logiench.shardLib.core.attribute.data.OperationList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


@Singleton
public class AttributeManager {
	private final AttributeDefinitionRegisterImpl registry;
	private List<AttributeDefinition> sortedDerivedAttributes = null;

	@Inject
	public AttributeManager(AttributeDefinitionRegisterImpl registry) {
		this.registry = registry;
	}

	private double applyModifiers(@Nullable CalculationContext context, double stat, @Nullable OperationList modifiers) {
		if (modifiers == null) {
			return stat;
		}
		if (modifiers.hasSet()) {
			// 一番最後のSETを取得する
			return getValue(context, modifiers.getModifiers(ModifierOperation.SET).getLast());
		}
		// SETはないから大丈夫
		for (AttributeOperationModifier modifier : modifiers) {
			stat = applyModifier(context, stat, modifier);
		}
		return stat;
	}

	private double applyModifier(@Nullable CalculationContext context, double stat, @NotNull AttributeOperationModifier modifier) {
		return switch (modifier.getOperation()) {
			case ADD -> stat + getValue(context, modifier);
			case SUBTRACT -> stat - getValue(context, modifier);
			case MULTIPLY -> stat * getValue(context, modifier);
			case DIVIDE -> stat / getValue(context, modifier);
			case SET -> getValue(context, modifier);
		};
	}

	private double getValue(@Nullable CalculationContext context, @NotNull AttributeOperationModifier modifier) {
		return switch (modifier) {
			case AttributeModifier m -> m.getValue();
			case AttributeValueProvider m -> m.getValue(context);
		};
	}

	/**
	 * 与えられたステータスを使用し、FinalValuesを計算します
	 *
	 * @param context        Modifierを適応するためのデータ
	 * @param baseValues     ベースステータス一覧
	 * @param finalModifiers 適応するModifierのイテレーター
	 * @return Attributeの計算結果
	 */
	public Map<String, Double> calculateStats(@Nullable CalculationContext context, @NotNull Map<String, Double> baseValues, @NotNull Map<String, OperationList> finalModifiers) {
		Map<String, Double> finalValues = new HashMap<>(baseValues);

		// 計算で求めない全てのステータスにデフォルト値とModifierを適応
		for (Map.Entry<String, AttributeDefinition> entry : registry.getAll().entrySet()) {
			AttributeDefinition definition = entry.getValue();
			if (definition.canCalculate()) {
				continue;
			}
			OperationList operation = finalModifiers.get(entry.getKey());

			finalValues.put(entry.getKey(),
				applyModifiers(context,
					finalValues.getOrDefault(entry.getKey(), definition.defaultValue()),
					operation
				)
			);
		}

		// 順番に派生ステータスを計算
		for (AttributeDefinition attr : getSortedDerivedAttributes()) {
			if (!attr.canCalculate()) {
				continue;
			}
			String id = attr.id();
			// 計算に使用する値が存在しないという場合を防ぐ
			for (String dep : attr.dependencies()) {
				if (finalValues.containsKey(dep)) {
					continue;
				}
				Optional<AttributeDefinition> defOptional = registry.get(dep);
				double defaultValue = 0;
				if (defOptional.isEmpty()) {
					ShardLib.getInstance().getLogger().warning("'" + (id) + "' Cannot find dependency attribute '" + dep + "'");
				} else {
					defaultValue = defOptional.get().defaultValue();
				}
				finalValues.put(dep, defaultValue);
			}
			// 計算を実行する
			try {
				double result = attr.calculateValue(Collections.unmodifiableMap(finalValues));
				// modifierを適応
				finalValues.put(id,
					applyModifiers(context, result, finalModifiers.get(id)));
			} catch (Exception e) {
				ShardLib.getInstance().getLogger().warning("Failed to evaluate formula for " + id + ": " + e.getMessage());
			}
		}
		return finalValues;
	}

	public Set<String> getAttributeNames() {
		return registry.getAll().keySet();
	}

	/**
	 * 依存関係を元に計算順序を決定（トポロジカルソート）し、
	 * 計算が必要な派生ステータスのリストを返します。
	 *
	 * @return 計算順序でソートされた派生ステータスのIDリスト
	 */
	private List<AttributeDefinition> getSortedDerivedAttributes() {
		if (!registry.isUpdateAndReset() && sortedDerivedAttributes != null) {
			return sortedDerivedAttributes; // キャッシュがあればそれを返す
		}

		Map<String, AttributeDefinition> attributes = registry.getAll();
		Map<AttributeDefinition, List<AttributeDefinition>> adj = new HashMap<>(); // 依存関係グラフ
		Map<String, Integer> inDegree = new HashMap<>(); // 入次数

		List<AttributeDefinition> derived = attributes.values().stream()
			.filter(attr -> attr.formula() != null)
			.toList();

		for (AttributeDefinition attr : derived) {
			String id = attr.id();
			adj.putIfAbsent(attr, new ArrayList<>());
			inDegree.putIfAbsent(id, 0);
			for (String dep : attr.dependencies()) {
				// 派生ステータス間の依存関係のみをグラフに追加
				AttributeDefinition depAttr = attributes.get(dep);
				if (depAttr != null && attributes.get(dep).formula() != null) {
					adj.putIfAbsent(depAttr, new ArrayList<>());
					adj.get(depAttr).add(attr);
					inDegree.put(id, inDegree.getOrDefault(id, 0) + 1);
				}
			}
		}

		Queue<AttributeDefinition> queue = new LinkedList<>();
		for (AttributeDefinition attr : derived) {
			if (inDegree.getOrDefault(attr.id(), 0) == 0) {
				queue.add(attr);
			}
		}

		List<AttributeDefinition> result = new ArrayList<>();
		while (!queue.isEmpty()) {
			AttributeDefinition u = queue.poll();
			result.add(u);
			if (adj.containsKey(u)) {
				for (AttributeDefinition v : adj.get(u)) {
					inDegree.put(v.id(), inDegree.get(v.id()) - 1);
					if (inDegree.get(v.id()) == 0) {
						queue.add(v);
					}
				}
			}
		}

		if (result.size() != derived.size()) {
			// 循環参照がある場合はエラー
			throw new IllegalStateException("Circular dependency detected in attributes!");
		}

		this.sortedDerivedAttributes = result;
		return result;
	}
}