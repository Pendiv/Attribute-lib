package net.logiench.shardCore.data.item.module.stats;

import com.google.gson.reflect.TypeToken;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.Rarity;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.params.GenParamKey;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.core.stats.base.view.AttributeValue;
import net.logiench.shardCore.core.stats.base.view.SubStatsView;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.NavigableSet;

public class StatsKeys {
	public static final ContextKey<Map<AttributeEnum, MainStatsModule.ValueAndCompleteness>> CTX_MAIN_STATS = new ContextKey<>("main_stats");
	public static final ContextKey<NavigableSet<AttributeValue>> CTX_SUB_STATS = new ContextKey<>("sub_stats");
	public static final ContextKey<Map<AttributeEnum, Double>> CTX_UNIQUE_STATS = new ContextKey<>("unique_stats");

	/**
	 * サブステータスの抽選を行う対象と重みが含まれた一覧表を指定します。
	 * 指定されない場合は {@link SubStatsView#DEFAULT} が使用されます。
	 */
	public static final GenParamKey<SubStatsView> GEN_SUB_STATS_VIEW = new GenParamKey<>("sub_stats_view", SubStatsView.class);
	/**
	 * {@link Rarity#getSubStatsCount()} に追加で適応できる最大のサブステータスの数を設定します。
	 */
	public static final GenParamKey<Integer> GEN_ADDITIONAL_SUB_STATS_AMOUNT = new GenParamKey<>("add_sub_stats_amount", Integer.class);
	/**
	 * メインステータスの完成度に下限を設定します。
	 * Doubleは 0~100までで、Mapの対応する項目に下限が設定されます。
	 */
	// ジェネリクス型は TypeToken を使う
	public static final GenParamKey<Map<AttributeEnum, Double>> GEN_COMPLETENESS_MIN =
		new GenParamKey<>("mainCompleteMin", new TypeToken<>() {});
	/**
	 * メインステータスの完成度に上限を設定します。
	 * Doubleは 0~100までで、対応する項目に上限が設定されます。指定された値までが含まれます。
	 */
	public static final GenParamKey<Map<AttributeEnum, Double>> GEN_COMPLETENESS_MAX =
		new GenParamKey<>("mainCompleteMax", new TypeToken<>() {});

	static final ContainerKey<String, String> PDC_MAIN_COMPLETENESS = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "stats_comp");
	static final ContainerKey<String, String> PDC_SUB_STATS = new ContainerKey<>(PersistentDataType.STRING, ShardCore.getInstance(), "stats_sub");
}
