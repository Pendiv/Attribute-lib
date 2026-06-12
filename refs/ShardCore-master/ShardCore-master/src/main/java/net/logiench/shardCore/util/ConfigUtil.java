package net.logiench.shardCore.util;

import net.logiench.shardCore.ShardCore;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigUtil {

	/**
	 * 単一のファイルを展開し、その File オブジェクトを返します。
	 * 既に存在する場合は何もしません。
	 *
	 * @param resourcePath JAR内のパス (例: "config.yml" や "lang/ja_JP.yml")
	 * @return 展開された、または既に存在する File
	 */
	public static File deployFile(String resourcePath) {
		ShardCore plugin = ShardCore.getInstance();

		File outFile = new File(plugin.getDataFolder(), resourcePath);
		if (!outFile.exists()) {
			// 親フォルダが存在しない場合は作成
			File outFolder = outFile.getParentFile();
			if (!outFolder.exists() && !outFolder.mkdirs()) {
				throw new RuntimeException("フォルダが作成できません: " + outFile.getPath());
			}
			// false を指定することで、ユーザーが編集したファイルを上書きしない
			plugin.saveResource(resourcePath, false);
		}
		return outFile;
	}

	/**
	 * 指定されたフォルダが存在しない場合、JAR内の同名フォルダからデフォルトファイルをすべて展開します。
	 * 既にフォルダが存在する場合は展開を行わず、フォルダ内のすべてのファイルを返します。
	 *
	 * @param folderPath JAR内のフォルダパス (例: "items" や "mobs")
	 * @return フォルダ内に存在するすべてのファイルのリスト
	 */
	public static List<File> deployFolderAndGet(String folderPath) {
		File outDir = deployFolder(folderPath);
		if (!(outDir.exists() && outDir.isDirectory())) {
			return List.of();
		}
		List<File> loadedFiles = new ArrayList<>();
		collectFilesRecursive(outDir, loadedFiles);
		return loadedFiles;
	}

	private static void collectFilesRecursive(File directory, List<File> fileList) {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				// フォルダなら中身をさらに探索
				collectFilesRecursive(file, fileList);
			} else {
				// ファイルならリストに追加
				fileList.add(file);
			}
		}
	}

	/**
	 * 指定されたフォルダが存在しない場合、JAR内の同名フォルダからデフォルトファイルをすべて展開します。
	 * 既にフォルダが存在する場合は展開を行わず、指定されたフォルダを返します。
	 *
	 * @param folderPath JAR内のフォルダパス (例: "items" や "mobs")
	 * @return 指定されたフォルダのFileインスタンス
	 */
	public static File deployFolder(String folderPath) {
		ShardCore plugin = ShardCore.getInstance();
		File outDir = new File(plugin.getDataFolder(), folderPath);

		// フォルダが存在する場合は何もしない
		if (outDir.exists()) {
			return outDir;
		}
		// フォルダが存在しない場合のみ、JARの中身をスキャンして展開する
		if (!outDir.mkdirs()) {
			plugin.getLogger().info("deployFolderでフォルダの作成ができません: " + outDir.getPath());
			return outDir;
		}

		try {
			// 自分自身のJARファイルを取得 (空白を含むパスに対応するため URI 経由)
			File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

			try (JarFile jar = new JarFile(jarFile)) {
				Enumeration<JarEntry> entries = jar.entries();
				// 指定されたパスから始まっていて、後ろに1文字以上ある(指定されたディレクトリ自信を除外した)ファイルだけマッチさせる
				String prefix = folderPath.endsWith("/") ? folderPath : folderPath + "/";

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();

					// 指定したフォルダ配下のファイルであり、ディレクトリではない場合
					if (entryName.startsWith(prefix) && !entry.isDirectory()) {
						plugin.saveResource(entryName, false);
					}
				}
			}
		} catch (URISyntaxException | IOException e) {
			plugin.getLogger().severe("デフォルトデータの展開中にエラーが発生しました: " + folderPath);
			e.printStackTrace();
		}

		return outDir;
	}
}