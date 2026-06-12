package net.logiench.shardCore.core.loot.system;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import net.logiench.shardCore.core.loot.base.LootItem;
import net.logiench.shardCore.core.loot.base.LootTable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LootItemGenerateProvider {
	private final ItemGenerator itemGenerator;
	@Getter
	private final LootTable<LootItem> lootTable;

	@Inject
	public LootItemGenerateProvider(ItemGenerator itemGenerator, @Assisted LootTable<LootItem> lootTable) {
		this.itemGenerator = itemGenerator;
		this.lootTable = lootTable;
	}

	/**
	 * @see LootTable#generate(Object)
	 */
	public List<LootItem> generate(@Nullable Object context) {
		return lootTable.generate(context);
	}

	public List<SuperItemStack> generateItem(@Nullable Object context) {
		return generate(context).stream()
			.map(item -> item.generate(itemGenerator))
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * LootTableから抽選、ItemStackへ変換、配列に一様に散乱するように配置されます。
	 * これは{@link Inventory#setContents(ItemStack[])}で使用することを想定しています。
	 *
	 * @param arrayLength 配列の長さ。{@link Inventory#setContents(ItemStack[])}を使用する場合、ここは{@link Inventory#getSize()}になります。
	 * @return 生成された要素が一様に配置された配列
	 */
	public ItemStack[] generateItemArray(@Nullable Object context, int arrayLength) {
		return Stream.of(generateArray(context, arrayLength))
			.map(SuperItemStack::build)
			.toArray(ItemStack[]::new);
	}

	/**
	 * LootTableから抽選、SuperItemStackへ変換、配列に一様に散乱するように配置されます。
	 * これは{@link Inventory#setContents(ItemStack[])}で使用する前に編集することを想定しています。
	 * もし編集を行わないのであれば、{@link #generateItemArray(Object, int)}を使用することで、より容易に配置ができます。
	 *
	 * @param arrayLength 配列の長さ。{@link Inventory#setContents(ItemStack[])}を使用する場合、ここは{@link Inventory#getSize()}になります。
	 * @return 生成された要素が一様に配置された配列
	 */
	public SuperItemStack[] generateSuperItemArray(@Nullable Object context, int arrayLength) {
		return generateArray(context, arrayLength);
	}

	private SuperItemStack[] generateArray(@Nullable Object context, int arrayLength) {
		List<Integer> indexList = new ArrayList<>(IntStream.range(0, arrayLength).boxed().toList());
		Collections.shuffle(indexList);

		SuperItemStack[] itemArray = new SuperItemStack[arrayLength];
		List<LootItem> generate = generate(context);
		for (int i = 0; i < arrayLength; i++) {
			itemArray[indexList.get(i)] = generate.get(i).generate(itemGenerator);
		}
		return itemArray;
	}

	public interface Factory {
		LootItemGenerateProvider create(LootTable<LootItem> lootTable);
	}
}
