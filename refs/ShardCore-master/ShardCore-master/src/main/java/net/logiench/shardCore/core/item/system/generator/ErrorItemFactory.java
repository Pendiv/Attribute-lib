package net.logiench.shardCore.core.item.system.generator;

import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.ShardCore;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;

public class ErrorItemFactory {
	private static final ContainerKey<Byte, Boolean> ERROR_TAG_KEY = new ContainerKey<>(PersistentDataType.BOOLEAN, ShardCore.getInstance(), "is_error_item");

	public static SuperItemStack create(ItemGenerationResult result) {
		// 将来的にはエラー番号 000 とかにして、運営がbot使ってその番号のログ確認するようにしたい
		SuperItemStack item = SuperItemStack.init(Material.BARRIER)
			.setName("§cアイテムの生成に失敗しました")
			.setLore(
				"§b権限者に以下のエラーを報告してください",
				"§6状態: §d" + result.state(),
				"§6メッセージ: §d" + result.message()
			)
			.setItemData(ERROR_TAG_KEY, true)
			.setShowOnly();
		if (result.error() != null) {
			item.addLore("§6エラー: §d" + result.error().getMessage());
		}
		return item.addLore("§8このアイテムは表示専用です。移動させることはできません");
	}

	public static boolean isErrorItem(SuperItemStack item) {
		if (item == null) {
			return false;
		}
		return item.getItemDataOrDefault(ERROR_TAG_KEY, false);
	}
}
