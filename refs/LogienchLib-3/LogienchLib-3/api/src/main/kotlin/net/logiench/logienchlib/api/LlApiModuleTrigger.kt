package net.logiench.logienchlib.api

import io.avaje.inject.InjectModule
import net.logiench.logienchlib.api.internal.UnsupportedService

/**
 * APIモジュール用のAvaje Injectトリガークラス。
 */
@InjectModule(
	name = "ll-api",
	requiresPackages = [
		// このクラスのあるパッケージの実装はすべて外部に頼る
		UnsupportedService::class
	]
)
class LlApiModuleTrigger
