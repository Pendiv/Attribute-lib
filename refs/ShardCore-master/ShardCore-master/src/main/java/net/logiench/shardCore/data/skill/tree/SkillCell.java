package net.logiench.shardCore.data.skill.tree;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.inventory.ItemStack;

public class SkillCell implements TreeCell {
	@Getter
	private final SkillType skillType;
	private final ItemStack item;

	public SkillCell(SkillType skillType) {
		this.skillType = skillType;
		this.item = SuperItemStack.init(skillType.icon()).name(skillType.displayName()).build();
	}

	@Override
	public ItemStack getItem() {
		return item.clone();
	}
}
