package net.logiench.shardCore.core.item.system.data;

public record SerializedItemData(String itemId, String genParamsJson) {
	public boolean isEmpty() {
		return itemId == null || genParamsJson == null;
	}
}