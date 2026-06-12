package net.logiench.shardCore;

import com.google.inject.Injector;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.core.skill.system.SkillContext;
import net.logiench.shardCore.core.skill.system.SkillManager;
import net.logiench.shardCore.data.skill.def.TestSkillDef;
import net.logiench.shardLib.api.ShardLibProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// fixme | ShardCoreでスキルを使用するためのテストコード
public final class ShardCore extends JavaPlugin {
	private static ShardCore instance;
	private ShardCoreStartup startup;

	/**
	 * このプラグインのインスタンスを取得します
	 *
	 * @return JavaPluginを継承したプラグインのメインインスタンス
	 *
	 * @throws IllegalStateException プラグインがロードされる前に呼び出された場合
	 */
	public static ShardCore getInstance() {
		if (instance == null) {
			throw new IllegalStateException("ShardCoreプラグインはまだロードされていません。");
		}
		return instance;
	}

	/**
	 * プラグインのLoggerを直接取得します。
	 * これは<code>ShardCore.getInstance().getLogger()</code>の短縮系です
	 *
	 * @return logger
	 */
	public static Logger getPLogger() {
		return getInstance().getLogger();
	}

	@Override
	public void onLoad() {
		instance = this;
		this.startup = new ShardCoreStartup(this);

		startup.load();
	}

	@Override
	public void onEnable() {
		startup.enable();

		// test start

		Injector injector = startup.getInjector();

		Timer.on(() -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				ShardLibProvider.get().getPlayerAPI().getCharacterAPI(player).ifPresent(character -> {
					String collect = character.getAttributeAPI().getFinalAttributes().entrySet().stream().flatMap(e -> {
						if (e.getValue() == 0 && !e.getKey().equals("level")) {
							return Stream.empty();
						}
						return Stream.of("%s: %.2f".formatted(e.getKey().substring(0, Math.min(e.getKey().length(), 5)), e.getValue()));
					}).collect(Collectors.joining(",  "));
					player.sendActionBar(Component.text(collect));
				});
			}
		}, 5, 5);

		getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler
			private void onInteract(PlayerInteractEvent ev) {
				Player p = ev.getPlayer();
				if (ev.getAction() != Action.RIGHT_CLICK_AIR) {
					return;
				}
				if (ev.getItem() != null && ev.getItem().getType() == Material.DIAMOND_AXE) {
					SkillContext context = new SkillContext(p, null, 1);
					p.sendMessage("castSkill: " + injector.getInstance(SkillManager.class).castSkill(context, TestSkillDef.class));
				}
			}
		}, this);

		// test end
	}

	@Override
	public void onDisable() {
		if (startup == null) {
			return;
		}

		startup.shutdownResources();
	}
}
