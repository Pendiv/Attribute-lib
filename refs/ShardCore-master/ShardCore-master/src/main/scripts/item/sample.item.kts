import net.kyori.adventure.text.Component
import net.logiench.shardCore.core.item.base.def.ChestplateItem
import net.logiench.shardCore.core.item.base.def.Rarity
import net.logiench.shardCore.core.itemRequirement.base.RequirementDef
import net.logiench.shardCore.core.stats.base.AttributeEnum
import net.logiench.shardCore.data.itemRequirement.MinLevelReqType
import net.logiench.shardCore.data.stats.keys.CoreStats
import java.util.*

/*
itemディレクトリ内のすべての .item.kts ファイルが読み込まれます。
フォルダを作成した場合、その中も探索されます。
 */
class KotlinScriptingChestplate : ChestplateItem() {
	override val id = "sample"
	override val name = Component.text("KotlinScriptingChestplate")
	override val rarity = Rarity.COMMON

	override val uniqueBaseStats = TreeMap<AttributeEnum, Double>().apply {
		put(CoreStats.HP_REGEN, 5.0)
		put(CoreStats.MAX_HP, 20.0)
	}

	override val mainStats = TreeMap<AttributeEnum, Double>().apply {
		// ここでMainStatsの要素を追加していく
		// メソッドで毎回作成するよりもフィールドで定数にして取得のほうがより高速
		put(CoreStats.ATTACK_SPEED, 2.0)
		put(CoreStats.CRITICAL_CHANCE, 5.0)
	}

	override val requirementDefs = listOf<RequirementDef<*>>(
		MinLevelReqType.MinLevelDef(10),
	)
}

// 最後にこのクラスのインスタンスを生成します。この行は必須です
KotlinScriptingChestplate()