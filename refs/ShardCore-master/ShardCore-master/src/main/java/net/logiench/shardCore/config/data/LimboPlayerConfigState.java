package net.logiench.shardCore.config.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.config.system.ConfigKey;
import net.logiench.shardCore.config.system.ConfigManager;
import net.logiench.shardCore.config.system.ConfigSection;
import net.logiench.shardCore.config.system.DefaultConfigKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@Singleton
public class LimboPlayerConfigState {

	private static final String CONFIG_PATH = "player_config.yml|limbo";

	private static final DefaultConfigKey<String> LIMBO_SAFE_WORLD = ConfigKey.of("limbo.safe_location.world", String.class, "world");
	private static final DefaultConfigKey<Double> LIMBO_SAFE_X = ConfigKey.of("limbo.safe_location.x", Double.class, 0d);
	private static final DefaultConfigKey<Double> LIMBO_SAFE_Y = ConfigKey.of("limbo.safe_location.y", Double.class, 400d);
	private static final DefaultConfigKey<Double> LIMBO_SAFE_Z = ConfigKey.of("limbo.safe_location.z", Double.class, 0d);

	private final String worldName;
	private final double x;
	private final double y;
	private final double z;

	@Inject
	private LimboPlayerConfigState(ConfigManager configManager) {
		ConfigSection rootConfig = configManager.getConfig(CONFIG_PATH);

		this.worldName = rootConfig.get(LIMBO_SAFE_WORLD);
		this.x = rootConfig.get(LIMBO_SAFE_X);
		this.y = rootConfig.get(LIMBO_SAFE_Y);
		this.z = rootConfig.get(LIMBO_SAFE_Z);
	}

	public Location getSafeLocation() {
		World world = null;
		if (worldName != null) {
			world = Bukkit.getWorld(worldName);
		}
		// 指定されたワールドが存在しない、または未ロードの場合はデフォルトワールドに
		if (world == null) {
			world = Bukkit.getWorlds().getFirst();
		}

		return new Location(world, x, y, z);
	}
}
