package net.logiench.logienchlibv2.commands;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Filter {
	protected static List<String> filter(List<String> list, String input) {
		return filter(String.class, list, input, true);
	}

	protected static List<String> filter(List<String> list, String input, boolean toLower) {
		return filter(String.class, list, input, toLower);
	}

	protected static <T> List<String> filter(Class<T> clazz, List<T> list, String input) {
		return filter(clazz, list, input, true);
	}

	@SuppressWarnings("unchecked")
	protected static <T> List<String> filter(Class<T> clazz, List<T> list, String input, boolean toLower) {
		if (list.isEmpty()) {
			return List.of();
		}
		Stream<T> stream = list.stream();
		Stream<String> stringStream;
		if (clazz.equals(String.class)) {
			stringStream = (Stream<String>) stream;
		} else {
			stringStream = stream.map(Object::toString);
		}
		String inp = toLower ? input.toLowerCase() : input;
		return stringStream.filter(n -> n.contains(inp)).toList();
	}
}
