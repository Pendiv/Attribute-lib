package net.logiench.logienchlib.api

import jakarta.inject.Singleton
import net.logiench.logienchlib.api.internal.*

object InstanceHolder {

	enum class State {
		UNINITIALIZED,
		BEFORE_LOADED,
		LOADED,
		BEFORE_ENABLED,
		ENABLED,
	}

	private var state: State = State.UNINITIALIZED

	@Singleton
	class LoadContext(
		val lTaskPoolService: LTaskPoolService,
		val configService: ConfigService
	)

	@Singleton
	class EnableContext(
		val itemBuilderFactory: ItemBuilderFactory,
		val inventoryMenuService: InventoryMenuService,
		val timerService: TimerService,
	)

	internal lateinit var itemBuilderFactory: ItemBuilderFactory
	internal lateinit var inventoryMenuService: InventoryMenuService
	internal lateinit var timerService: TimerService
	internal lateinit var lTaskPoolService: LTaskPoolService
	internal lateinit var configService: ConfigService

	@Deprecated(level = DeprecationLevel.ERROR, message = "内部API")
	fun setLoad(context: LoadContext) {
		if (state != State.BEFORE_LOADED) throw IllegalStateException("APIの注入は現在行えません")

		this.lTaskPoolService = context.lTaskPoolService
		this.configService = context.configService

		state = State.LOADED
	}

	@Deprecated(level = DeprecationLevel.ERROR, message = "内部API")
	fun setEnabled(context: EnableContext) {
		if (state != State.BEFORE_ENABLED) throw IllegalStateException("APIの注入は現在行えません")

		this.timerService = context.timerService
		this.itemBuilderFactory = context.itemBuilderFactory
		this.inventoryMenuService = context.inventoryMenuService

		state = State.ENABLED
	}
}