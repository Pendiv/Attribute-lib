package net.logiench.shardLib.database.dialect;

import net.logiench.shardLib.database.config.DatabaseConfig;

/**
 * DBMSによって変化するSQL構文を取得します
 */
public abstract class SQLDialect {
	public static final int IN_CHUNK_SIZE = 500;

	protected final DatabaseConfig config;

	public SQLDialect(DatabaseConfig config) {
		this.config = config;
	}

	/**
	 * プレースホルダ 0<br>
	 * <code>table[owner_uuid, stats]</code>
	 */
	public abstract String createTablePlayerStats();

	/**
	 * プレースホルダ 2<br>
	 * 1. owner_uuid<br>
	 * 2. stats<br>
	 * <code>save[owner_uuid, stats]</code>
	 */
	public abstract String savePlayerStats();

	/**
	 * プレースホルダ 1<br>
	 * 1. owner_uuid<br>
	 * <code>load[stats]</code>
	 */
	public abstract String loadPlayerStats();

	// -------------------------------------------

	/**
	 * プレースホルダ 0<br>
	 * <code>table[instance_id, owner_uuid, source_id, target_id, operation, stacking_rule, value, remaining_ticks]</code>
	 */
	public abstract String createTablePlayerModifier();

	/**
	 * プレースホルダ 7<br>
	 * 1. owner_uuid<br>
	 * 2. source_id <br>
	 * 3. target_id<br>
	 * 4. operation<br>
	 * 5. stacking_rule<br>
	 * 6. value<br>
	 * 7. remaining_ticks<br>
	 * <code>save[owner_uuid, source_id, target_id, operation, stacking_rule, value, remaining_ticks]</code>
	 */
	public abstract String insertPlayerModifier();

	/**
	 * プレースホルダ 8<br>
	 * 1. owner_uuid<br>
	 * 2. source_id <br>
	 * 3. target_id<br>
	 * 4. operation<br>
	 * 5. stacking_rule<br>
	 * 6. value<br>
	 * 7. remaining_ticks<br>
	 * 8. instance_id<br>
	 * <code>save[instance_id, owner_uuid, source_id, target_id, operation, stacking_rule, value, remaining_ticks]</code>
	 */
	public abstract String updatePlayerModifier();

	/**
	 * プレースホルダ 1<br>
	 * 1. instance_id<br>
	 */
	public abstract String removePlayerModifiers();

	/**
	 * プレースホルダ 1<br>
	 * 1. owner_uuid<br>
	 * <code>load[instance_id, source_id, target_id, operation, stacking_rule, value, remaining_ticks]</code>
	 */
	public abstract String loadPlayerModifier();

	// -------------------------------------------

	/**
	 * プレースホルダ 0<br>
	 * <code>table[instance_id, owner_uuid, source_id, target_id, operation, provider_key, remaining_ticks]</code>
	 */
	public abstract String createTablePlayerProvider();

	/**
	 * プレースホルダ 6<br>
	 * 1. owner_uuid<br>
	 * 2. source_id<br>
	 * 3. target_id<br>
	 * 4. operation<br>
	 * 5. provider_key<br>
	 * 6. remaining_ticks<br>
	 * <code>save[owner_uuid, source_id, target_id, operation, provider_key, remaining_ticks]</code>
	 */
	public abstract String insertPlayerProvider();

	/**
	 * プレースホルダ 7<br>
	 * 1. owner_uuid<br>
	 * 2. source_id<br>
	 * 3. target_id<br>
	 * 4. operation<br>
	 * 5. provider_key<br>
	 * 6. remaining_ticks<br>
	 * 7. instance_id<br>
	 * <code>save[instance_id, owner_uuid, source_id, target_id, operation, provider_key, remaining_ticks]</code>
	 */
	public abstract String updatePlayerProvider();

	/**
	 * プレースホルダ 1<br>
	 * 1. instance_id<br>
	 */
	public abstract String removePlayerProviders();

	/**
	 * プレースホルダ 1<br>
	 * 1. owner_uuid<br>
	 * <code>load[instance_id, source_id, target_id, operation, provider_key, remaining_ticks]</code>
	 */
	public abstract String loadPlayerProvider();

}







