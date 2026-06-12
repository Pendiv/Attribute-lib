package net.logiench.logienchlibv2.cacheKeys;

import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.api.cache.key.player.MapKey;

public interface PlayerDataKey {
	MapKey<SkinKey, String> BE_SKIN_DATA = new MapKey<>(LogienchLib.getInstance(), "skin", SkinKey.class, String.class);

	enum SkinKey {
		TEXTURE_ID,
		VALUE,
		SIGNATURE
	}
}
