package net.logiench.shardLib.core.mob;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.mob.MobCharacterAPI;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.core.attribute.AttributeAPIImpl;
import net.logiench.shardLib.core.attribute.AttributeManager;
import net.logiench.shardLib.core.data.CustomDataContainerAPIImpl;
import net.logiench.shardLib.core.data.CustomDataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MobCharacterManager implements Listener {
	private final Map<UUID, MobCharacterAPI> characters = new HashMap<>();
	private final MobAttributeRegisterImpl mobAttribute;

	@Inject
	public MobCharacterManager(MobAttributeRegisterImpl mobAttribute) {
		this.mobAttribute = mobAttribute;
	}

	public boolean isShardEntity(Entity entity) {
		if (entity == null) {
			return false;
		}
		return entity.getPersistentDataContainer().has(MobDataKey.ATTRIBUTE_ID);
	}

	public Optional<MobCharacterAPI> getCharacter(UUID uuid) {
		return Optional.ofNullable(characters.get(uuid));
	}

	public Optional<Entity> spawnEntity(@NotNull Location location, EntityType type, String attributeProfileId) {
		Optional<AttributeDefinitionRegister> attribute = mobAttribute.get(attributeProfileId);
		if (attribute.isEmpty()) {
			return Optional.empty();
		}
		Entity entity = location.getWorld().spawnEntity(location, type);
		entity.setPersistent(true);
		entity.getPersistentDataContainer().set(MobDataKey.ATTRIBUTE_ID, PersistentDataType.STRING, attributeProfileId);

		registerCharacter(entity);

		return Optional.of(entity);
	}

	public void reloadProfiles() {
		for (UUID uuid : characters.keySet()) {
			characters.remove(uuid);
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null) {
				entity.remove();
				ShardLib.getInstance().getLogger().warning("Config change removed entity.\t\ttype: " + entity.getType());
			}
		}
	}

	public void saveAllCharacters() {
		characters.values().forEach(m -> {
			if (m.isDirty()) {
				m.save();
			}
		});
	}

	private void registerCharacter(Entity entity) {
		PersistentDataContainer data = entity.getPersistentDataContainer();
		String attributeId = data.get(MobDataKey.ATTRIBUTE_ID, PersistentDataType.STRING);
		if (attributeId == null) {
			return;
		}
		Optional<AttributeManager> optionalManager = mobAttribute.getManager(attributeId);
		Optional<AttributeDefinitionRegister> optionalRegister = mobAttribute.get(attributeId);
		// そのモブが定義されているものと同一か、もしそうでなければ不正なエンティティなので削除
		if (optionalManager.isEmpty() || optionalRegister.isEmpty()) {
			entity.remove();
			ShardLib.getInstance().getLogger().warning("Deleted because no definition for loaded entity was found.\t\ttype: " + entity.getType() + ", id: " + attributeId);
			return;
		}

		AttributeAPIImpl attributeAPI = new AttributeAPIImpl(optionalManager.get(), optionalRegister.get());
		attributeAPI.setBaseAttributes(MobCharacterAPIImpl.loadStats(data));
		for (AttributeModifier modifier : MobCharacterAPIImpl.loadModifiers(data)) {
			attributeAPI.addModifier(modifier);
		}

		UUID uuid = entity.getUniqueId();
		MobCharacterAPIImpl characterAPI = new MobCharacterAPIImpl(uuid,
			attributeAPI,
			CustomDataContainerAPIImpl.fromGson(
				data.get(
					CustomDataKey.CUSTOM_DATA,
					PersistentDataType.STRING
				)
			).orElseGet(CustomDataContainerAPIImpl::new)
		);
		characterAPI.getAttributeAPI().recalculateStats();
		characters.putIfAbsent(uuid, characterAPI);
	}

	private void unregisterCharacter(Entity entity) {
		MobCharacterAPI characterAPI = characters.remove(entity.getUniqueId());
		if (characterAPI != null && characterAPI.isDirty()) {
			characterAPI.save();
		}
	}

	// EntityAddToWorldEvent というものがある。全てのエンティティロードで通るため、こっちのほうが良い可能性

	// LOWESTにするとapiの初期化と被る
	@EventHandler(priority = EventPriority.LOW)
	private void onWorldLoad(ServerLoadEvent ev) {
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				registerCharacter(entity);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	private void onChunkLoad(ChunkLoadEvent ev) {
		for (Entity entity : ev.getChunk().getEntities()) {
			registerCharacter(entity);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onEntityRemove(EntityRemoveFromWorldEvent ev) {
		Entity entity = ev.getEntity();
		if (!isShardEntity(entity)) {
			return;
		}
		unregisterCharacter(ev.getEntity());
	}

	/// 名前の付いたモブが死ぬと出てくるコンソールの対策
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onEntityDeath(EntityDeathEvent ev) {
		Entity entity = ev.getEntity();
		if (!isShardEntity(entity)) {
			return;
		}
		entity.setCustomNameVisible(false);
		entity.customName(null);
	}
}
