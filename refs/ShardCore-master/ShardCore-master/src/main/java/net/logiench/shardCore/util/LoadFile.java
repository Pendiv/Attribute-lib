package net.logiench.shardCore.util;

import java.io.File;
import java.util.*;

public class LoadFile {
	/**
	 * 指定されたフォルダからその中にあるすべてのファイ路を指定した拡張子の条件で取得します。
	 *
	 * @param rootFile  探索開始地点
	 * @param extension 拡張子(endsWith)
	 * @return 見つかったファイル
	 */
	public static List<File> collectAll(File rootFile, String extension) {
		if (!rootFile.isDirectory()) {
			return List.of();
		}
		List<File> result = new ArrayList<>();
		Queue<File> queue = new LinkedList<>();
		File[] initialFiles = rootFile.listFiles();
		if (initialFiles != null) {
			queue.addAll(Arrays.asList(initialFiles));
		}

		while (!queue.isEmpty()) {
			File file = queue.poll();
			if (file.isDirectory()) {
				File[] subFiles = file.listFiles();
				if (subFiles != null) {
					queue.addAll(Arrays.asList(subFiles));
				}
			} else if (file.getName().endsWith(extension)) {
				result.add(file);
			}
		}
		return result;
	}
}
