package net.logiench.shardCore.loader.ktsScriptConfiguration

import net.logiench.shardCore.core.item.base.def.ShardItem
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

/*
.item.kts という拡張子をこの設定に紐付ける
-> をしたかったが、うまく読み込まれず断念。
importだけは正常にされてるけどIntellJの補完が働かない
*/
@KotlinScript(
	displayName = "ItemDefinition",
	fileExtension = "item.kts",
	compilationConfiguration = ItemScriptConfiguration::class
)
abstract class ItemScript

object ScriptCache {
	val instance = SimpleFileScriptCache(File("plugins/ShardCore/cache/item").absoluteFile)
}

object ItemScriptConfiguration : ScriptCompilationConfiguration({
	// 1. デフォルトのインポート定義
	defaultImports(
		"net.logiench.shardCore.core.stats.base.AttributeEnum",
		"net.logiench.shardCore.core.item.base.Rarity",
		"net.logiench.shardCore.core.item.base.ShardItem",
		"net.logiench.shardCore.core.item.base.ChestplateItem",
		"net.logiench.shardCore.data.stats.keys.item.ItemStats",
		"net.logiench.shardCore.core.item.system.generator.processor.data.ArmorContext",
		"net.logiench.shardCore.core.itemRequirement.base.ItemRequirement",
		"net.logiench.shardCore.data.itemRequirement.type.MinLevelRequirement",
		"net.logiench.shardCore.data.stats.keys.CoreStats",

		"net.kyori.adventure.text.Component",
		"org.bukkit.Material",
		"java.util.*"
	)

	jvm {
		compilerOptions("-jvm-target", "21")

		// 1. プラグインのJarファイルを取得
		// ShardItemクラスが入っているJarファイル（プラグイン本体）の場所を特定します
		val pluginJar = File(ShardItem::class.java.protectionDomain.codeSource.location.toURI())

		// 2. クラスパスを更新
		// dependencies(...) の代わりにこれを使います
		updateClasspath(listOf(pluginJar))

		dependenciesFromCurrentContext(wholeClasspath = true)
	}

	hostConfiguration(ScriptingHostConfiguration {
		jvm {
			// ここで自作のキャッシュを指定
			compilationCache(ScriptCache.instance)
		}
	})

	refineConfiguration {
		beforeCompiling { context ->
			val pluginJar = File(ShardItem::class.java.protectionDomain.codeSource.location.toURI())
			context.compilationConfiguration.with {
				jvm {
					updateClasspath(listOf(pluginJar))
				}
			}.asSuccess()
		}
	}
}) {
	private fun readResolve(): Any = ItemScriptConfiguration
}