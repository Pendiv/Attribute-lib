package net.logiench.shardLib.util;

import java.util.Arrays;

public class ConfigLoadException extends RuntimeException {
	public ConfigLoadException(String message) {
		super(message);
	}

	public ConfigLoadException(String message, Object... values) {
		super(Arrays.stream(values).map(Object::toString).reduce(message, (a, b) -> a.replaceFirst("\\{}", b)));
	}
}
