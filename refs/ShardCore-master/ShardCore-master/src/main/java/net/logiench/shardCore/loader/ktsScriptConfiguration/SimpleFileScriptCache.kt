package net.logiench.shardCore.loader.ktsScriptConfiguration

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache

// 自作のキャッシュ実装
class SimpleFileScriptCache(val cacheDir: File) : CompiledJvmScriptsCache {
	private val accessedFiles = Collections.synchronizedSet(mutableSetOf<String>())

	init {
		if (!cacheDir.exists()) cacheDir.mkdirs()
	}

	// キャッシュから取得する処理
	override fun get(
		script: SourceCode,
		scriptCompilationConfiguration: ScriptCompilationConfiguration
	): CompiledScript? {
		val cacheFile = File(cacheDir, getCacheFileName(script))
		// ファイルにアクセスした記録を残す
		accessedFiles.add(cacheFile.name)

		if (!cacheFile.exists()) return null

		return try {
			ObjectInputStream(cacheFile.inputStream()).use { it.readObject() as CompiledScript }
		} catch (_: Exception) {
			cacheFile.delete() // 壊れたキャッシュは消す
			null
		}
	}

	// キャッシュを保存する処理
	override fun store(
		compiledScript: CompiledScript,
		script: SourceCode,
		scriptCompilationConfiguration: ScriptCompilationConfiguration
	) {
		val prefix = getScriptIdentifier(script)
		cacheDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { it.delete() }

		val cacheFile = File(cacheDir, getCacheFileName(script))
		accessedFiles.add(cacheFile.name)
		try {
			ObjectOutputStream(cacheFile.outputStream()).use { it.writeObject(compiledScript) }
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	// スクリプト自体の識別子（ファイル名など）を取得
	private fun getScriptIdentifier(script: SourceCode): String {
		// locationId (ファイルパス) からファイル名を取得、またはハッシュ化
		return script.locationId?.let { File(it).nameWithoutExtension } ?: "unknown"
	}

	private fun getCacheFileName(script: SourceCode): String {
		val identifier = getScriptIdentifier(script)
		val hash = script.text.hashCode().toString(16)
		return "${identifier}_${hash}.bin"
	}

	fun resetAccessedFiles() {
//		accessedFiles.clear()
	}

	fun cleanup() {
		val allFiles = cacheDir.listFiles { _, name -> name.endsWith(".bin") } ?: return
		for (file in allFiles) {
			if (!accessedFiles.contains(file.name)) {
				file.delete() // 記録にない＝もう使われていない古いキャッシュ
			}
		}

		resetAccessedFiles()
	}
}