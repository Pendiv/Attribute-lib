package net.logiench.shardCore.core.item.base.module.tools;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StructuredLore {

	private final Map<LoreSection, List<Component>> sections = new EnumMap<>(LoreSection.class);

	public StructuredLore() {
	}

	public StructuredLore(List<Component> flatLore, @NotNull Map<LoreSection, Integer> sectionIndexes, boolean hasSeparator) {
		List<Map.Entry<LoreSection, Integer>> sortedEntries = sectionIndexes.entrySet().stream()
			.sorted(Map.Entry.comparingByValue())
			.toList();

		for (int i = 0; i < sortedEntries.size(); i++) {
			LoreSection currentSection = sortedEntries.get(i).getKey();
			int startIndex = sortedEntries.get(i).getValue();

			int endIndex;
			if (i + 1 < sortedEntries.size()) {
				endIndex = sortedEntries.get(i + 1).getValue();
				if (hasSeparator) {
					endIndex--; // セパレーターの行をスキップする
				}
			} else {
				endIndex = flatLore.size(); // 最後のセクションはリストの最後まで
			}

			sections.put(currentSection, new ArrayList<>(flatLore.subList(startIndex, endIndex)));
		}
	}

	public void add(LoreSection section, Component component) {
		sections.computeIfAbsent(section, k -> new ArrayList<>()).add(component);
	}

	public void addAll(LoreSection section, Component @NotNull ... components) {
		addAll(section, Arrays.asList(components));
	}

	public void addAll(LoreSection section, @NotNull List<Component> components) {
		sections.computeIfAbsent(section, k -> new ArrayList<>()).addAll(components);
	}

	public void set(LoreSection section, int index, Component component) {
		List<Component> lore = sections.get(section);
		if (lore == null) {
			throw new NullPointerException("セクション " + section + " は存在しません");
		}
		lore.set(index, component);
	}

	public void setAll(LoreSection section, @NotNull List<Component> components) {
		sections.put(section, new ArrayList<>(components));
	}

	@NotNull
	public List<Component> getOrCreateSection(LoreSection section) {
		return sections.computeIfAbsent(section, k -> new ArrayList<>());
	}

	@Nullable
	public List<Component> getSection(LoreSection section) {
		return sections.get(section);
	}

	/**
	 * {@link #join()}した際の各セクションごとの開始地点を取得します
	 *
	 * @return 各セクションごとの開始地点。0 ~ size() までの範囲です
	 */
	public Map<LoreSection, Integer> getSectionIndexes(boolean useSeparator) {
		Map<LoreSection, Integer> result = new EnumMap<>(LoreSection.class);
		int currentOffset = 0;
		boolean isFirst = true;

		for (Map.Entry<LoreSection, List<Component>> entry : sections.entrySet()) {
			List<Component> lore = entry.getValue();
			if (lore.isEmpty()) {
				continue;
			}

			// 最初以外のセクションでセパレーターが挿入される場合、オフセットを+1する
			if (!isFirst && useSeparator) {
				currentOffset++;
			}

			result.put(entry.getKey(), currentOffset);
			currentOffset += lore.size();
			isFirst = false;
		}
		return result;
	}

	public List<Component> join() {
		return sections.values().stream()
			.flatMap(List::stream)
			.toList();
	}

	public List<Component> join(Component separator) {
		return sections.values().stream()
			// 空のLoreは無視する
			.filter(l -> !l.isEmpty())
			// 最初以外の要素追加時はseparatorを挿入する
			.collect(
				ArrayList::new,
				(list, section) -> {
					if (!list.isEmpty()) {
						list.add(separator);
					}
					list.addAll(section);
				},
				ArrayList::addAll
			);
	}
}
