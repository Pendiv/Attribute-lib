package net.logiench.shardLib.database.dao;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.logiench.shardLib.ShardLib;
import net.logiench.shardLib.api.attribute.data.*;
import net.logiench.shardLib.api.register.attribute.AttributeValueProviderRegister;
import net.logiench.shardLib.database.DatabaseManager;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Singleton
public class SqlPlayerDataDAO implements PlayerDataDAO {
	private static final Type STATS_MAP_TYPE = new TypeToken<Map<String, Double>>() {}.getType();

	private final DatabaseManager dbManager;
	private final AttributeValueProviderRegister attributeValueProviderRegister;

	@Inject
	public SqlPlayerDataDAO(DatabaseManager dbManager, AttributeValueProviderRegister attributeValueProviderRegister) {
		this.dbManager = dbManager;
		this.attributeValueProviderRegister = attributeValueProviderRegister;
	}

	@Override
	public CompletableFuture<Map<UUID, Map<AttributeOperationModifier, Long>>> savePlayerData(PlayerData... data) {
		if (data.length == 0) {
			return CompletableFuture.completedFuture(null);
		}
		// 非同期でDB処理
		return CompletableFuture.supplyAsync(() -> {
			Map<UUID, Map<AttributeOperationModifier, Long>> instanceIds = new HashMap<>();
			Map<UUID, PlayerData> playerDataMap = new HashMap<>();
			List<UUIDModifierData> modifiersToInsert = new ArrayList<>();
			List<UUIDModifierData> modifiersToUpdate = new ArrayList<>();
			List<UUIDProviderData> providersToInsert = new ArrayList<>();
			List<UUIDProviderData> providersToUpdate = new ArrayList<>();

			// 接続確立
			try (Connection connection = dbManager.getConnection()) {
				connection.setAutoCommit(false);
				try (
					Statement stmt = connection.createStatement();
					// Stats
					PreparedStatement statsStmt = connection.prepareStatement(dbManager.getSQLDialect().savePlayerStats());
					// AttributeModifier
					PreparedStatement insertModifierStmt = connection.prepareStatement(dbManager.getSQLDialect().insertPlayerModifier(), Statement.RETURN_GENERATED_KEYS);
					PreparedStatement updateModifierStmt = connection.prepareStatement(dbManager.getSQLDialect().updatePlayerModifier());
					// AttributeValueProvider
					PreparedStatement insertProviderStmt = connection.prepareStatement(dbManager.getSQLDialect().insertPlayerProvider(), Statement.RETURN_GENERATED_KEYS);
					PreparedStatement updateProviderStmt = connection.prepareStatement(dbManager.getSQLDialect().updatePlayerProvider())
				) {
					// 各種プレイヤーのデータを処理
					for (PlayerData playerData : data) {
						UUID uuid = playerData.uuid();
						playerDataMap.put(uuid, playerData);

						statsStmt.setString(1, uuid.toString());
						statsStmt.setString(2, ShardLib.getGson().toJson(playerData.baseAttributes()));
						statsStmt.addBatch();

						for (AttributeModifier modifier : playerData.modifiers()) {
							Long instanceId = playerData.modifierInstanceIds().get(modifier);
							if (instanceId == null) {
								modifiersToInsert.add(new UUIDModifierData(uuid, modifier));
							} else {
								modifiersToUpdate.add(new UUIDModifierData(uuid, modifier));
							}
						}
						for (AttributeValueProvider provider : playerData.providers()) {
							Long instanceId = playerData.modifierInstanceIds().get(provider);
							if (instanceId == null) {
								providersToInsert.add(new UUIDProviderData(uuid, provider));
							} else {
								providersToUpdate.add(new UUIDProviderData(uuid, provider));
							}
						}
					}

					List<Long> modifierInstanceIds = dbManager.getBatchStrategy().executeAndMapKeys(
						insertModifierStmt, stmt, i -> i < modifiersToInsert.size(),
						(s, i) -> {
							UUIDModifierData modifierData = modifiersToInsert.get(i);
							PlayerData playerData = playerDataMap.get(modifierData.uuid());
							try {
								addModifierBatch(modifierData.uuid(), s, modifierData.modifier(), playerData.modifierRemainingTicks());
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					);
					List<Long> providerInstanceIds = dbManager.getBatchStrategy().executeAndMapKeys(
						insertProviderStmt, stmt, i -> i < providersToInsert.size(),
						(s, i) -> {
							UUIDProviderData providerData = providersToInsert.get(i);
							PlayerData playerData = playerDataMap.get(providerData.uuid());
							try {
								addProviderBatch(providerData.uuid(), s, providerData.provider(), playerData.modifierRemainingTicks());
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					);

					for (UUIDModifierData modifierData : modifiersToUpdate) {
						PlayerData playerData = playerDataMap.get(modifierData.uuid());
						addModifierBatch(modifierData.uuid(), updateModifierStmt, modifierData.modifier(), playerData.modifierRemainingTicks());
					}
					for (UUIDProviderData providerData : providersToUpdate) {
						PlayerData playerData = playerDataMap.get(providerData.uuid());
						addProviderBatch(providerData.uuid(), updateProviderStmt, providerData.provider(), playerData.modifierRemainingTicks());
					}

					statsStmt.executeBatch();
					updateModifierStmt.executeBatch();
					updateProviderStmt.executeBatch();

					for (int i = 0; i < modifierInstanceIds.size() && i < modifiersToInsert.size(); i++) {
						UUIDModifierData modifierData = modifiersToInsert.get(i);
						instanceIds.computeIfAbsent(modifierData.uuid(), a -> new HashMap<>()).put(modifierData.modifier(), modifierInstanceIds.get(i));
					}
					for (int i = 0; i < providerInstanceIds.size() && i < providersToInsert.size(); i++) {
						UUIDProviderData providerData = providersToInsert.get(i);
						instanceIds.computeIfAbsent(providerData.uuid(), a -> new HashMap<>()).put(providerData.provider, providerInstanceIds.get(i));
					}

					connection.commit();
				} catch (SQLException e) {
					connection.rollback();
					ShardLib.getInstance().getLogger().log(Level.SEVERE, "Failed to save player data. rollback is performed.", e);
				} finally {
					connection.setAutoCommit(true);
				}
			} catch (SQLException e) {
				ShardLib.getInstance().getLogger().log(Level.SEVERE, "Failed to connect to database.", e);
			}
			return instanceIds;
		});
	}

	@Override
	public CompletableFuture<Optional<PlayerData>> loadPlayerData(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = dbManager.getConnection()) {
				try (
					// Stats
					PreparedStatement statsStmt = connection.prepareStatement(dbManager.getSQLDialect().loadPlayerStats());
					// AttributeModifier
					PreparedStatement modifierStmt = connection.prepareStatement(dbManager.getSQLDialect().loadPlayerModifier());
					// AttributeValueProvider
					PreparedStatement providerStmt = connection.prepareStatement(dbManager.getSQLDialect().loadPlayerProvider())
				) {
					statsStmt.setString(1, uuid.toString());
					modifierStmt.setString(1, uuid.toString());
					providerStmt.setString(1, uuid.toString());
					try (ResultSet statsResult = statsStmt.executeQuery();
						 ResultSet modifierResult = modifierStmt.executeQuery();
						 ResultSet providerResult = providerStmt.executeQuery()
					) {
						Map<String, Double> baseAttributes;
						if (statsResult.next()) {
							baseAttributes = ShardLib.getGson().fromJson(statsResult.getString(1), STATS_MAP_TYPE);
						} else {
							baseAttributes = Map.of();
						}
						Map<AttributeOperationModifier, Long> modifierInstanceIds = new HashMap<>();
						List<AttributeModifier> modifiers = new ArrayList<>();
						List<AttributeValueProvider> providers = new ArrayList<>();

						while (modifierResult.next()) {
							AttributeModifier modifier = new AttributeModifier(
								modifierResult.getString(2),
								modifierResult.getString(3),
								ModifierOperation.valueOf(modifierResult.getString(4)),
								StackingRule.valueOf(modifierResult.getString(5)),
								modifierResult.getDouble(6),
								modifierResult.getLong(7)
							);
							modifiers.add(modifier);
							modifierInstanceIds.put(modifier, modifierResult.getLong(1));
						}
						while (providerResult.next()) {
							Optional<ProviderCalculation> optional = attributeValueProviderRegister.get(providerResult.getString(5));
							if (optional.isEmpty()) {
								ShardLib.getInstance().getLogger().warning("========================================");
								ShardLib.getInstance().getLogger().warning("!!! NOT FOUND AttributeValueProvider !!!");
								ShardLib.getInstance().getLogger().warning("  ProviderKey: " + providerResult.getString(5));
								ShardLib.getInstance().getLogger().warning("========================================");
								continue;
							}
							AttributeValueProvider provider = new AttributeValueProvider(
								providerResult.getString(2),
								providerResult.getString(3),
								ModifierOperation.valueOf(providerResult.getString(4)),
								optional.get(),
								providerResult.getLong(6)
							);
							providers.add(provider);
							modifierInstanceIds.put(provider, providerResult.getLong(1));
						}

						return Optional.of(new PlayerData(uuid, baseAttributes, modifiers, providers, null, modifierInstanceIds));
					}
				}
			} catch (SQLException e) {
				ShardLib.getInstance().getLogger().log(Level.SEVERE, "Failed to load or connect data.", e);
			}
			return Optional.empty();
		});
	}

	@Override
	public CompletableFuture<Void> deletePlayerModifier(List<Long> deleteModifierInstanceId, List<Long> deleteProviderInstanceId) {
		if (deleteProviderInstanceId.isEmpty() && deleteModifierInstanceId.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = dbManager.getConnection();
				 PreparedStatement modifierStmt = connection.prepareStatement(dbManager.getSQLDialect().removePlayerModifiers());
				 PreparedStatement providerStmt = connection.prepareStatement(dbManager.getSQLDialect().removePlayerProviders())
			) {
				for (Long id : deleteModifierInstanceId) {
					modifierStmt.setLong(1, id);
					modifierStmt.addBatch();
				}
				for (Long id : deleteProviderInstanceId) {
					providerStmt.setLong(1, id);
					providerStmt.addBatch();
				}

				modifierStmt.executeBatch();
				providerStmt.executeBatch();
			} catch (SQLException e) {
				ShardLib.getInstance().getLogger().log(Level.SEVERE, "Failed to delete or connect data.", e);
			}
		});
	}

	private void addModifierBatch(UUID uuid, PreparedStatement stmt, AttributeModifier modifier, Map<AttributeOperationModifier, Long> modifierRemainingTicks) throws SQLException {
		stmt.setString(1, uuid.toString());
		stmt.setString(2, modifier.getSourceId());
		stmt.setString(3, modifier.getTargetAttributeId());
		stmt.setString(4, modifier.getOperation().name());
		stmt.setString(5, modifier.getStackingRule().name());
		stmt.setDouble(6, modifier.getValue());
		stmt.setLong(7, modifierRemainingTicks.getOrDefault(modifier, -1L));
	}

	private void addProviderBatch(UUID uuid, PreparedStatement stmt, AttributeValueProvider provider, Map<AttributeOperationModifier, Long> modifierRemainingTicks) throws SQLException {
		stmt.setString(1, uuid.toString());
		stmt.setString(2, provider.getSourceId());
		stmt.setString(3, provider.getTargetAttributeId());
		stmt.setString(4, provider.getOperation().name());
		stmt.setString(5, provider.getProviderCalculation().key());
		stmt.setLong(6, modifierRemainingTicks.getOrDefault(provider, -1L));
	}

	public record UUIDModifierData(UUID uuid, AttributeModifier modifier) {
	}

	public record UUIDProviderData(UUID uuid, AttributeValueProvider provider) {
	}
}





