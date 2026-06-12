package net.logiench.shardLib.di;

import com.google.inject.AbstractModule;
import net.logiench.shardLib.database.dao.PlayerDataDAO;
import net.logiench.shardLib.database.dao.SqlPlayerDataDAO;

public class DaoModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(PlayerDataDAO.class)
			.to(SqlPlayerDataDAO.class);
	}
}
