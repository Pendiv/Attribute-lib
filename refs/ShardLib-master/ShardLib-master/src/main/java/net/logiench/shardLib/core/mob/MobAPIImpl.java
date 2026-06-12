package net.logiench.shardLib.core.mob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.mob.MobAPI;
import net.logiench.shardLib.api.mob.MobCharacterAPI;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class MobAPIImpl implements MobAPI {

	private final MobCharacterManager mobCharacterManager;

	@Inject
	public MobAPIImpl(MobCharacterManager mobCharacterManager) {
		this.mobCharacterManager = mobCharacterManager;
	}

	@Override
	@NotNull
	public Optional<MobCharacterAPI> getCharacterAPI(UUID uuid) {
		return mobCharacterManager.getCharacter(uuid);
	}

	@Override
	@NotNull
	public Optional<MobCharacterAPI> getCharacterAPI(@NotNull Entity entity) {
		return getCharacterAPI(entity.getUniqueId());
	}

	@Override
	@NotNull
	public Optional<Entity> spawnEntity(@NotNull Location location, EntityType type, String attributeProfileId) {
		return mobCharacterManager.spawnEntity(location, type, attributeProfileId);
	}

	@Override
	public boolean isShardEntity(@Nullable Entity entity) {
		return mobCharacterManager.isShardEntity(entity);
	}
}
