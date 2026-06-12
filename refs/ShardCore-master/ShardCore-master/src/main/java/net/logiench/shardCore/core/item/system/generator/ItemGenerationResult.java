package net.logiench.shardCore.core.item.system.generator;

import kotlin.Pair;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system.module.context.Context;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 *
 * @param state   アイテムの生成結果
 * @param item    生成されたアイテム
 * @param context 生成に使用されたデータや、作業の過程で作成されたデータ
 * @param message 生成のエラーメッセージ
 * @param error   エラークラス
 * @param <I>     対象アイテムの型
 */
public record ItemGenerationResult<I extends ShardItem>(@NotNull State state, @Nullable SuperItemStack item,
                                                        @Nullable Context<I> context, @Nullable String message,
                                                        @Nullable Exception error) {

	private static final Logger LOGGER = ShardCore.getPLogger();
	private static final String LOGGER_PREFIX = "[ItemGenerator] ";

	public ItemGenerationResult(@NotNull State state, @NotNull Exception error) {
		this(state, null, null, null, error);
	}

	public ItemGenerationResult(@NotNull State state, @NotNull String message) {
		this(state, null, null, message, null);
	}

	public ItemGenerationResult(@NotNull State state, @NotNull String message, @NotNull Exception error) {
		this(state, null, null, message, error);
	}

	public ItemGenerationResult(@NotNull State state, @NotNull SuperItemStack item, @NotNull Context<I> context) {
		this(state, item, context, null, null);
	}

	public ItemGenerationResult {
		if (state == State.SUCCESS && item == null) {
			throw new IllegalStateException("StateがSUCCESSの場合、アイテムをnullにすることはできません");
		}
	}

	/**
	 * 生成されたアイテムがnullの場合、エラーメッセージを付与したエラーアイテムを返します。
	 * エラーアイテムは表示専用で、移動やドロップなどをすると消滅するようになっています。
	 *
	 * @return 生成されたアイテム、またはエラーアイテム
	 */
	@NotNull
	public SuperItemStack safeItem() {
		if (item != null) {
			return item;
		}

		// 将来的にはエラー番号 000 とかにして、運営がbot使ってその番号のログ確認するようにしたい
		SuperItemStack item = SuperItemStack.init(Material.BARRIER)
			.setName("§cアイテムの生成に失敗しました")
			.setLore(
				"§b権限者に以下のエラーを報告してください",
				"§6状態: §d" + state,
				"§6メッセージ: §d" + message
			)
			.setShowOnly();
		if (error != null) {
			item.addLore("§6エラー: §d" + error.getMessage());
		}
		return item.addLore("§8このアイテムは表示専用です。移動させることはできません");
	}

	@Nullable
	public Pair<SuperItemStack, Context<I>> resultPair() {
		if (item == null || context == null) {
			return null;
		}
		return new Pair<>(item, context);
	}

	public boolean isSuccess() {
		return state == State.SUCCESS;
	}

	public ItemGenerationResult<I> ifSuccess(Consumer<@NotNull SuperItemStack> consumer) {
		if (isSuccess()) {
			consumer.accept(Objects.requireNonNull(this.item));
		}
		return this;
	}

	public ItemGenerationResult<I> printMessage() {
		if (message != null) {
			LOGGER.warning(LOGGER_PREFIX + message);
		}
		if (error != null) {
			LOGGER.warning(LOGGER_PREFIX + "-".repeat(15) + " " + error.getMessage() + " " + "-".repeat(15));
			for (StackTraceElement trace : error.getStackTrace()) {
				LOGGER.warning("|  " + trace.toString());
			}
			LOGGER.warning(LOGGER_PREFIX + "-".repeat(60));
		}
		/*
		if (isError()) {
			LOGGER.warning(LOGGER_PREFIX + message);
		}*/
		return this;
	}

	public enum State {
		SUCCESS,
		NOT_FOUND,
		INVALID_PARAMS,
		READER_IS_NULL,
		ERROR // processorがなぜエラー吐いたか不明なのでスコープを狭められない
	}
}
