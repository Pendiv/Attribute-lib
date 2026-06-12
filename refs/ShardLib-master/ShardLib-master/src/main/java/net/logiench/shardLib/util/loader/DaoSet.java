package net.logiench.shardLib.util.loader;

import com.google.inject.Inject;
import net.logiench.shardLib.database.dao.PlayerDataDAO;

public record DaoSet(
	PlayerDataDAO PlayerDataDAO
) {
	@Inject
	public DaoSet {
	}
}
