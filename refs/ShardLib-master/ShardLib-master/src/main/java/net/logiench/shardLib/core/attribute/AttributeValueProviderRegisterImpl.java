package net.logiench.shardLib.core.attribute;

import com.google.inject.Singleton;
import net.logiench.shardLib.api.attribute.data.CalculationContext;
import net.logiench.shardLib.api.attribute.data.ProviderCalculation;
import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

@Singleton
public class AttributeValueProviderRegisterImpl implements AttributeValueProviderRegister {
	private final Map<String, ProviderCalculation> providers = new HashMap<>();
	private boolean isLock = false;

	@Override
	public @NotNull ProviderCalculation register(String key, ToDoubleFunction<CalculationContext> provider) {
		if (isLock) {
			throw new IllegalStateException("Registration is now closed");
		}
		if (key.length() > 127) {
			throw new IllegalArgumentException("key length exceed 127");
		}
		if (providers.containsKey(key)) {
			throw new IllegalStateException(key + " is already registered");
		}
		ProviderCalculation calculation = new ProviderCalculationImpl(key, provider);
		providers.put(key, calculation);
		return calculation;
	}

	@Override
	public @NotNull Map<String, ProviderCalculation> getAll() {
		if (isLock) {
			return Collections.unmodifiableMap(providers);
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	@Override
	public @NotNull Optional<ProviderCalculation> get(String key) {
		if (isLock) {
			return Optional.ofNullable(providers.get(key));
		}
		throw new IllegalStateException("Unable to obtain because registration is in progress");
	}

	public void bake() {
		isLock = true;
	}
}
