package net.logiench.logienchlibv2.base.minecraft.menu;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.util.function.Consumer;

public class MenuEventException {

	public static <T> void accept(Player p, String name, Consumer<T> consumer, T value) {
		if (run(p, name, () -> consumer.accept(value))) {
			return;
		}
		if (value instanceof Cancellable cancellableValue) {
			cancellableValue.setCancelled(true);
		}
	}

	/// @return 処理中にエラーが発生した場合は false それ以外は true
	public static boolean run(Player p, String name, Runnable runnable) {
		try {
			runnable.run();
			return true;
		} catch (Exception e) {
			StringBuilder errorMessage = (new StringBuilder("** ")).append(name).append("のイベント処理中に例外が発生しました **");
			errorMessage.append("\n").append(e);
			errorMessage.append("\nStack trace: ");

			for (StackTraceElement element : e.getStackTrace()) {
				errorMessage.append("\n\tat ").append(element.toString());
			}

			p.sendMessage(ComponentUtil.textLine(
				"§4§_'" + name + "' an exception occurred while processing",
				"§4- '" + name + "' の処理中に例外が発生しました。"
			));
			LogienchLib.log.error(errorMessage.toString());
		}
		return false;
	}
}
