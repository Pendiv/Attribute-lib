package net.logiench.shardCore.core.item.system.module.params;

import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;

/**
 * アイテムのデータをどのように引き継ぎ、更新するかを指定します。
 * 基本的には全てのデータを引き継ぎ、指定されたModuleだけ再計算、更新を行います。
 */
public class UpdateParameters {

	private static final UpdateParameters EMPTY = new UpdateParameters().setImmutable();

	@Unmodifiable
	@NotNull
	public static UpdateParameters empty() {
		return EMPTY;
	}

	public static UpdateParameters of() {
		return new UpdateParameters();
	}

	public static UpdateParameters of(Set<Class<? extends ItemModule<?>>> updateTargets) {
		return new UpdateParameters(Set.of(), updateTargets);
	}

	public static UpdateParameters of(Set<Class<? extends ItemModule<?>>> notKeepTargets, Set<Class<? extends ItemModule<?>>> updateTargets) {
		return new UpdateParameters(notKeepTargets, updateTargets);
	}

	/**
	 * 指定されたインスタンスの内容をコピーし、編集可能な状態で返します。
	 * このコピーは浅いコピーで、UpdateParametersの内容はコピー元と同様のインスタンスです。
	 *
	 * @param parameters コピー元パラメータ
	 */
	@NotNull
	public static UpdateParameters of(@Nullable UpdateParameters parameters) {
		UpdateParameters p = of();
		if (parameters != null) {
			p.params.putAll(parameters.params);
			p.notKeepTargets.addAll(parameters.notKeepTargets);
			p.updateTargets.addAll(parameters.updateTargets);
		}
		return p;
	}

	/**
	 * 指定されたインスタンスの内容をコピーし、編集不可な状態で返します。
	 * このコピーは浅いコピーで、UpdateParametersの内容はコピー元と同様のインスタンスです。
	 *
	 */
	@NotNull
	public static UpdateParameters ofImmutable(@Nullable UpdateParameters parameters) {
		if (parameters == null) {
			return EMPTY;
		}
		return of(parameters).setImmutable();
	}

	private final Map<UpdateParamKey<?>, Object> params = new HashMap<>();
	/// アイテムのデータを維持しないモジュール
	private final Set<Class<? extends ItemModule<?>>> notKeepTargets = new HashSet<>();
	/// データを維持しながらもそれを編集するモジュール。keepに同様のモジュールがある場合はupdateが優先される
	private final Set<Class<? extends ItemModule<?>>> updateTargets = new HashSet<>();

	private boolean immutable = false;

	private UpdateParameters() {
	}

	private UpdateParameters(Collection<Class<? extends ItemModule<?>>> notKeepTargets, Collection<Class<? extends ItemModule<?>>> updateTargets) {
		this.notKeepTargets.addAll(notKeepTargets);
		this.updateTargets.addAll(updateTargets);

		// 維持しない つまりは再生成するのにUpdateするのはありえない
		if (this.notKeepTargets.removeAll(this.updateTargets)) {
			// 整合性をremoveAllで修復して警告を出す
			ShardCore.getPLogger().warning("[UpdateParameters] 'updateTargets' に指定されているModuleは 'notKeepTargets' に指定できません。");
		}
	}

	public boolean shouldRead(ItemModule<?> itemModule) {
		return !shouldCalculate(itemModule);
	}

	public boolean shouldUpdate(ItemModule<?> itemModule) {
		return updateTargets.contains(itemModule.getClass());
	}

	public boolean shouldCalculate(ItemModule<?> itemModule) {
		return notKeepTargets.contains(itemModule.getClass());
	}

	public boolean isEmpty() {
		return params.isEmpty();
	}

	public <T> void put(@NotNull UpdateParamKey<T> key, T value) {
		if (immutable) {
			throw new IllegalStateException("このクラスは編集できません");
		}
		params.put(key, value);
	}

	@Nullable
	public <T> T get(@NotNull UpdateParamKey<T> key) {
		return key.cast(params.get(key));
	}

	@Contract(value = "_, !null -> !null")
	public <T> T get(@NotNull UpdateParamKey<T> key, T defaultValue) {
		Object val = params.get(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return key.cast(val);
		} catch (ClassCastException e) {
			return defaultValue;
		}
	}

	public <T> boolean ifPresent(@NotNull UpdateParamKey<T> key, @NotNull Consumer<T> consumer) {
		T value = get(key);
		if (value != null) {
			consumer.accept(value);
			return true;
		}
		return false;
	}

	public boolean has(UpdateParamKey<?> key) {
		return params.containsKey(key);
	}

	public UpdateParameters setImmutable() {
		this.immutable = true;
		return this;
	}
}
