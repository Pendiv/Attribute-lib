package net.logiench.shardCore.config.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Singleton
public class ConfigManager {
	/**
	 * 自動で配置するコンフィグのパスを設定します。
	 * ファイルが存在しない場合は新しく作成されます。
	 */
	private static final List<String> FILE_CONFIGS;
	private static final List<String> FOLDER_CONFIGS;

	static {
		FILE_CONFIGS = List.of(
			"config.yml",
			"player_config.yml"
		);
		FOLDER_CONFIGS = List.of(

		);
	}

	@Inject
	public ConfigManager() {
		deployFiles();
	}

	public void deployFiles() {
		for (String fileConfig : FILE_CONFIGS) {
			ConfigUtil.deployFile(fileConfig);
		}
		for (String folderConfig : FOLDER_CONFIGS) {
			ConfigUtil.deployFolder(folderConfig);
		}
	}

	/**
	 * ファイルまでのパスとそのコンフィグ内のパスを指定し、そのセクションのデータを取得します。
	 * <p>指定方法は以下の通り</p>
	 * <code>folder/file.yml|configPath.test</code>
	 * <p>コンフィグ内のパス指定がない場合は<code>|</code>以降を記述する必要はありません。</p>
	 *
	 * @param path コンフィグファイルのパスとコンフィグ内のパス
	 * @return 取得したコンフィグ、または存在しなければ {@link ConfigSection#empty()}
	 * @throws IllegalArgumentException 入力されたパスが異常だった場合
	 */
	@NotNull
	public ConfigSection getConfig(@NotNull String path) {
		String[] fileAndPath = path.split("\\|");
		if (fileAndPath.length > 2) {
			throw new IllegalArgumentException("パスの入力する形式が間違っています。JavaDocを確認してください。path: " + path);
		}
		String file = fileAndPath[0];
		String configPath = null;
		if (fileAndPath.length == 2) {
			configPath = fileAndPath[1];
		}
		// 指定されたファイルを取得
		File configFile = new File(ShardCore.getInstance().getDataFolder(), file);
		// 存在しない、もしくはファイルではない場合はエラー
		if (!configFile.exists() || !configFile.isFile()) {

			//			throw new IllegalStateException("指定されたファイルパスの対象はファイルでないか、存在しません");
			return ConfigSection.empty();
		}

		ConfigurationSection config = YamlConfiguration.loadConfiguration(configFile);
		// セクション取得が必要な場合は取得する
		if (configPath != null) {
			config = config.getConfigurationSection(configPath);
		}
		// ここだけは利用者側で変化させられるからEMPTYを返すようにする
		if (config == null) {
			//			("指定されたセクションパスは存在しません");
			return ConfigSection.empty();
		}
		return ConfigSection.of(config);
	}

	/**
	 * 指定されたパスの内部にあるファイルを再帰的に探索し取得します。
	 * ファイルの拡張子に関係なく取得するため、読込時に警告が発生する可能性があります。
	 *
	 * @param path フォルダのパス
	 * @return フォルダ内のすべてのファイル
	 */
	@NotNull
	public List<ConfigSection> getFolderConfig(@NotNull String path) {
		File rootFolder = new File(ShardCore.getInstance().getDataFolder(), path);
		if (!rootFolder.exists() || !rootFolder.isDirectory()) {
			return List.of();
		}
		List<ConfigSection> resultSections = new ArrayList<>();

		Queue<File> folderQueue = new LinkedList<>();
		folderQueue.add(rootFolder);
		while (!folderQueue.isEmpty()) {
			File folder = folderQueue.poll();
			File[] files = folder.listFiles();
			if (files == null) {
				continue;
			}
			for (File file : files) {
				if (file.isDirectory()) {
					folderQueue.add(file);
				} else if (file.isFile()) {
					resultSections.add(
						ConfigSection.of(YamlConfiguration.loadConfiguration(file)));
				}
			}
		}
		return resultSections;
	}
}
