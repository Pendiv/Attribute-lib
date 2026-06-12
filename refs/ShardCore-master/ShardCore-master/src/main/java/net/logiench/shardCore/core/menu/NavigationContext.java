package net.logiench.shardCore.core.menu;

import net.logiench.shardCore.core.menu.util.AbstractMenu;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class NavigationContext {
	// スタック（LIFO: 後入れ先出し）を使用
	private final Deque<Supplier<AbstractMenu>> backStack = new ArrayDeque<>();

	/**
	 * 新しいページに遷移した時の処理
	 * (進むスタックはクリアされる)
	 */
	public void pushNext(Supplier<AbstractMenu> currentMenu) {
		backStack.push(currentMenu);

		// メモリ対策: 履歴が溜まりすぎたら古いのを消す（例: 20件）
		if (backStack.size() > 20) {
			backStack.removeLast();
		}
	}

	public AbstractMenu popBack(AbstractMenu currentMenu) {
		if (backStack.isEmpty()) {
			return null;
		}

		return backStack.pop().get();
	}

	// 完全にメニューを閉じた時などに呼ぶ
	public void clear() {
		backStack.clear();
	}

	public boolean hasBack() {
		return !backStack.isEmpty();
	}
}