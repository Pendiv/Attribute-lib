package net.logiench.shardLib.util;

public class DefinitionLoadException extends ConfigLoadException {
	public DefinitionLoadException(String message) {
		super(message);
	}

	public DefinitionLoadException(String message, Object... values) {
		super(message, values);
	}
}
