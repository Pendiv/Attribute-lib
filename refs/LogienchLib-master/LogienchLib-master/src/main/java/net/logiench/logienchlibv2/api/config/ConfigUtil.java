package net.logiench.logienchlibv2.api.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * プラグインの複雑なコンフィグ作成をサポートします
 * ファイル、フォルダの状況は外的要因により変化するため、時間をおいて再び使用する際は注意してください
 */
@SuppressWarnings("unused")
public class ConfigUtil {
	/// .yml, .yamlのファイルのみにマッチするパターン
	private static final Pattern YML_YAML = Pattern.compile("\\.ya?ml$");

	private final JavaPlugin plugin;
	private final File dataFolder;

	public ConfigUtil(@NotNull JavaPlugin plugin) {
		this(plugin, plugin.getDataFolder());
	}

	public ConfigUtil(@Nullable JavaPlugin plugin, @NotNull File dataFolder) {
		this.plugin = plugin;
		this.dataFolder = dataFolder;
	}

	@NotNull
	public File getCurrentFolder() {
		return dataFolder;
	}

	public boolean isExists() {
		return dataFolder.exists();
	}

	/**
	 * 指定した名前のファイルが存在していた場合、そのファイルを返します
	 *
	 * @param fileName ファイルの名前
	 * @return ファイルが存在する場合は File それ以外の場合は null
	 */
	@Nullable
	public File getFile(String fileName) {
		File file = new File(dataFolder, fileName);
		if (file.exists() && file.isFile()) {
			return file;
		}
		return null;
	}

	/**
	 * 指定した名前のファイルが存在する場合はそれを YamlConfiguration にし返します
	 *
	 * @param fileName ファイルの名前
	 * @return ファイルが存在する場合 YamlConfiguration、それ以外の場合は null
	 */
	@Nullable
	public YamlConfiguration getYmlConfig(String fileName) {
		File file = getFile(fileName);
		if (file == null) {
			return null;
		}
		return YamlConfiguration.loadConfiguration(file);
	}

	/**
	 * 現在のディレクトリにあるファイルをすべて取得します
	 *
	 * @return 取得したすべてのファイル
	 */
	@NotNull
	public List<File> getFiles() {
		File[] files = dataFolder.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(files).filter(File::isFile).toList();
	}

	/**
	 * 現在のディレクトリにあるyml, yamlファイルをすべて取得します
	 *
	 * @return 取得したすべてのファイル
	 */
	@NotNull
	public List<File> getYmlFiles() {
		File[] files = dataFolder.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(files).filter(f -> f.isFile() && YML_YAML.matcher(f.getName()).find()).collect(Collectors.toList());
	}

	/**
	 * 現在のディレクトリにあるyml, yamlファイルをすべて取得します
	 *
	 * @return 取得したすべてのyml, yamlファイル
	 */
	@NotNull
	public List<YamlConfiguration> getYmlConfigs() {
		return getYmlFiles().stream().map(YamlConfiguration::loadConfiguration).toList();
	}

	/**
	 * 現在のディレクトリにあるyml, yamlファイルをすべて取得します
	 *
	 * @return 取得したすべてのyml, yamlファイルとそのファイルの名前(yml, yamlの部分を除く)
	 */
	@NotNull
	public Map<String, YamlConfiguration> getYmlNameConfigs() {
		return getYmlFiles().stream().collect(Collectors.toMap(f -> f.getName().replaceFirst(YML_YAML.pattern(), ""), YamlConfiguration::loadConfiguration));
	}

	/**
	 * 現在のディレクトリにあるyml, yamlファイルをすべて取得します
	 *
	 * @return 取得したすべてのyml, yamlファイルとそのファイル本体
	 */
	@NotNull
	public Map<File, YamlConfiguration> getYmlFileConfigs() {
		return getYmlFiles().stream().collect(Collectors.toMap(f -> f, YamlConfiguration::loadConfiguration));
	}

