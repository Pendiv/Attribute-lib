package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardCore.core.skill.base.SkillDefinition;
import net.logiench.shardCore.util.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SkillRegistry {
	private static final String SKILL_DEFINITION_PATH = "net.logiench.shardCore.data.skill.def";

	private final Map<Class<? extends SkillDefinition>, SkillDefinition> definitions = new HashMap<>();

	@Inject
	private SkillRegistry() {
		for (Class<? extends SkillDefinition> skillClass : ClassUtils.findSubClasses(SkillDefinition.class, SKILL_DEFINITION_PATH)) {
			registerSkillDefinition(skillClass);
		}
	}

	private <T extends SkillDefinition> void registerSkillDefinition(Class<T> skillDefinitonClass) {
		if (definitions.containsKey(skillDefinitonClass)) {
			return;
		}
		T instance = ClassUtils.initialize(skillDefinitonClass);
		if (instance == null) {
			return;
		}
		definitions.put(instance.getClass(), instance);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends SkillDefinition> T get(Class<T> skillDefinitionClass) {
		return (T) definitions.get(skillDefinitionClass);
	}

	@Unmodifiable
	@NotNull
	public Collection<SkillDefinition> getAll() {
		return Collections.unmodifiableCollection(definitions.values());
	}
}
