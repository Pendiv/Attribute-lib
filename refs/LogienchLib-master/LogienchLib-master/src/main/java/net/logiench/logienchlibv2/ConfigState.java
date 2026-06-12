package net.logiench.logienchlibv2;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigState {

	public static void loadConfig() {
		LogienchLib.getInstance().saveDefaultConfig();
		FileConfiguration config = LogienchLib.getInstance().getConfig();

		getBedrockPlayerSkin = config.getBoolean("get-bedrock-player-skin", false);
	}

	private static boolean getBedrockPlayerSkin = false;

	public static boolean isGetBedrockPlayerSkin() {
		return getBedrockPlayerSkin;
	}
}