	/**
	 * ディレクトリを変更します。もし移動先のフォルダが存在しない場合は作成します
	 *
	 * @param folderName 変更先のフォルダ名
	 * @return 移動先のConfigUtil
	 */
	@Contract(pure = true)
	@NotNull
	public ConfigUtil moveTo(@NotNull String folderName) {
		return moveTo(folderName, null);
	}

	/**
	 * ディレクトリを変更します。もし移動先のフォルダが存在しない場合は作成します
	 * また、ディレクトリが作成された場合、指定されたリソースを配置します
	 *
	 * @param folderName 変更先のフォルダ名
	 * @param onCreate   フォルダを新規作成した場合、実行される処理。作成した後に移動後のConfigUtilが与えられます
	 * @return 移動先のConfigUtil
	 */
	@Contract(pure = true)
	@NotNull
	public ConfigUtil moveTo(@NotNull String folderName, @Nullable Consumer<ConfigUtil> onCreate) {
		File folder = new File(dataFolder, folderName);
		// フォルダが存在せず
		ConfigUtil configUtil;
		if (!folder.exists()) {
			if (folder.mkdirs()) {
				// 一応フォルダを作成した後に作成
				configUtil = new ConfigUtil(plugin, folder);
				if (onCreate != null) {
					onCreate.accept(configUtil);
				}
				// 作成に失敗した
			} else {
				throw new IllegalArgumentException("Cannot create folder: " + folderName);
			}
		} else {
			configUtil = new ConfigUtil(plugin, folder);
		}
		return configUtil;
	}

	/**
	 * そのプラグインのresourcesの中身を現在地点に配置します
	 *
	 * @param resourcePath pluginのresourcesからの配置するファイルのパス
	 * @param replace      すでにあるファイルを置き換えるか
	 * @throws IllegalStateException plugin が指定されていない場合
	 */
	public void saveResource(@NotNull Path resourcePath, boolean replace) {
		if (plugin == null) {
			throw new IllegalStateException("Plugin has not been set!");
		}
		File file = saveFile(resourcePath.getFileName().toString(), replace);
		// すでにファイルがあり、replaceがfalse
		if (file == null) {
			return;
		}
		InputStream in = plugin.getResource(resourcePath.toString().replace('\\', '/'));
		if (in == null) {
			return;
		}

		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 現在地点に新規でファイルを作成します
	 *
	 * @param fileName ファイルの名前(拡張子を含む)
	 * @param replace  すでにあるファイルを置き換えるか
	 * @return 新規作成したファイル
	 */
	public File saveFile(@NotNull String fileName, boolean replace) {
		File file = new File(dataFolder, fileName);
		if (file.exists()) {
			if (!replace) {
				return null;
			}
			if (!file.delete()) {
				throw new IllegalArgumentException("Cannot delete file: " + fileName);
			}
		}
		try {
			if (!file.createNewFile()) {
				throw new IOException("return false");
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot create file: " + fileName, e);
		}
		return file;
	}

	/**
	 * @return null: 現在の位置がディレクトリではない、もしくはディレクトリが存在しない<br>
	 * true: ファイルが一つ以上見つかった<br>
	 * false: ファイルが一つも見つけられなかった
	 */
	@Nullable
	public Boolean findAnyFile() {
		File[] files = dataFolder.listFiles();
		if (files == null) {
			return null;
		}
		return Arrays.stream(files).anyMatch(File::isFile);
	}

	/**
	 * そのプラグインのresourcesの中身を現在地点に配置します
	 * ただし、そのディレクトリの中にファイルが一つもない場合に限ります
	 *
	 * @param resourcePath pluginのresourcesからの配置するファイルのパス
	 * @return 配置に成功したか
	 */
	public boolean saveDefaultConfig(@NotNull Path resourcePath) {
		Boolean findAnyFile = findAnyFile();
		if (findAnyFile == null) {
			throw new IllegalStateException("Data folder has not been set!");
		}
		if (findAnyFile) {
			return false;
		}
		saveResource(resourcePath, true);
		return true;
	}
}
