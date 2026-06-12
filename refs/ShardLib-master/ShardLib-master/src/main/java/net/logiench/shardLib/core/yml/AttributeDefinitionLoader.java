package net.logiench.shardLib.core.yml;

import net.logiench.logienchlibv2.api.config.ConfigUtil;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.data.AttributeFormula;
import net.logiench.shardLib.util.DefinitionLoadException;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttributeDefinitionLoader {
	/// 数式から変数を抽出するための正規表現
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

	public static Map<String, AttributeDefinition> coreAttribute() {
		return loadFolder(
			new ConfigUtil(ShardLib.getInstance())
				.moveTo("attribute", c ->
					c.saveResource(Path.of("example/attribute", "attribute.yml"), false)
				)
		);
	}

	public static Map<String, Map<String, AttributeDefinition>> mobAttributes() {
		return loadAllFiles(
			new ConfigUtil(ShardLib.getInstance())
				.moveTo("mob")
				.moveTo("attribute", c ->
					c.saveResource(Path.of("example/mob/attribute", "test.yml"), false)
				)
		);
	}

	public static Map<String, AttributeDefinition> mobCoreAttribute() {
		ConfigUtil config = new ConfigUtil(ShardLib.getInstance())
			.moveTo("mob");
		config.saveResource(Path.of("example/mob", "core_attribute.yml"), false);
		return loadFile(config, "core_attribute.yml");
	}

	public static Map<String, AttributeDefinition> playerAttribute() {
		return loadFolder(
			new ConfigUtil(ShardLib.getInstance())
				.moveTo("player")
				.moveTo("attribute", c ->
					c.saveResource(Path.of("example/player/attribute", "player_attribute.yml"), false)
				)
		);
	}

	private static Map<String, AttributeDefinition> loadFile(ConfigUtil config, String fileName) {
		return load(config.getYmlConfig(fileName));
	}

	private static Map<String, Map<String, AttributeDefinition>> loadAllFiles(ConfigUtil config) {
		Map<String, Map<String, AttributeDefinition>> fileRegisters = new HashMap<>();
		for (Map.Entry<String, YamlConfiguration> e : config.getYmlNameConfigs().entrySet()) {
			fileRegisters.put(e.getKey(), load(e.getValue()));
		}
		return fileRegisters;
	}

	private static Map<String, AttributeDefinition> loadFolder(ConfigUtil config) {
		Map<String, AttributeDefinition> register = new HashMap<>();
		for (YamlConfiguration cfg : config.getYmlConfigs()) {
			for (Map.Entry<String, AttributeDefinition> e : load(cfg).entrySet()) {
				if (register.put(e.getKey(), e.getValue()) != null) {
					throw new DefinitionLoadException("Duplicate definition for key: " + e.getKey());
				}
			}
		}
		return register;
	}

	private static Map<String, AttributeDefinition> load(YamlConfiguration config) {
		Map<String, AttributeDefinition> register = new HashMap<>();
		if (config == null) {
			return register;
		}
		for (String k : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(k);
			if (section == null) {
				throw new DefinitionLoadException("Missing attribute definition for key: " + k);
			}
			String formula = section.getString("formula");

			List<String> dependencies = new ArrayList<>();
			AttributeFormula attributeFormula = null;
			if (formula != null) {
				if (!formula.isBlank()) {
					// exp4jで数式を準備
					ExpressionBuilder builder = new ExpressionBuilder(formula);

					Matcher matcher = VARIABLE_PATTERN.matcher(formula);
					while (matcher.find()) {
						dependencies.add(matcher.group());
						// 数式に含まれる変数を特定し登録
						builder.variable(matcher.group());
					}
					// 完成した数式を登録
					Expression expression = builder.build();
					attributeFormula = s -> {
						try {
							for (String var : expression.getVariableNames()) {
								expression.setVariable(var, s.getOrDefault(var, 0.0));
							}

							// 計算を実行し、結果をfinalValuesに保存
							return expression.evaluate();
						} catch (Exception e) {
							ShardLib.getInstance().getLogger().warning(e.getMessage());
							return 0;
						}
					};
				} else {
					attributeFormula = AttributeFormula.NONE;
				}
			}
			double defaultValue = section.getDouble("default", 0d);
			register.put(k, new AttributeDefinition(k, dependencies, attributeFormula, defaultValue));
		}
		return register;
	}
}
