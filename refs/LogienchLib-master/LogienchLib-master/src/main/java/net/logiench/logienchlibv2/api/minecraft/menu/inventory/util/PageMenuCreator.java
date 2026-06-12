package net.logiench.logienchlibv2.api.minecraft.menu.inventory.util;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.InvMenuConsumers;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// これを使用する際は事前にアイテムをインベントリに追加しても変化しません
public class PageMenuCreator implements InvMenuConsumers.InventoryConsumer {
	private final List<Consumer<PageClickEvent>> clickListeners = new ArrayList<>();
	private final List<Consumer<PageChangeEvent>> pageListener = new ArrayList<>();
	private final int minPage;
	private int currentPage;
	private final int contentSize;
	private final int leftClickOffset;
	private final int rightClickOffset;
	private ItemStack leftArrow;
	private ItemStack rightArrow;
	private boolean showLeftArrow;
	private boolean showRightArrow;
	private ItemStack blankItem = null;

	public PageMenuCreator(int contentSize) {
		this(0, 0, contentSize, 9, 1);
	}

	public PageMenuCreator(int minPage, int currentPage, int contentSize, int leftClickOffset, int rightClickOffset) {
		this.minPage = minPage;
		this.currentPage = currentPage;
		this.contentSize = contentSize;
		this.leftClickOffset = leftClickOffset;
		this.rightClickOffset = rightClickOffset;
	}

	public PageMenuCreator setBlankItem(@Nullable ItemStack blankItem) {
		this.blankItem = blankItem;
		return this;
	}

	public PageMenuCreator addClickListener(Consumer<PageClickEvent> listener) {
		clickListeners.add(listener);
		return this;
	}

	public PageMenuCreator addChangePageListener(Consumer<PageChangeEvent> listener) {
		return addChangePageListener(true, listener);
	}

	/**
	 * ページ変更時もインベントリはクリア、再生成されないので <code>ev.getInventory().clear();</code> 等を行ってください
	 *
	 * @param useAllowCancel trueの場合 {@link PageChangeEvent} に付属してる {@link PageChangeEvent#isAllowChange()} を事前に行い、trueの場合のみ実行されます。falseの場合は無条件で実行されます
	 */
	public PageMenuCreator addChangePageListener(boolean useAllowCancel, Consumer<PageChangeEvent> listener) {
		if (!useAllowCancel) {
			pageListener.add(listener);
		} else {
			pageListener.add(ev -> {
				if (ev.isAllowChange()) {
					listener.accept(ev);
				} else {
					ev.setCancelled(true);
				}
			});
		}
		return this;
	}

	public InvMenuConsumers.InventoryOpenConsumer getOpenConsumer() {
		return (p, ev) -> {
			PageChangeEvent event = new PageChangeEvent(p.getOpenInventory(), InventoryType.SlotType.OUTSIDE, -999, ClickType.UNKNOWN, InventoryAction.UNKNOWN, PageChangeEvent.MoveDirection.NONE);
			for (Consumer<PageChangeEvent> listener : pageListener) {
				listener.accept(event);
			}
			reload(event);
		};
	}

	public InvMenuConsumers.InventoryClickConsumer getClickConsumer() {
		return ev -> {
			Inventory clicked = ev.getClickedInventory();
			if (clicked == null || clicked != ev.getInventory()) {
				return;
			}
			int size = ev.getInventory().getSize();
			int slot = ev.getRawSlot();
			PageChangeEvent.MoveDirection move = null;
			if (size - leftClickOffset == slot) {
				move = PageChangeEvent.MoveDirection.LEFT;
			} else if (size - rightClickOffset == slot) {
				move = PageChangeEvent.MoveDirection.RIGHT;
			}
			if (move != null) {
				ev.setCancelled(true);
				PageChangeEvent event = new PageChangeEvent(ev, move);
				for (Consumer<PageChangeEvent> listener : pageListener) {
					listener.accept(event);
				}
				reload(event);
				return;
			}
			int i = currentPage * contentSize + slot;
			if (slot >= contentSize) {
				i = -999;
			}
			PageClickEvent event = new PageClickEvent(i, ev);
			for (Consumer<PageClickEvent> listener : clickListeners) {
				listener.accept(event);
			}
			if (event.isChanged) {
				reload(event);
			}
		};
	}

	private void reload(PageChange ev) {
		int size = ev.getInventory().getSize();

		if (ev.isCancelled()) {
			return;
		}
		Inventory inv = ev.getInventory();

		currentPage = ev.getNewPage();
		leftArrow = ev.getLeftArrow();
		rightArrow = ev.getRightArrow();
		showLeftArrow = ev.showLeftArrow();
		showRightArrow = ev.showRightArrow();
		inv.setItem(size - leftClickOffset, showLeftArrow ? ev.getLeftArrow() : getBlank());
		inv.setItem(size - rightClickOffset, showRightArrow ? ev.getRightArrow() : getBlank());
	}

