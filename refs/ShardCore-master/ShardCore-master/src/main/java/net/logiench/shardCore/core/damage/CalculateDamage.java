package net.logiench.shardCore.core.damage;

import lombok.Getter;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardLib.api.attribute.AttributeAPI;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 攻撃側エンティティ、防御側エンティティを渡し、ダメージの計算処理を行います。
 */
public class CalculateDamage {
	private static final Map<AttributeEnum, AttributeEnum> DAMAGE_DEFENSE_STATS = new HashMap<>();

	static {
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_NATURAL_DAMAGE, null);
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_FIRE_DAMAGE, CoreStats.FINAL_FIRE_DEFENSE);
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_WATER_DAMAGE, CoreStats.FINAL_WATER_DEFENSE);
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_EARTH_DAMAGE, CoreStats.FINAL_EARTH_DEFENSE);
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_HOLY_DAMAGE, CoreStats.FINAL_HOLY_DEFENSE);
		DAMAGE_DEFENSE_STATS.put(CoreStats.FINAL_DARK_DAMAGE, CoreStats.FINAL_DARK_DEFENSE);
	}

	@Getter
	private final double damage;

	public CalculateDamage(@NotNull Entity sourceEntity, @NotNull AttributeAPI sourceAttribute, @Nullable AttributeAPI targetAttribute, @NotNull DamageType type) {
		double damage = 0;

		// 全種類のダメージを (攻撃力 - 耐性) で計算
		for (Map.Entry<AttributeEnum, AttributeEnum> damageDefenseEntry : DAMAGE_DEFENSE_STATS.entrySet()) {
			AttributeEnum defenseKey = damageDefenseEntry.getValue();
			double defenseValue = 0;
			if (targetAttribute != null && defenseKey != null) {
				defenseValue = targetAttribute.getFinalAttributeOptional(defenseKey.getId()).orElse(0d);
			}

			AttributeEnum damageKey = damageDefenseEntry.getKey();
			if (damageKey == null) {
				continue;
			}
			damage += Math.max(0, sourceAttribute.getFinalAttributeOptional(damageKey.getId()).orElse(0d) - defenseValue);
		}

		this.damage = damage + ThreadLocalRandom.current().nextDouble(0, 100);
		//		System.out.println(damage);
	}
}
