package net.logiench.shardCore.config.system;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface ConfigKey<T> {
	@NotNull
	String getConfigPath();

	@NotNull
	Class<T> getClazz();

	static <T> DefaultConfigKey<T> of(@NotNull String configPath, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		return new DefaultConfigKey<>(configPath, clazz, defaultValue);
	}

	static <T> DefaultConfigKey<T> of(@NotNull String configPath, @NotNull Class<T> clazz, @NotNull Supplier<T> defaultValue) {
		return new DefaultConfigKey<>(configPath, clazz, defaultValue);
	}

	static <T> SpecificConfigKey<T> of(@NotNull String configPath, @NotNull Class<T> clazz) {
		return new SpecificConfigKey<>(configPath, clazz);
	}
}
