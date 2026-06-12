package net.logiench.shardCore.core.itemRequirement.base;

import com.google.inject.Singleton;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardLib.api.player.PlayerCharacterAPI;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Singleton
public abstract class RequirementType<T> {
	static final ContainerKey<PersistentDataContainer, PersistentDataContainer> REQUIREMENT_CONTAINER =
		new ContainerKey<>(PersistentDataType.TAG_CONTAINER, ShardCore.getInstance(), "require");

	@Getter
	private final ContainerKey<?, T> containerKey;
	@Getter
	private final String keyName;

	public RequirementType(PersistentDataType<?, T> dataType, String keyName) {
		this.containerKey = new ContainerKey<>(dataType, ShardCore.getInstance(), keyName);
		this.keyName = keyName;
	}

	public Class<T> getDataType() {
		return containerKey.type().getComplexType();
	}

	/**
	 * この条件が達成されているか確認します。
	 *
	 * @param player       判定に使用するプレイヤー
	 * @param characterAPI 判定に使用するプレイヤーのキャラクターデータ
	 * @param value        要求する条件。判定に使用する要素ではないので注意
	 * @return この条件が達成されているか。されている場合はtrue, それ以外の場合はfalse
	 */
	public abstract boolean check(@NotNull Player player, @NotNull PlayerCharacterAPI characterAPI, T value);

	public abstract Component getLoreFormat(T value);

	public static DataContainer getRequireContainer(SuperItemStack item) {
		return item.getDataContainer().getOrCreateContainer(REQUIREMENT_CONTAINER);
	}

	public static void editRequirementContainer(SuperItemStack item, Consumer<DataContainer> consumer) {
		item.editItemMeta(meta -> {
			DataContainer container = new DataContainer(meta.getPersistentDataContainer());
			DataContainer requireContainer = container.getOrCreateContainer(REQUIREMENT_CONTAINER);
			consumer.accept(requireContainer);
			container.set(REQUIREMENT_CONTAINER, requireContainer.getPDC());
		});
	}
}
