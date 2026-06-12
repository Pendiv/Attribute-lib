package net.logiench.shardCore.core.item.system.module.context;

import net.logiench.shardCore.core.item.system.generator.Key;

public record ContextKey<T>(String key) implements Key<T> {
}
