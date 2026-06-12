package net.logiench.shardCore.core.mob.system.loader;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.shardCore.core.mob.system.generator.MobGenerator;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MobLoader {
	@Getter
	@NotNull
	private final String id;
	@Getter
	private final Entity loadedEntity;
	@Getter
	private final DataContainer container;

	@Nullable
	@Contract("null -> null")
	public static MobLoader of(Entity entity) {
		if (entity == null) {
			return null;
		}
		DataContainer container = new DataContainer(entity);
		String id = container.get(MobGenerator.MOB_ID);
		if (id != null) {
			return new MobLoader(id, container, entity);
		}
		return null;
	}

	private MobLoader(@NotNull String id, DataContainer container, Entity loadedEntity) {
		this.id = id;
		this.loadedEntity = loadedEntity;
		this.container = container;
	}
}
