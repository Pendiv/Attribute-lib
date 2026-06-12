package net.logiench.logienchlib.api.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

fun Component.parseString(): String = ComponentUtil.string(this)
fun Iterable<Component>.parseString(): List<String> = ComponentUtil.string(this)

@JvmName("nullableParseString")
fun Iterable<Component?>.parseString(): List<String?> = ComponentUtil.string(this)

object ComponentUtil {

	const val DEFAULT_TARGET: Char = '§'

	private val NO_ITALIC: TextComponent =
		Component.empty().style(Style.style(TextDecoration.ITALIC.withState(TextDecoration.State.FALSE)))

	private val plainSerializer: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()

	/**
	 * [TextDecoration.ITALIC]がfalseな状態で取得します
	 */
	@JvmStatic
	fun empty(): TextComponent = NO_ITALIC

	/**
	 * [TextDecoration.ITALIC]がfalseな状態で取得します
	 */
	@JvmStatic
	fun builder(): TextComponent.Builder = NO_ITALIC.toBuilder()


	@JvmStatic
	fun string(component: Component): String = plainSerializer.serialize(component)

	@JvmStatic
	fun string(components: Iterable<Component>): List<String> = components.map { string(it) }

	@JvmStatic
	@JvmName("nullableString")
	fun string(component: Component?): String? = component?.let { plainSerializer.serialize(component) }

	@JvmStatic
	@JvmName("nullableString")
	fun string(components: Iterable<Component?>): List<String?> = components.map { string(it) }

	/**
	 * [target]に指定した記号で色指定なども含まれた形に復元します。
	 */
	@JvmStatic
	fun legacyString(component: Component, target: Char = DEFAULT_TARGET): String =
		LegacyComponentSerializer.legacy(target).serialize(component)

	/**
	 * [target]に指定した記号で色指定なども含まれた形に復元します。
	 * RGB指定された`§a`のような形で存在しないものは`§#FFAA00`のように16新数でパースされるようになります。
	 */
	@JvmStatic
	fun legacyHexString(component: Component, target: Char = DEFAULT_TARGET): String =
		LegacyComponentSerializer.builder().character(target).hexColors().build().serialize(component)


	@JvmStatic
	fun text(string: String?, target: Char = DEFAULT_TARGET): Component =
		string?.let { LegacyComponentSerializer.legacy(target).deserialize(string) }
			?: Component.empty()

	@JvmStatic
	fun text(strings: Iterable<String?>, target: Char = DEFAULT_TARGET): List<Component> =
		strings.map { text(it, target) }

	@JvmStatic
	fun mini(string: String?): Component =
		string?.let { MiniMessage.miniMessage().deserialize(string) } ?: Component.empty()

	@JvmStatic
	fun mini(strings: Iterable<String?>): List<Component> =
		strings.map { mini(it) }
}