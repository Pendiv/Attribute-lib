package net.logiench.shardCore.config.system;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SpecificConfigKey<T> implements ConfigKey<T> {
	@NotNull
	private final String configPath;
	@NotNull
	private final Class<T> clazz;

	SpecificConfigKey(@NotNull String configPath, @NotNull Class<T> clazz) {
		this.configPath = configPath;
		this.clazz = clazz;
	}
}