	private ItemStack getBlank() {
		return blankItem == null ? null : blankItem.clone();
	}

	public class PageClickEvent extends PageChange {
		private final int index;

		protected PageClickEvent(int index, @NotNull InventoryView view, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action) {
			super(view, type, slot, click, action, MoveDirection.NONE);
			this.index = index;
		}

		protected PageClickEvent(int index, InventoryClickEvent ev) {
			this(index, ev.getView(), ev.getSlotType(), ev.getRawSlot(), ev.getClick(), ev.getAction());
		}

		public boolean isInContent() {
			return isInSize(contentSize);
		}

		public boolean isInSize(int size) {
			return index >= 0 && index < size;
		}

		public int getIndex() {
			return index;
		}
	}

	public class PageChangeEvent extends PageChange {
		protected PageChangeEvent(@NotNull InventoryView view, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action, MoveDirection move) {
			super(view, type, slot, click, action, move);
			isChanged = true;
		}

		protected PageChangeEvent(InventoryClickEvent ev, MoveDirection move) {
			this(ev.getView(), ev.getSlotType(), ev.getRawSlot(), ev.getClick(), ev.getAction(), move);
		}

		public boolean isInitialize() {
			return getSlotType() == InventoryType.SlotType.OUTSIDE && getMoveDirection() == MoveDirection.NONE;
		}

		public MoveDirection getMoveDirection() {
			return move;
		}

		public boolean isAllowChange() {
			return showLeftArrow && move == MoveDirection.LEFT || showRightArrow && move == MoveDirection.RIGHT || move == MoveDirection.NONE;
		}
	}

	public class PageChange extends InventoryClickEvent {
		protected ItemStack newLeftArrow = leftArrow;
		protected ItemStack newRightArrow = rightArrow;
		protected final boolean showLeftArrow;
		protected final boolean showRightArrow;
		protected boolean newShowLeftArrow;
		protected boolean newShowRightArrow;
		protected int newPage;
		protected final MoveDirection move;
		protected boolean isChanged = false;

		protected PageChange(@NotNull InventoryView view, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action, MoveDirection move) {
			super(view, type, slot, click, action);
			this.move = move;
			this.newPage = currentPage + move.move;
			showLeftArrow = PageMenuCreator.this.showLeftArrow;
			newShowRightArrow = showRightArrow = PageMenuCreator.this.showRightArrow;
			newShowLeftArrow = newPage > minPage;
		}

		protected PageChange(InventoryClickEvent ev, MoveDirection move) {
			this(ev.getView(), ev.getSlotType(), ev.getRawSlot(), ev.getClick(), ev.getAction(), move);
		}

		public int getCurrentPage() {
			return currentPage;
		}

		public int getNewPage() {
			return newPage;
		}

		public void setNewPage(int newPage) {
			if (this.newPage == newPage) {
				return;
			}
			this.newPage = newPage;
			isChanged = true;
		}

		public ItemStack getLeftArrow() {
			return newLeftArrow;
		}

		public void setLeftArrow(ItemStack item) {
			newLeftArrow = item;
			isChanged = true;
		}

		public void setLeftArrow(SuperItemStack item) {
			setLeftArrow(item.build());
			isChanged = true;
		}

		public ItemStack getRightArrow() {
			return newRightArrow;
		}

		public void setRightArrow(ItemStack item) {
			newRightArrow = item;
			isChanged = true;
		}

		public void setRightArrow(SuperItemStack item) {
			setRightArrow(item.build());
			isChanged = true;
		}

		public boolean showLeftArrow() {
			return newShowLeftArrow;
		}

		public void showLeftArrow(boolean showLeftArrow) {
			if (this.newShowLeftArrow == showLeftArrow) {
				return;
			}
			this.newShowLeftArrow = showLeftArrow;
			isChanged = true;
		}

		public boolean showRightArrow() {
			return newShowRightArrow;
		}

		public void showRightArrow(boolean canMoveRight) {
			if (this.newShowRightArrow == canMoveRight) {
				return;
			}
			this.newShowRightArrow = canMoveRight;
			isChanged = true;
		}

		public int getContentSize() {
			return contentSize;
		}

		public int getContentStartIndex() {
			return newPage * contentSize;
		}

		public int getContentEndIndex() {
			return (newPage + 1) * contentSize - 1;
		}

		public int getContentEndSize() {
			return (newPage + 1) * contentSize;
		}

		public enum MoveDirection {
			LEFT(-1), RIGHT(1), NONE(0);

			public final int move;

			MoveDirection(int move) {
				this.move = move;
			}
		}
	}
}
