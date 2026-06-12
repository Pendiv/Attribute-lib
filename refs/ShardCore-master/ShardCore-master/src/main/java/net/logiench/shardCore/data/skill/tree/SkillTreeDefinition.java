package net.logiench.shardCore.data.skill.tree;

import lombok.Getter;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SkillTreeDefinition {
	@Getter
	private final int width;
	@Getter
	private final int height;
	private final TreeCell[][] cells;

	public SkillTreeDefinition(String[] layout, Map<Character, SkillType> mapping) {
		assert layout.length > 0;
		this.height = layout.length;
		this.width = layout[0].length();
		this.cells = new TreeCell[height][width];

		for (int y = 0; y < height; y++) {
			String row = layout[y];
			for (int x = 0; x < width; x++) {
				char c = row.charAt(x);
				cells[y][x] = parseCell(c, mapping);
			}
		}
	}

	private TreeCell parseCell(char c, Map<Character, SkillType> mapping) {
		for (Cell cell : Cell.values()) {
			if (cell.getSymbol() == c) {
				return cell;
			}
		}

		// マッピングに登録されている文字ならスキルセルを作成
		SkillType skillType = mapping.get(c);
		if (skillType != null) {
			return new SkillCell(skillType);
		}

		return Cell.EMPTY;
	}

	public TreeCell getCell(int x, int y) {
		if (x < 0 || y < 0 || x >= width || y >= height) {
			return Cell.EMPTY;
		}
		return cells[y][x];
	}

	@Getter
	private enum Cell implements TreeCell {
		EMPTY(' ', false, false, false, false, null),
		UP_DOWN('│', true, true, false, false, SuperItemStack.init(Material.STICK)),
		LEFT_RIGHT('─', false, false, true, true, SuperItemStack.init(Material.STICK)),
		UP_LEFT('┘', true, false, true, false, SuperItemStack.init(Material.STICK)),
		UP_RIGHT('└', true, false, false, true, SuperItemStack.init(Material.STICK)),
		DOWN_LEFT('┐', false, true, true, false, SuperItemStack.init(Material.STICK)),
		DOWN_RIGHT('┌', false, true, false, true, SuperItemStack.init(Material.STICK)),
		;

		private final char symbol;
		private final boolean up;
		private final boolean down;
		private final boolean left;
		private final boolean right;
		private final ItemStack item;

		Cell(char c, boolean up, boolean down, boolean left, boolean right, SuperItemStack item) {
			this.symbol = c;
			this.up = up;
			this.down = down;
			this.left = left;
			this.right = right;
			this.item = item == null ? ItemStack.empty() : item.build();
		}

		@Override
		public ItemStack getItem() {
			return item.clone();
		}
	}
}
