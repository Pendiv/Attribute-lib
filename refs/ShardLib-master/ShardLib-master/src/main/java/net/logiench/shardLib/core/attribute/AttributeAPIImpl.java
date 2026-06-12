package net.logiench.shardLib.core.attribute;

import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import net.logiench.logienchlibv2.api.minecraft.time.TimeTask;
import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.DoubleEditFunction;
import net.logiench.shardLib.api.attribute.Ticket;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.attribute.data.AttributeOperationModifier;
import net.logiench.shardLib.api.attribute.data.StackingRule;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.core.attribute.data.OperationList;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AttributeAPIImpl implements AttributeAPI {
	final Map<String, Double> baseAttributes = new HashMap<>();
	final Map<String, OperationList> finalModifiers = new HashMap<>();
	final Map<AttributeOperationModifier, TimeTask> modifierTimers = new HashMap<>();
	private final Map<String, Double> finalAttributes = new HashMap<>();
	final AttributeManager attributeManager;
	private final AttributeDefinitionRegister registry;
	boolean isDirtyToMob = false;

	public AttributeAPIImpl(AttributeManager attributeManager, AttributeDefinitionRegister registry) {
		this.attributeManager = attributeManager;
		this.registry = registry;
	}

	@Override
	@NotNull
	public Map<String, Double> getFinalAttributes() {
		return Map.copyOf(finalAttributes);
	}

	@Override
	public double getFinalAttribute(String attributeId) throws IllegalArgumentException {
		checkContains(attributeId);
		return finalAttributes.get(attributeId);
	}

	@Override
	public @NotNull Optional<Double> getFinalAttributeOptional(String attributeId) {
		return Optional.ofNullable(finalAttributes.get(attributeId));
	}

	@Override
	@NotNull
	public Map<String, Double> getBaseAttributes() {
		return Map.copyOf(baseAttributes);
	}

	@Override
	public double getBaseAttribute(String attributeId) throws IllegalArgumentException {
		AttributeDefinition definition = getDefinition(attributeId);
		return baseAttributes.getOrDefault(attributeId, definition.defaultValue());
	}

	@Override
	public @NotNull Optional<Double> getBaseAttributeOptional(String attributeId) {
		return Optional.ofNullable(baseAttributes.get(attributeId));
	}

	@Override
	public void setBaseAttribute(String attributeId, double value) throws IllegalArgumentException {
		checkContains(attributeId);
		baseAttributes.put(attributeId, value);
		isDirtyToMob = true;
	}

	@Override
	public void setBaseAttributes(Map<String, Double> attributes) throws IllegalArgumentException {
		// containsAllだとどのAttributeがないかが表示できないからforで処理
		for (Map.Entry<String, Double> entry : attributes.entrySet()) {
			checkContains(entry.getKey());
			baseAttributes.put(entry.getKey(), entry.getValue());
		}
		isDirtyToMob = true;
	}

	@Override
	public void removeBaseAttribute(String attributeId) throws IllegalArgumentException {
		checkContains(attributeId);
		baseAttributes.remove(attributeId);
		isDirtyToMob = true;
	}

	@Override
	public double editBaseAttribute(String attributeId, DoubleEditFunction editFunction) throws IllegalArgumentException {
		double baseValue = getBaseAttribute(attributeId);
		double newValue = editFunction.apply(baseValue);
		baseAttributes.put(attributeId, newValue);
		isDirtyToMob = true;
		return newValue;
	}

	@Override
	public double addBaseAttribute(String attributeId, double value) throws IllegalArgumentException {
		AttributeDefinition definition = getDefinition(attributeId);
		double baseValue = baseAttributes.getOrDefault(attributeId, definition.defaultValue());
		double newValue = baseValue + value;
		baseAttributes.put(attributeId, newValue);
		isDirtyToMob = true;
		return newValue;
	}

	@Override
	public double subtractBaseAttribute(String attributeId, double value) throws IllegalArgumentException {
		AttributeDefinition definition = getDefinition(attributeId);
		double baseValue = baseAttributes.getOrDefault(attributeId, definition.defaultValue());
		double newValue = baseValue - value;
		baseAttributes.put(attributeId, newValue);
		isDirtyToMob = true;
		return newValue;
	}

	@Override
	public void clearBaseAttributes() {
		baseAttributes.clear();
		isDirtyToMob = true;
	}

	List<AttributeOperationModifier> getExistingModifiers(String sourceId) {
		List<AttributeOperationModifier> modifiers = new ArrayList<>();
		for (OperationList list : finalModifiers.values()) {
			for (AttributeOperationModifier modifier : list) {
				if (modifier.getSourceId().equals(sourceId)) {
					modifiers.add(modifier);
				}
			}
		}
		return modifiers;
	}

	@Override
	@NotNull
	public Ticket addModifier(AttributeModifier modifier) throws IllegalArgumentException {
		StackingRule rule = modifier.getStackingRule();
		Ticket ticket;
		if (rule == StackingRule.STACKABLE) {
			ticket = addOperationModifier(modifier);
		} else {
			// 同じsourceIdを持つModifierのみを全て集める
			List<AttributeModifier> existingModifiers = getExistingModifiers(modifier.getSourceId()).stream()
				.filter(m -> m instanceof AttributeModifier)
				.map(m -> (AttributeModifier) m)
				.toList();

			ticket = switch (modifier.getStackingRule()) {
				case DENY -> handleDeny(modifier, existingModifiers);
				case REPLACE -> handleReplace(modifier, existingModifiers);
				case HIGHEST_WINS -> handleHighestWins(modifier, existingModifiers);
				case LOWEST_WINS -> handleLowestWins(modifier, existingModifiers);
				default -> throw new IllegalArgumentException("Unknown modifier: " + modifier);
			};
		}
		// isDirtyは高頻度で読み書き(ロードアンロード)が行われるモブに対してのみ使用しているのでToMobでOK
		if (modifier.isPersistentToMob()) {
			isDirtyToMob = true;
		}
		return ticket;
	}

	Ticket addOperationModifier(AttributeOperationModifier modifier) throws IllegalArgumentException {
		checkContains(modifier.getSourceId());
		Ticket ticket = () -> removeOperationModifier(modifier);
		finalModifiers.computeIfAbsent(modifier.getTargetAttributeId(), k -> new OperationList())
			.add(modifier);

		modifier.getDurationTicks().ifPresent(
			tick -> modifierTimers.put(
				modifier, Delay.on(ticket::remove, tick)
			)
		);
		isDirtyToMob = true;
		return ticket;
	}

	@Override
	public void removeModifier(AttributeModifier modifier) {
		removeOperationModifier(modifier);
	}

	@Override
	public void removeModifiers(String sourceId) {
		getExistingModifiers(sourceId).forEach(this::removeOperationModifier);
	}

	@Override
	public void removeAll() {
		for (OperationList list : finalModifiers.values()) {
			while (!list.isEmpty()) {
				removeOperationModifier(list.getFirst());
			}
		}
	}

	void removeOperationModifier(AttributeOperationModifier modifier) {
		OperationList list = finalModifiers.get(modifier.getTargetAttributeId());
		if (list == null || !list.remove(modifier)) {
			return;
		}
		TimeTask timeTask = modifierTimers.remove(modifier);
		if (timeTask != null) {
			timeTask.cancel();
		}
		isDirtyToMob = true;
	}

	@Override
	public List<AttributeOperationModifier> getModifiers() {
		return finalModifiers.values()
			.stream()
			.reduce(new OperationList(), (l, v) -> {
				l.addAll(v);
				return l;
			}).toList();
	}

	public boolean isDirtyToMob() {
		return isDirtyToMob;
	}

	public void resetDirty() {
		isDirtyToMob = false;
	}

	private void checkContains(String attributeId) throws IllegalArgumentException {
		if (registry.contains(attributeId)) {
			return;
		}
		throw new IllegalArgumentException("Attribute '" + attributeId + "' does not exist");
	}

	@NotNull
	private AttributeDefinition getDefinition(String attributeId) throws IllegalArgumentException {
		Optional<AttributeDefinition> optional = registry.get(attributeId);
		if (optional.isPresent()) {
			return optional.get();
		}
		throw new IllegalArgumentException("Attribute '" + attributeId + "' does not exist");
	}

	/// {@link StackingRule#DENY}
	private Ticket handleDeny(AttributeModifier newModifier, List<AttributeModifier> existingModifiers) {
		if (existingModifiers.isEmpty()) {
			return addOperationModifier(newModifier);
		}
		return () -> {};
	}

	/// {@link StackingRule#REPLACE}
	private Ticket handleReplace(AttributeModifier newModifier, List<AttributeModifier> existingModifiers) {
		existingModifiers.forEach(this::removeOperationModifier);
		return addOperationModifier(newModifier);
	}

	/// {@link StackingRule#HIGHEST_WINS}
	private Ticket handleHighestWins(AttributeModifier newModifier, List<AttributeModifier> existingModifiers) {
		List<AttributeModifier> candidates = new ArrayList<>(existingModifiers);
		candidates.add(newModifier);

		AttributeModifier winner = candidates.stream()
			.max(Comparator.comparingDouble(AttributeModifier::getValue))
			.orElse(newModifier);

		return removeLosers(newModifier, winner, existingModifiers);
	}

	/// {@link StackingRule#LOWEST_WINS}
	private Ticket handleLowestWins(AttributeModifier newModifier, List<AttributeModifier> existingModifiers) {
		List<AttributeModifier> candidates = new ArrayList<>(existingModifiers);
		candidates.add(newModifier);

		AttributeModifier winner = candidates.stream()
			.min(Comparator.comparingDouble(AttributeModifier::getValue))
			.orElse(newModifier);

		return removeLosers(newModifier, winner, existingModifiers);
	}

	/// 敗者を適切に削除し、勝者を維持、追加します
	private Ticket removeLosers(AttributeModifier newModifier, AttributeModifier winner, List<AttributeModifier> existingModifiers) {
		if (winner == newModifier) {
			// 新しいModifierが勝った場合: 既存のものを全て削除し、新しいものを追加する
			existingModifiers.forEach(this::removeOperationModifier);

			return addOperationModifier(newModifier);
		} else {
			// 既存のModifierが勝った場合: 勝者以外の既存Modifierを削除する。新しいModifierはそもそも追加しない
			// 敗者（勝者ではない既存Modifier）をリストアップ
			List<AttributeModifier> losers = existingModifiers.stream()
				.filter(m -> m != winner)
				.toList();

			// 敗者を削除
			losers.forEach(this::removeOperationModifier);

			return () -> removeOperationModifier(winner);
		}
	}

	Map<String, Double> getRecalculatedStats() {
		return attributeManager.calculateStats(
			null,
			baseAttributes,
			finalModifiers
		);
	}

	@Override
	public void recalculateStats() {
		Map<String, Double> recalculatedStats = getRecalculatedStats();
		finalAttributes.clear();
		finalAttributes.putAll(recalculatedStats);
	}
}
