package net.logiench.shardCore.core.item.base.def;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.shardCore.core.stats.base.AttributeEnum;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import org.bukkit.Material;

import java.util.List;

public enum ItemType {
	SWORD(ItemGroup.WEAPON, "Sword", Material.LEATHER_HORSE_ARMOR),
	CLAYMORE(ItemGroup.WEAPON, "Claymore", Material.LEATHER_HORSE_ARMOR),

	BOW(ItemGroup.WEAPON, "Bow", Material.LEATHER_HORSE_ARMOR),
	CROSSBOW(ItemGroup.WEAPON, "Claymore", Material.LEATHER_HORSE_ARMOR),

	STAFF(ItemGroup.WEAPON, "Staff", Material.LEATHER_HORSE_ARMOR),
	BOOK(ItemGroup.WEAPON, "Book", Material.LEATHER_HORSE_ARMOR),

	BATTLE_AXE(ItemGroup.WEAPON, "Battle Axe", Material.LEATHER_HORSE_ARMOR),
	HAMMER(ItemGroup.WEAPON, "Hammer", Material.LEATHER_HORSE_ARMOR),


	HELMET(ItemGroup.ARMOR, "Helmet", Material.LEATHER_HELMET),
	CHEST_PLATE(ItemGroup.ARMOR, "Chest Plate", Material.LEATHER_CHESTPLATE, /* test */ CoreStats.ATTACK_SPEED, CoreStats.FINAL_DARK_DAMAGE),
	LEGGINGS(ItemGroup.ARMOR, "Leggings", Material.LEATHER_LEGGINGS),
	BOOTS(ItemGroup.ARMOR, "Boots", Material.LEATHER_BOOTS),

	ACCESSORY(ItemGroup.ACCESSORY, "Accessory", Material.LEATHER_HORSE_ARMOR), // これももうちょっと分けてもいいかも、例えばスタックできるアクセサリー(?)

	GEM(ItemGroup.GEM, "Gem", Material.LEATHER_HORSE_ARMOR),
	;

	@Getter
	private final ItemGroup group;
	@Getter
	private final Component itemTypeName;
	@Getter
	private final Material material;
	@Getter
	private final List<AttributeEnum> uniqueStats;

	/**
	 *
	 * @param group        アイテムが所属するグループ
	 * @param itemTypeName アイテムタイプの表示名
	 * @param material     そのアイテムはどれで表現するか
	 * @param uniqueStats  そのアイテムタイプが持つユニークステータス(ex: 防御力、攻撃力など)
	 */
	ItemType(ItemGroup group, String itemTypeName, Material material, AttributeEnum... uniqueStats) {
		this.group = group;
		this.itemTypeName = ComponentUtil.text("§b" + itemTypeName);
		this.material = material;
		this.uniqueStats = List.of(uniqueStats);
	}
}
