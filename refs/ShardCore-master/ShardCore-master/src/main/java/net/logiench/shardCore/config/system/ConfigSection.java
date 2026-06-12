package net.logiench.shardCore.config.system;

import net.logiench.shardCore.ShardCore;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigSection {

	private static final ConfigSection EMPTY = new EmptyConfigSection();

	@Nullable
	@Contract("null -> null; !null -> !null")
	public static ConfigSection of(@Nullable ConfigurationSection section) {
		if (section == null) {
			return null;
		}
		return new ConfigSection(section);
	}

	@NotNull
	public static ConfigSection empty() {
		return EMPTY;
	}

	private final ConfigurationSection section;

	private ConfigSection(ConfigurationSection section) {
		this.section = section;
	}

	/**
	 * このクラスがemptyの場合、このメソッドはnullを返します。
	 *
	 * @return このセクションが内包しているConfigの実体クラス
	 */
	@Nullable
	public ConfigurationSection getConfig() {
		return section;
	}

	@Nullable
	public ConfigSection getSection(String path) {
		return of(section.getConfigurationSection(path));
	}

	@NotNull
	public <T> T getOrThrow(ConfigKey<T> key) {
		T value = get(key);
		if (value == null) {
			throw new IllegalStateException("ConfigKey " + key + " の値がnullです。");
		}
		return value;
	}

	@Nullable
	public <T> T get(ConfigKey<T> key) {
		return section.getObject(key.getConfigPath(), key.getClazz());
	}

	@NotNull
	public <T> T get(DefaultConfigKey<T> key) {
		Object v = section.get(key.getConfigPath());
		if (v instanceof String s) {
			T resolved = resolveIfString(key, s);
			if (resolved != null) {
				return resolved;
			}
		}
		return section.getObject(key.getConfigPath(), key.getClazz(), key.getDefaultValue());
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveIfString(DefaultConfigKey<T> key, String value) {
		if (key.getClazz() != String.class) {
			return null;
		}
		return (T) resolvePlaceholders(value);
	}

	private String resolvePlaceholders(String text) {
		if (text == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		int lastMatchEnd = 0;
		Pattern pattern = Pattern.compile("(?<!\\\\)\\$\\{([^}]+)}");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			sb.append(text, lastMatchEnd, matcher.start());
			String envVarName = matcher.group(1);
			String envValue = System.getenv(envVarName);
			if (envValue != null) {
				sb.append(envValue);
			} else {
				ShardCore.getPLogger().warning("環境変数 '" + envVarName + "' が見つかりません。");
				// ここでは部分的な置換ができないため、プレースホルダーをそのまま残すか、
				// あるいは呼び出し元でデフォルト値を使うように促す必要があるが、
				// String全体をパースしているので、見つからない場合はそのまま ${} を残す形にする
				sb.append(matcher.group(0));
			}
			lastMatchEnd = matcher.end();
		}
		sb.append(text.substring(lastMatchEnd));
		// エスケープされた \${} を ${} に戻す
		return sb.toString().replace("\\${", "${");
	}


	private static class EmptyConfigSection extends ConfigSection {
		private EmptyConfigSection() {
			super(null);
		}

		@Override
		public @Nullable ConfigSection getSection(String path) {
			return empty();
		}

		@Override
		public @Nullable <T> T get(ConfigKey<T> key) {
			return null;
		}

		@Override
		public @NotNull <T> T get(DefaultConfigKey<T> key) {
			return key.getDefaultValue();
		}
	}
}
