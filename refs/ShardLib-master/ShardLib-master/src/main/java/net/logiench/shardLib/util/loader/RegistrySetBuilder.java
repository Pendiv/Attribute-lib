package net.logiench.shardLib.util.loader;

import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import net.logiench.shardLib.core.attribute.AttributeDefinitionRegisterImpl;
import net.logiench.shardLib.core.mob.MobAttributeRegisterImpl;
import net.logiench.shardLib.core.yml.AttributeDefinitionLoader;
import net.logiench.shardLib.util.DefinitionLoadException;

import java.util.Map;

public class RegistrySetBuilder {
	private RegistrySet registry = null;
	private boolean loadYml = false;
	private Map<String, AttributeDefinition> newCoreAttributeDefs = null;
	private Map<String, AttributeDefinition> newMobCoreAttributeDefs = null;
	private Map<String, AttributeDefinition> newPlayerAttributeDefs = null;
	private Map<String, Map<String, AttributeDefinition>> newMobAttributeDefs = null;

	public RegistrySetBuilder setRegistry(RegistrySet registry) {
		this.registry = registry;
		return this;
	}

	private void loadYml() {
		if (loadYml) {
			return;
		}
		this.newCoreAttributeDefs = AttributeDefinitionLoader.coreAttribute();
		this.newMobCoreAttributeDefs = AttributeDefinitionLoader.mobCoreAttribute();
		this.newPlayerAttributeDefs = AttributeDefinitionLoader.playerAttribute();
		this.newMobAttributeDefs = AttributeDefinitionLoader.mobAttributes();
		loadYml = true;
	}

	public void build() throws DefinitionLoadException {
		build(registry, false);
	}

	public void dryRun() throws DefinitionLoadException {
		AttributeDefinitionRegisterImpl validationCoreAttributeRegister = new AttributeDefinitionRegisterImpl(null);
		AttributeDefinitionRegisterImpl validationPlayerAttributeRegister = new AttributeDefinitionRegisterImpl(validationCoreAttributeRegister);
		AttributeDefinitionRegisterImpl validationMobCoreAttributeRegister = new AttributeDefinitionRegisterImpl(validationCoreAttributeRegister);
		MobAttributeRegisterImpl validationMobAttributeRegister = new MobAttributeRegisterImpl(validationMobCoreAttributeRegister);
		// 既存のRegistrySetがあればそこからapiの登録情報を持ってくる
		if (registry != null) {
			registry.coreAttributeRegister().getFromApiDef().values().forEach(validationCoreAttributeRegister::register);
			registry.playerAttributeRegister().getFromApiDef().values().forEach(validationPlayerAttributeRegister::register);
			registry.mobCoreAttributeRegister().getFromApiDef().values().forEach(validationMobCoreAttributeRegister::register);
			registry.mobAttributeRegister().getAll().forEach((key, value) -> {
				if (value instanceof AttributeDefinitionRegisterImpl valueImpl) {
					AttributeDefinitionRegister validation = validationMobAttributeRegister.registerFor(key);
					valueImpl.getFromApiDef().values().forEach(validation::register);
				}
			});
		}
		build(new RegistrySet(
			validationCoreAttributeRegister,
			null, //この要素はリロードで変化しないapi限定Register
			validationMobCoreAttributeRegister,
			validationMobAttributeRegister,
			validationPlayerAttributeRegister
		), true);
	}

	private void build(RegistrySet registry, boolean isDry) throws DefinitionLoadException {
		loadYml();

		registry.coreAttributeRegister().clearFromYml().loadFromYml(newCoreAttributeDefs).bake();
		registry.playerAttributeRegister().clearFromYml().loadFromYml(newPlayerAttributeDefs).bake();
		registry.mobCoreAttributeRegister().clearFromYml().loadFromYml(newMobCoreAttributeDefs).bake();
		registry.mobAttributeRegister().clearFromYml().loadFromYml(newMobAttributeDefs).bake();

		// リロードで変化しない場所はnullで検証するため
		if (isDry) {
			return;
		}
		registry.attributeValueProviderRegister().bake();
	}
}
