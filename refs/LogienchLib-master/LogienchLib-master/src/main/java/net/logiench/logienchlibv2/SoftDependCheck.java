package net.logiench.logienchlibv2;

public class SoftDependCheck {
	public static final boolean NBTAPI = isEnable("NBTAPI");

	public static boolean isEnable(String plugin) {
		return LogienchLib.getInstance().getServer().getPluginManager().isPluginEnabled(plugin);
	}
}
