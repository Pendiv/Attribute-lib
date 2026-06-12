package net.logiench.shardCore.core.menu.main.skillTree;

import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.util.ClickMenuCreator;
import net.logiench.logienchlibv2.api.minecraft.player.EditionCheck;
import net.logiench.shardCore.core.menu.util.SimpleMenu;
import net.logiench.shardCore.data.skill.tree.SkillTreeDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class SkillTreeMainMenu extends SimpleMenu {
	private static final int WIDTH = 9;
	private static final int HEIGHT = 5;
	private static final Component TITLE = Component.text("Skill Tree");
	private static final Map<MoveDirection, ItemStack> MOVE_ITEM_MAP = Map.of(

	);

	private final SkillTreeDefinition tree;
	/// 画面左上の座標
	private int x;
	private int y;

	private final EnumMap<MoveDirection, Boolean> canMoveMap = new EnumMap<>(MoveDirection.class);

	public SkillTreeMainMenu(Player player, SkillTreeDefinition tree) {
		super(player);
		this.tree = tree;
		this.x = (tree.getWidth() / 2) - (WIDTH / 2);
		this.y = (tree.getHeight() / 2) - (HEIGHT / 2);
	}

	@Override
	protected void initMenu() {
		refresh();
		menu.addAllListener(new ClickMenuCreator()
			.addEvent(47, ev -> move(MoveDirection.UP))
			.addEvent(48, ev -> move(MoveDirection.LEFT))
			.addEvent(50, ev -> move(MoveDirection.RIGHT))
			.addEvent(51, ev -> move(MoveDirection.DOWN))
		);
		// 1スロット操作可能にするための
		menu.addClickListener(ev -> {
			if (ev.getSlot() != 49) {
				return;
			}
			switch (ev.getClick()) {
				case LEFT -> move(MoveDirection.LEFT);
				case RIGHT -> move(MoveDirection.RIGHT);
				case SHIFT_LEFT -> move(MoveDirection.UP);
				case SHIFT_RIGHT -> move(MoveDirection.DOWN);
			}
		});
	}

	@Override
	public Component getTitle() {
		return TITLE;
	}

	@Override
	public int getSize() {
		return 54;
	}

	private boolean canMove(MoveDirection direction) {
		int x = direction.moveX(this.x);
		int y = direction.moveY(this.y);
		return switch (direction) {
			case LEFT -> x > -WIDTH;
			case RIGHT -> x < tree.getWidth();
			case UP -> y > -HEIGHT;
			case DOWN -> y < tree.getHeight();
		};
	}

	private void move(MoveDirection direction) {
		if (!canMoveMap.getOrDefault(direction, false)) {
			return;
		}
		this.x = direction.moveX(x);
		this.y = direction.moveY(y);
		refresh();
	}

	private void refresh() {
		for (MoveDirection direction : MoveDirection.values()) {
			canMoveMap.put(direction, canMove(direction));
		}

		ItemStack[] contents = menu.getInventory().getContents();

		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				contents[x + y * WIDTH] = tree.getCell(x + this.x, y + this.y).getItem();
			}
		}
		//contents
		for (MoveDirection direction : MoveDirection.values()) {
			contents[direction.getSlot()] = canMoveMap.get(direction) ? direction.getItem() : null;
		}

		if (EditionCheck.isJavaPlayer(player)) {
			contents[49] = SuperItemStack.init(Material.ENDER_PEARL).setName("§bJava版操作用スティック").setLore(
				"§e以下の操作で矢印ボタンの操作が可能です",
				"§f左クリック: 左",
				"§f右クリック: 右",
				"§fシフト+左クリック: 上",
				"§fシフト+右クリック: 下"
			).build();
		}

		menu.getInventory().setContents(contents);
	}
}
