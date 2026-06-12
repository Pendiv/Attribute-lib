package net.logiench.shardLib.core.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.AttributeDefinition;
import net.logiench.shardLib.api.attribute.data.AttributeFormula;
import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

@Singleton
public class AttributeDefinitionRegisterImpl implements AttributeDefinitionRegister {
	private final AttributeDefinitionRegisterImpl root;
	private final Map<String, AttributeDefinition> fromApiDef = new HashMap<>();
	private final Map<String, AttributeDefinition> fromYmlDef = new HashMap<>();
	private Map<String, AttributeDefinition> finalDefinitions = Map.of();
	private boolean isUpdate = false;
	private boolean isLock = false;

	@Inject
	public AttributeDefinitionRegisterImpl(AttributeDefinitionRegisterImpl root) {
		this.root = root;
	}

	@Override
	public @NotNull Supplier<AttributeDefinition> register(@NotNull AttributeDefinition def) {
		if (isLock) {
			throw new IllegalStateException("Registration is now closed");
		}
		String id = def.id();
		if (id.length() > 127) {
			throw new IllegalArgumentException("key length exceed 127");
		}
		if (fromApiDef.containsKey(id)) {
			// このスコープで定義されたものをオーバーライドしないように対策
			throw new IllegalArgumentException("Identifier " + id + " is defined multiple times in the same scope");
		}
		fromApiDef.put(id, def);
		return () -> getDefinition(id);
	}

	private AttributeDefinition getDefinition(String id) {
		if (!isLock) {
			throw new IllegalStateException("Unable to obtain because registration is in progress");
		}
		return finalDefinitions.get(id);
	}

	@Override
	public @NotNull Map<String, AttributeDefinition> getAll() {
		if (isLock) {
			return finalDefinitions;
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	@Override
	public boolean contains(String id) {
		return finalDefinitions.containsKey(id);
	}

	@Override
	public @NotNull Optional<AttributeDefinition> get(String id) {
		if (isLock) {
			return Optional.ofNullable(finalDefinitions.get(id));
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	private void finalRegister(Map<String, AttributeDefinition> definitionMap, AttributeDefinition def, boolean overrideMessage) {
		/// 現在登録されている同一IDのステータス
		AttributeDefinition currentAttribute = definitionMap.get(def.id());
		if (currentAttribute == null) {
			// 新規登録
			definitionMap.put(def.id(), def);
		} else {
			// オーバーライド
			AttributeDefinition d;
			if (overrideMessage && (d = fromYmlDef.get(def.id())) != null && d != def) {
				// 同じスコープのymlの定義をapiで上書きした
				ShardLib.getInstance().getLogger().info("Attribute '" + def.id() + "' from YML has been overridden by an API registration.");
			}
			definitionMap.put(def.id(), new AttributeDefinition(
				def.id(),
				// NONE だったら継承せず未定義に、nullなら上位のを継承
				def.formula() == AttributeFormula.NONE ? List.of() : def.formula() == null ? currentAttribute.dependencies() : def.dependencies(),
				def.formula() == AttributeFormula.NONE ? null : def.formula() == null ? currentAttribute.formula() : def.formula(),
				def.hasDefaultValue() ? def.defaultValue() : currentAttribute.defaultValue()
			));
		}
	}

	public void bake() {
		isLock = true;
		isUpdate = true;
		Map<String, AttributeDefinition> definitionMap = new HashMap<>();
		if (root != null) {
			definitionMap.putAll(root.finalDefinitions);
		}
		for (AttributeDefinition def : fromYmlDef.values()) {
			finalRegister(definitionMap, def, false);
		}
		for (AttributeDefinition def : fromApiDef.values()) {
			finalRegister(definitionMap, def, true);
		}
		this.finalDefinitions = Collections.unmodifiableMap(definitionMap);
	}

	public boolean isUpdateAndReset() {
		if (isUpdate) {
			isUpdate = false;
			return true;
		}
		return false;
	}

	public AttributeDefinitionRegisterImpl clearFromYml() {
		fromYmlDef.clear();
		return this;
	}

	public AttributeDefinitionRegisterImpl loadFromYml(Map<String, AttributeDefinition> map) {
		fromYmlDef.putAll(map);
		return this;
	}

	public Map<String, AttributeDefinition> getFromApiDef() {
		return fromApiDef;
	}
}
