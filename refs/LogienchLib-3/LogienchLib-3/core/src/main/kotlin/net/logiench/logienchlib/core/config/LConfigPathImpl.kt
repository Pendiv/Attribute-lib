package net.logiench.logienchlib.core.config

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import net.logiench.logienchlib.api.config.LConfig
import net.logiench.logienchlib.api.config.LConfigPath
import net.logiench.logienchlib.api.internal.ConfigPathService
import java.io.File

/**
 * BoostedYAML を使用した [LConfigPath] の共通実装です。
 */
class LConfigPathImpl(
	private val file: File,
	private val pathService: ConfigPathService,
	private val plugin: Any?,
) : LConfigPath {

	override fun toFile(): File = file

	override fun exists(): Boolean = file.exists()

	override fun isDirectory(): Boolean = file.isDirectory

	override fun loadResource(): LConfig {
		val p = plugin
			?: throw IllegalStateException("プラグインインスタンスが登録されていないため、Jar内のリソースを特定できません。")
		val stream = pathService.getResource(p, file.name)
			?: throw IllegalStateException("Jar 内にリソース '${file.name}' が見つかりませんでした。")
		return stream.use { input ->
			val doc = YamlDocument.create(input)
			LConfigImpl(doc)
		}
	}

	override fun saveResource(replace: Boolean): Boolean {
		return saveResource(file.name, replace)
	}

	override fun saveResource(resourcePath: String, replace: Boolean): Boolean {
		val p = plugin ?: return false
		if (file.exists() && !replace) return true

		val stream = pathService.getResource(p, resourcePath) ?: return false
		file.parentFile?.mkdirs()
		stream.use { input ->
			file.outputStream().use { output ->
				input.copyTo(output)
			}
		}
		return true
	}

	override fun load(): LConfig {
		if (file.isDirectory) throw IllegalArgumentException("ディレクトリは読み込めません")
		val doc = YamlDocument.create(file)
		return LConfigImpl(doc, this)
	}

	override fun loadWithUpdate(): LConfig {
		return loadWithUpdate(UpdaterSettings.DEFAULT)
	}

	override fun loadWithUpdate(updaterSettings: UpdaterSettings): LConfig {
		val p = plugin
			?: throw IllegalStateException("プラグインインスタンスが登録されていないため、マージ元リソースを取得できません。")
		val stream = pathService.getResource(p, file.name)
			?: throw IllegalStateException("Jar 内にデフォルトのリソース '${file.name}' が見つかりませんでした。")

		val doc = stream.use { input ->
			YamlDocument.create(
				file,
				input,
				GeneralSettings.DEFAULT,
				LoaderSettings.builder().setAutoUpdate(true).build(),
				DumperSettings.DEFAULT,
				updaterSettings
			)
		}
		return LConfigImpl(doc, this)
	}

	override fun loadAllInDirectory(extensions: Collection<String>): Map<String, LConfig> {
		if (!file.exists() || !file.isDirectory) return emptyMap()
		val files = file.listFiles() ?: return emptyMap()

		return files.filter { it.isFile && extensions.contains(it.extension) }
			.associate { it.nameWithoutExtension to LConfigPathImpl(it, pathService, plugin).load() }
	}

	override fun save(config: LConfig) {
		val doc = config.asDocument()
			?: throw IllegalArgumentException("保存可能な LConfigImpl インスタンスではありません。")
		doc.save()
	}
}
