package net.logiench.shardCore.data.skill.tree;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

// スキルについての定義がないので現状の仮
public interface SkillType {
	Component displayName();

	// iconはすべて同じだったり？もしくはカテゴリで統一
	Material icon();
}
