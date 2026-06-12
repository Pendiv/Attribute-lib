package net.logiench.logienchlib.core.config

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.block.implementation.Section
import net.logiench.logienchlib.api.config.LConfig
import net.logiench.logienchlib.api.config.LConfigPath

/**
 * BoostedYAML の [Section] をラップした [LConfig] の共通実装です。
 */
class LConfigImpl internal constructor(
	val section: Section,
	root: LConfigImpl?,
	override val parent: LConfig?,
	/**
	 * この設定が紐付く物理ファイルパス。
	 * ルート（parent == null）かつファイルから読み込んだ場合のみ非 null になります。
	 */
	internal val configPath: LConfigPath?,
) : LConfig {

	constructor(document: YamlDocument, configPath: LConfigPath? = null) :
			this(document, null, null, configPath)

	constructor(section: Section, parent: LConfigImpl) :
			this(section, parent.root, parent, null)

	override val root: LConfigImpl = root ?: this

	override val currentPath: String
		get() = if (section is YamlDocument) "" else section.route.toString()

	override fun getName(): String = section.nameAsString ?: ""

	override fun asSection(): Section = section

	override fun asDocument(): YamlDocument? = section as? YamlDocument

	override fun getSection(path: String): LConfig? {
		val sub = section.getSection(path) ?: return null
		return LConfigImpl(sub, this)
	}

	override fun getKeys(deep: Boolean): Set<String> {
		if (!deep) {
			return section.getKeys().map { it.toString() }.toSet()
		}
		val keys = mutableSetOf<String>()
		collectKeys(section, "", keys)
		return keys
	}

	private fun collectKeys(current: Section, prefix: String, result: MutableSet<String>) {
		for (keyObj in current.getKeys()) {
			val key = keyObj.toString()
			val path = if (prefix.isEmpty()) key else "$prefix.$key"
			result.add(path)
			val sub = current.getSection(key)
			if (sub != null) {
				collectKeys(sub, path, result)
			}
		}
	}

	override fun getValues(deep: Boolean): Map<String, Any?> {
		val values = mutableMapOf<String, Any?>()
		collectValues(this, "", deep, values)
		return values
	}

	private fun collectValues(config: LConfigImpl, prefix: String, deep: Boolean, result: MutableMap<String, Any?>) {
		val current = config.section
		for (keyObj in current.getKeys()) {
			val key = keyObj.toString()
			val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
			val sub = current.getSection(key)
			if (sub != null) {
				val subConfig = LConfigImpl(sub, config)
				if (deep) {
					collectValues(subConfig, fullKey, true, result)
				} else {
					result[fullKey] = subConfig
				}
			} else {
				result[fullKey] = current.get(key)
			}
		}
	}

	override fun contains(path: String): Boolean = section.contains(path)

	override fun isSet(path: String): Boolean = section.contains(path) && section.get(path) != null

	override fun get(path: String): Any? {
		val valObj = section.get(path)
		if (valObj is Section) {
			return LConfigImpl(valObj, this)
		}
		return valObj
	}

	override fun getString(path: String): String? = section.getString(path)

	override fun getInt(path: String): Int? {
		if (!section.contains(path)) return null
		return (section.get(path) as? Number)?.toInt()
	}

	override fun getLong(path: String): Long? {
		if (!section.contains(path)) return null
		return (section.get(path) as? Number)?.toLong()
	}

	override fun getDouble(path: String): Double? {
		if (!section.contains(path)) return null
		return (section.get(path) as? Number)?.toDouble()
	}

	override fun getFloat(path: String): Float? {
		if (!section.contains(path)) return null
		return (section.get(path) as? Number)?.toFloat()
	}

	override fun getBoolean(path: String): Boolean? {
		if (!section.contains(path)) return null
		return section.get(path) as? Boolean
	}

	override fun getList(path: String): List<*>? = section.getList(path)

	override fun getStringList(path: String): List<String> = section.getStringList(path) ?: emptyList()

	override fun getIntList(path: String): List<Int> = section.getIntList(path) ?: emptyList()

	override fun getLongList(path: String): List<Long> = section.getLongList(path) ?: emptyList()

	override fun getDoubleList(path: String): List<Double> = section.getDoubleList(path) ?: emptyList()

	override fun getFloatList(path: String): List<Float> = section.getFloatList(path) ?: emptyList()

	override fun getMapList(path: String): List<Map<*, *>> = section.getMapList(path) ?: emptyList()

	override fun createSection(path: String): LConfig {
		val sub = section.createSection(path)
		return LConfigImpl(sub, this)
	}

	override fun createSection(path: String, values: Map<*, *>): LConfig {
		val sub = section.createSection(path)
		values.forEach { (k, v) ->
			sub.set(k.toString(), v)
		}
		return LConfigImpl(sub, this)
	}

	override fun isSection(path: String): Boolean = section.isSection(path)

	override fun set(path: String, value: Any?) {
		section.set(path, value)
	}

	override fun remove(path: String) {
		section.remove(path)
	}

	override fun save() {
		val doc = section.getRoot()
			?: throw UnsupportedOperationException("このセクションには物理ファイルが紐付いていません。")
		val path = root.configPath
			?: throw UnsupportedOperationException(
				"この LConfig インスタンスは物理ファイルと紐付いていません（空の設定、または Jar 内リソース等の読み取り専用オブジェクトです）。" +
						"save() は実行できません。物理ファイルへ書き出したい場合は、対象の LConfigPath に対して 'path.save(config)' を使用するようにコードを修正してください。"
			)
		doc.save()
	}

	override fun saveTo(path: LConfigPath) {
		path.save(this)
	}
}
