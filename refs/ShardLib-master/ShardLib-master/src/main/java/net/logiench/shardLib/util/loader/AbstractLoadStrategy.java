package net.logiench.shardLib.util.loader;

import java.util.Optional;

public abstract class AbstractLoadStrategy implements LoadStrategy {
	private boolean success = false;
	private String message = null;

	/**
	 * 実行が成功したことを記録します。
	 *
	 * @param message 成功メッセージ
	 */
	public void setSuccess(String message) {
		this.success = true;
		this.message = message;
	}

	/**
	 * 実行が失敗したことを記録します。
	 *
	 * @param errorMessage エラーメッセージ
	 */
	public void setFailure(String errorMessage) {
		this.success = false;
		this.message = errorMessage;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public Optional<String> getMessage() {
		return Optional.ofNullable(this.message);
	}
}
