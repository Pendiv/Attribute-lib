package net.logiench.shardCore.config.system;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DefaultConfigKey<T> extends SpecificConfigKey<T> {
	@NotNull
	private final Supplier<T> defaultValueSupplier;

	DefaultConfigKey(@NotNull String configPath, @NotNull Class<T> clazz, @NotNull T defaultValue) {
		this(configPath, clazz, () -> defaultValue);
	}

	DefaultConfigKey(@NotNull String configPath, @NotNull Class<T> clazz, @NotNull Supplier<T> defaultValueSupplier) {
		super(configPath, clazz);
		this.defaultValueSupplier = defaultValueSupplier;
	}

	@NotNull
	public T getDefaultValue() {
		T defaultValue = defaultValueSupplier.get();
		if (defaultValue == null) {
			throw new NullPointerException("Config %s はデフォルト値がnullになっています".formatted(getConfigPath()));
		}
		return defaultValueSupplier.get();
	}
}
