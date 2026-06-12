package net.logiench.shardLib.core.mob;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.AttributeAPI;
import net.logiench.shardLib.api.attribute.data.AttributeModifier;
import net.logiench.shardLib.api.data.CustomDataContainerAPI;
import net.logiench.shardLib.api.mob.MobCharacterAPI;
import net.logiench.shardLib.core.attribute.AttributeAPIImpl;
import net.logiench.shardLib.core.data.CustomDataContainerAPIImpl;
import net.logiench.shardLib.core.data.CustomDataKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobCharacterAPIImpl implements MobCharacterAPI {
	private static final Type STATS_MAP_TYPE = new TypeToken<Map<String, Double>>() {}.getType();

	private final UUID uuid;
	private final AttributeAPIImpl attributeAPI;
	private CustomDataContainerAPI customData;

	public MobCharacterAPIImpl(UUID uuid, AttributeAPIImpl attributeAPI, CustomDataContainerAPI customData) {
		this.uuid = uuid;
		this.attributeAPI = attributeAPI;
		this.customData = customData;
	}

	@Override
	@NotNull
	public AttributeAPI getAttributeAPI() {
		return attributeAPI;
	}

	@Override
	public void save() {
		Entity entity = Bukkit.getEntity(uuid);
		if (entity == null) {
			return;
		}
		Gson gson = ShardLib.getGson();
		// modifierのうち、保存する(isPersistentがtrue)物で、AttributeModifierにキャスト可能なもの。Entity用だからAttributeModifierにしかできないはずだけど一応
		List<String> modifiers = attributeAPI.getModifiers().stream()
			.filter(m -> m instanceof AttributeModifier s && s.isPersistentToMob())
			.map(gson::toJson)
			.toList();
		PersistentDataContainer container = entity.getPersistentDataContainer();
		container.set(MobDataKey.MODIFIERS, PersistentDataType.LIST.strings(), modifiers);
		container.set(MobDataKey.STATS, PersistentDataType.STRING, gson.toJson(attributeAPI.getBaseAttributes()));
		attributeAPI.resetDirty();
	}

	@Override
	public boolean isDirty() {
		return attributeAPI.isDirtyToMob(); // 保存するものが増えたら or で追加してく
	}

	static List<AttributeModifier> loadModifiers(PersistentDataContainer container) {
		Gson gson = ShardLib.getGson();
		return container.getOrDefault(MobDataKey.MODIFIERS, PersistentDataType.LIST.strings(), List.of()).stream().map(m -> gson.fromJson(m, AttributeModifier.class)).toList();
	}

	static Map<String, Double> loadStats(PersistentDataContainer container) {
		Map<String, Double> stats = ShardLib.getGson().fromJson(container.get(MobDataKey.STATS, PersistentDataType.STRING), STATS_MAP_TYPE);
		if (stats != null) {
			return stats;
		}
		return Map.of();
	}

	@Override
	@NotNull
	public CustomDataContainerAPI getPersistentData() {
		return customData.clone();
	}

	@Override
	public void setPersistentData(@NotNull CustomDataContainerAPI customData) {
		if (!(customData instanceof CustomDataContainerAPIImpl dataContainerImpl)) {
			throw new IllegalArgumentException("CustomDataContainerAPI is not cannot set PersistentData");
		}
		this.customData = customData;
		Entity entity = Bukkit.getEntity(uuid);
		if (entity == null) {
			return;
		}
		entity.getPersistentDataContainer().set(CustomDataKey.CUSTOM_DATA, PersistentDataType.STRING, dataContainerImpl.toGson());

	}
}
