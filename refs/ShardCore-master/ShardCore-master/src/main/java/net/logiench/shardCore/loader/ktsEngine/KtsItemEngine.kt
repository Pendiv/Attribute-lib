package net.logiench.shardCore.loader.ktsEngine

import net.logiench.shardCore.core.item.base.def.ShardItem
import net.logiench.shardCore.loader.ktsScriptConfiguration.ItemScriptConfiguration
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object KtsItemEngine {
	private val host = BasicJvmScriptingHost()

	// 戻り値を Class<ShardItem> から ShardItem (インスタンス) に変更
	fun loadItemScript(file: File): ShardItem {
		val scriptSource = file.toScriptSource()
		val compilationConfiguration = ItemScriptConfiguration

		// スクリプトの実行（評価）設定を作成します
		val evaluationConfiguration = ScriptEvaluationConfiguration {
			jvm {
				// プラグインのクラスローダーを「親（ベース）」として指定します。
				// これにより、スクリプトは独自のコピーを作らず、
				// プラグイン本体のクラス（ShardItemなど）を再利用するようになります。
				baseClassLoader(KtsItemEngine::class.java.classLoader)
			}
		}

		val result = host.eval(scriptSource, compilationConfiguration, evaluationConfiguration)

		if (result.isError()) {
			val messages = result.reports.joinToString("\n") { it.message }
			throw RuntimeException("スクリプトの読み込みに失敗しました: ${file.name}\n$messages")
		}

		// スクリプトの評価結果（最終行の値）を取得
		// ResultValue.Value からインスタンスを取り出す
		val instance = when (
			val evaluationResult = result.valueOrNull()?.returnValue
		) {
			is ResultValue.Value -> evaluationResult.value as? ShardItem
			else -> null
		}

		if (instance == null) {
			throw RuntimeException(
				"""
                ${file.name} の読み込みに失敗しました。
                ファイルの最後にクラスのインスタンスを生成するコードを記述してください。
                例: ObsidianChestplate()
                """.trimIndent()
			)
		}

		return instance
	}
}