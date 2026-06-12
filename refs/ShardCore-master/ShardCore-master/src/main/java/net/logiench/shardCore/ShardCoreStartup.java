package net.logiench.shardCore;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.command.ShardCommand;
import net.logiench.shardCore.config.data.DatabaseConfigState;
import net.logiench.shardCore.core.damage.DamageEventListener;
import net.logiench.shardCore.core.item.system.gem.GemEventListener;
import net.logiench.shardCore.core.menu.MenuNavigationManager;
import net.logiench.shardCore.core.player.system.PlayerSessionManager;
import net.logiench.shardCore.core.player.system._PlayerCharacterManager;
import net.logiench.shardCore.core.skill.system.SkillManager;
import net.logiench.shardCore.data.loot.LootItemRegisterProvider;
import net.logiench.shardCore.db.DatabaseManager;
import net.logiench.shardCore.db.ServiceManager;
import net.logiench.shardCore.di.ConfigModule;
import net.logiench.shardCore.di.FactoryModule;
import net.logiench.shardCore.di.RegisterModule;
import net.logiench.shardCore.listener.EventListener;
import net.logiench.shardCore.listener.LimboProtectionListener;
import net.logiench.shardCore.loader.KtsItemLoader;
import net.logiench.shardCore.util.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * サーバーの起動時に行う処理を書くクラス。
 * 実行された処理にエラーがあった場合はサーバーがシャットダウンされるため、フェールセーフです。
 */
public class ShardCoreStartup {

	private static final List<Class<? extends Listener>> EVENT_LISTENERS = List.of(
		// 1. 汎用 & テスト用
		EventListener.class,
		// 2. プレイヤーやエンティティのダメージ管理
		DamageEventListener.class,
		// 3. プレイヤー
		// 3.1. プレイヤーセッション管理
		LimboProtectionListener.class,
		PlayerSessionManager.class,
		_PlayerCharacterManager.class,
		// 3.2. DB
		ServiceManager.class,
		// 3.3. メニュー
		MenuNavigationManager.class,
		// 3.4. スキル
		SkillManager.class,
		// 3.5. ジェム
		GemEventListener.class
	);

	private final ShardCore plugin;
	@Getter
	private Injector injector;
	private STARTUP_PHASE phase = STARTUP_PHASE.BEFORE_LOAD;

	// disable時に専用処理をする必要のある変数
	private DatabaseManager databaseManager;
	private ServiceManager serviceManager;

	// disable用変数ここまで

	public ShardCoreStartup(ShardCore plugin) {
		this.plugin = plugin;
	}

	public void load() throws RuntimeException {
		if (phase != STARTUP_PHASE.BEFORE_LOAD) {
			return;
		}
		try {
			/*
			処理順
			1. 非同期処理の開始命令
			2. エラーの発生しやすい処理
			3. 軽い処理
			4. 重い処理(これはできれば非同期に対応させる)
			ただし処理の順番が重要な場合を除く
			 */
			this.injector = Guice.createInjector(
				new RegisterModule(),
				new FactoryModule(),
				new ConfigModule()
			);

			// 1. 非同期でktsのロードを開始する
			injector.getInstance(KtsItemLoader.class).startAsyncRegistryAll(
				plugin.getDataFolder().toPath().resolve("item").toFile(),
				Path.of("item", "sample.item.kts")
			);

			// 2. DBに接続し、テーブルをすべて作成する
			this.databaseManager = injector.getInstance(DatabaseManager.class);
			databaseManager.createTables();

			// 3.
			injector.getInstance(LootItemRegisterProvider.class).registerDefaults();

			// 4. ファイルをスキャンしてサービスに登録
			this.serviceManager = injector.getInstance(ServiceManager.class);
			serviceManager.loadServiceClasses();

			/*if (true) {
				throw new RuntimeException("作りかけなので強制終了");
			}*/
		} catch (Exception e) {
			onException(e);
		}
		this.phase = STARTUP_PHASE.LOAD;
	}

	public void enable() throws RuntimeException {
		if (phase != STARTUP_PHASE.LOAD) {
			return;
		}
		try {
			/*
			 処理順
			 一番後ろ. 定期処理用Timerのスタート
			 基本的に必要な順番。それ以外はloadと同じ
			 */
			PluginManager pluginManager = plugin.getServer().getPluginManager();
			// イベントリスナーとして登録するクラスたち。Injectorによってインスタンスが作成されます
			for (Class<? extends Listener> listener : EVENT_LISTENERS) {
				pluginManager.registerEvents(injector.getInstance(listener), plugin);
			}

			// コマンド登録
			plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, command -> {
				Commands commands = command.registrar();

				// 特定のディレクトリ内のすべてのコマンドクラスを取得
				for (Class<? extends ShardCommand> clazz : ClassUtils.findSubClasses(
					ShardCommand.class, "net.logiench.shardCore.command")) {
					ShardCommand shardCommand = injector.getInstance(clazz);

					// registerでコマンドを構築
					LiteralCommandNode<CommandSourceStack> node = shardCommand.builder(
						Commands.literal(shardCommand.getName()));
					// その他の情報を付与して登録
					commands.register(node, shardCommand.getDescription(), shardCommand.getAliases());
				}
			});

			// 定期処理Timer
			DatabaseConfigState databaseConfigState = injector.getInstance(DatabaseConfigState.class);
			int intervalTick = databaseConfigState.getAutoSaveIntervalTick();
			Timer.on(serviceManager::saveAllAsync, intervalTick, intervalTick);

			injector.getInstance(LimboProtectionListener.class).limboTaskStart();
		} catch (Exception e) {
			onException(e);
		}
		this.phase = STARTUP_PHASE.ENABLE;
	}

	public void shutdownResources() {
		if (phase == STARTUP_PHASE.SHUTDOWN || phase == STARTUP_PHASE.BEFORE_LOAD) {
			return;
		}
		if (injector == null) {
			return;
		}
		try {
			// note: saveAllAsyncではキャッシュのデータは削除されない
			runIfNotNull(serviceManager, ServiceManager::saveAllAsync);
			runIfNotNull(databaseManager, DatabaseManager::shutdown);
		} catch (Exception e) {
			plugin.getLogger().warning("プラグインの終了処理中にエラーが発生しました: " + e.getMessage());
		}
		phase = STARTUP_PHASE.SHUTDOWN;
	}

	private <T> void runIfNotNull(@Nullable T data, Consumer<T> consumer) {
		if (data != null) {
			consumer.accept(data);
		}
	}

	/**
	 * エラーを表示してシャットダウンする
	 *
	 * @param e 発生したエラー
	 */
	private void onException(Exception e) throws RuntimeException {
		Logger logger = plugin.getLogger();
		logger.severe("=".repeat(60));
		logger.severe("ShardCoreの起動処理中にエラーが発生しました");
		logger.severe("-".repeat(60));

		logger.severe("エラー内容: " + e.getMessage());
		logger.severe("スタックトレース:");
		Arrays.stream(e.getStackTrace())
			.map(StackTraceElement::toString)
			.forEach(line -> logger.severe("    at " + line));

		if (e.getCause() != null) {
			logger.severe("原因: " + e.getCause().getMessage());
			Arrays.stream(e.getCause().getStackTrace())
				.map(StackTraceElement::toString)
				.forEach(line -> logger.severe("    at " + line));
		}

		logger.severe("=".repeat(60));

		// 終了処理の必要なものをここで行う
		shutdownResources();

		Bukkit.shutdown();

		throw new RuntimeException(e);
	}

	private enum STARTUP_PHASE {
		BEFORE_LOAD, LOAD, ENABLE, SHUTDOWN
	}
}
