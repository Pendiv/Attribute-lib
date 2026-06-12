package net.logiench.shardCore.core.loot.base;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.data.item.module.level.LevelKeys;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lootで出現するアイテムのデータを格納します。
 *
 * @param item           生成する対象となるアイテムのクラス
 * @param minLevel       アイテムの最低レベル。-1にすると無効になります。この値も抽選の範囲に含まれます。<code>maxLevel</code>が指定されていて、この値が無効になった場合、0が最低値になります。
 * @param maxLevel       アイテムの最大レベル。-1にすると無効になります。この値も抽選の範囲に含まれます。<code>minLevel</code>が指定されていて、この値が無効になった場合、{@link Long#MAX_VALUE}-1 が最大値になります。
 * @param params         アイテムの生成時に設定されるパラメータ
 * @param isUnidentified 鑑定する必要のあるアイテムを生成するか
 */
public record LootItemData(
	ShardItem item,
	long minLevel,
	long maxLevel,
	@Nullable GenerationParameters params,
	boolean isUnidentified
) implements LootItem {
	public LootItemData {
		if (minLevel != -1 && maxLevel != -1 && minLevel > maxLevel) {
			throw new IllegalArgumentException("レベルの下限が上限を超えています。 最低値 %d, 最大値 %d".formatted(minLevel, maxLevel));
		}
	}

	public LootItemData(ShardItem item, boolean isUnidentified) {
		this(item, null, isUnidentified);
	}

	public LootItemData(ShardItem item, GenerationParameters params, boolean isUnidentified) {
		this(item, -1, -1, params, isUnidentified);
	}

	public LootItemData(ShardItem item, long minLevel, long maxLevel, boolean isUnidentified) {
		this(item, minLevel, maxLevel, null, isUnidentified);
	}

	@Override
	public @Nullable SuperItemStack generate(ItemGenerator generator) {
		GenerationParameters params = this.params;
		if (hasLevelConditions()) {
			if (params == null) {
				params = GenerationParameters.of();
			}
			params.put(LevelKeys.GEN_LEVEL, getRandomLevel());
		}

		if (isUnidentified()) {
			return generator.generateUnidentified(item, params).printMessage().item();
		}
		return generator.generateNew(item, params).printMessage().item();
	}

	/**
	 * このルートアイテムデータがレベルの条件を持っているか
	 */
	private boolean hasLevelConditions() {
		// min, maxどちらも-1だとデータはない、片方でも-1以外ならあり
		return minLevel != -1 || maxLevel != -1;
	}

	/**
	 * レベルの制限に従ってランダムに抽選します
	 *
	 * @return 抽選されたランダムな値
	 *
	 * @throws IllegalStateException このデータがレベルの制限情報を持たない場合
	 */
	private long getRandomLevel() {
		if (minLevel == maxLevel) {
			return minLevel;
		}
		// Long.MAX_VALUE がmaxLevelに指定されると、値を含ませるために +1 するため、オーバーフローしてminLevelよりも小さくなる場合がある
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		if (minLevel == -1) {
			return rand.nextLong(maxLevel + 1);
		}
		if (maxLevel == -1) {
			return rand.nextLong(minLevel, Long.MAX_VALUE);
		}
		return rand.nextLong(minLevel, maxLevel + 1);
	}
}
