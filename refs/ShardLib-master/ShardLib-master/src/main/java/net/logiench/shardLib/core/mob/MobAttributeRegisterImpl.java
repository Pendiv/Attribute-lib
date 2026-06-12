package net.logiench.shardLib.core.mob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.api.register.mob.MobAttributeRegister;
import net.logiench.shardLib.core.attribute.AttributeDefinitionRegisterImpl;
import net.logiench.shardLib.core.attribute.AttributeManager;
import net.logiench.shardLib.di.annotations.MobCoreAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Singleton
public class MobAttributeRegisterImpl implements MobAttributeRegister {
	private final Set<String> fromApiDef = new HashSet<>();
	private final Map<String, AttributeDefinitionRegisterImpl> attributeRegistry = new HashMap<>();
	private final Map<String, AttributeManager> attributeManager = new HashMap<>();
	private final AttributeDefinitionRegisterImpl core;
	private boolean isLock = false;

	@Inject
	public MobAttributeRegisterImpl(@MobCoreAttribute AttributeDefinitionRegisterImpl core) {
		this.core = core;
	}

	@Override
	@NotNull
	public Map<String, AttributeDefinitionRegister> getAll() {
		if (isLock) {
			return Collections.unmodifiableMap(attributeRegistry);
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	@Override
	@NotNull
	public Optional<AttributeDefinitionRegister> get(String id) {
		if (isLock) {
			return Optional.ofNullable(attributeRegistry.get(id));
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	public Optional<AttributeManager> getManager(String id) {
		return Optional.ofNullable(attributeManager.get(id));
	}

	@Override
	@NotNull
	public AttributeDefinitionRegisterImpl registerFor(@NotNull String id) {
		if (isLock) {
			throw new IllegalStateException("Registration is now closed");
		}
		return registerFor(id, true);
	}

	private AttributeDefinitionRegisterImpl registerFor(@NotNull String id, boolean isApi) {
		if (id.length() > 127) {
			throw new IllegalArgumentException("key length exceed 127");
		}
		AttributeDefinitionRegisterImpl attribute = attributeRegistry.get(id);
		if (attribute != null) {
			return attribute;
		}
		attribute = new AttributeDefinitionRegisterImpl(core);
		attributeRegistry.put(id, attribute);
		if (isApi) {
			fromApiDef.add(id);
		}
		attributeManager.put(id, new AttributeManager(attribute));
		return attribute;
	}

	public MobAttributeRegisterImpl clearFromYml() {
		attributeRegistry.entrySet().removeIf(a -> !fromApiDef.contains(a.getKey()));
		attributeRegistry.values().forEach(AttributeDefinitionRegisterImpl::clearFromYml);
		return this;
	}

	public MobAttributeRegisterImpl loadFromYml(Map<String, Map<String, AttributeDefinition>> map) {
		for (Map.Entry<String, Map<String, AttributeDefinition>> attributeDef : map.entrySet()) {
			registerFor(attributeDef.getKey(), false).loadFromYml(attributeDef.getValue());
		}
		return this;
	}

	public void bake() {
		this.isLock = true;
		for (AttributeDefinitionRegisterImpl register : attributeRegistry.values()) {
			register.bake();
		}
	}
}
