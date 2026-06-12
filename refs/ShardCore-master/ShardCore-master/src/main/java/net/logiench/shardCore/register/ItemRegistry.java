package net.logiench.shardCore.register;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.base.module.ItemModule;
import net.logiench.shardCore.util.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アイテム種別（ShardItem）のクラスと、その共有インスタンスを管理するレジストリ
 */
@Singleton
public class ItemRegistry {
	private static final String ITEM_PATH = "net.logiench.shardCore.data.item.def";

	private final ModuleRegistry moduleRegistry;
	private boolean isImmutable = false;

	private final Map<String, ShardItem> idItems = new ConcurrentHashMap<>();
	private final Map<Class<? extends ShardItem>, ShardItem> items = new ConcurrentHashMap<>();

	@Inject
	private ItemRegistry(ModuleRegistry moduleRegistry) {
		this.moduleRegistry = moduleRegistry;
		for (Class<? extends ShardItem> clazz : ClassUtils.findSubClasses(ShardItem.class, ITEM_PATH)) {
			register(clazz);
		}
	}

	public void setImmutable() {
		this.isImmutable = true;
	}

	protected <T extends ShardItem> void register(@NotNull Class<T> clazz) {
		RegisterResult result = registerAndCheck(clazz);
		if (result.isSuccess()) {
			return;
		}
		ShardCore.getPLogger().warning("ItemRegistry | " + result.message());
	}

	public synchronized <T extends ShardItem> RegisterResult registerAndCheck(@NotNull Class<T> clazz) {
		T instance = ClassUtils.initialize(clazz);
		if (instance == null) {
			return new RegisterResult(RegisterResultState.INITIALIZATION_FAILED, clazz.getName());
		}
		return registerAndCheck(instance);
	}

	public synchronized <T extends ShardItem> RegisterResult registerAndCheck(@NotNull T instance) {
		if (isImmutable) {
			throw new IllegalStateException("ItemRegistry は現在編集不可です");
		}
		Class<? extends ShardItem> clazz = instance.getClass();
		if (items.containsKey(clazz)) {
			return new RegisterResult(RegisterResultState.ALREADY_REGISTERED, clazz.getName());
		}
		String id = instance.getId();
		if (idItems.containsKey(id)) {
			return new RegisterResult(RegisterResultState.DUPLICATE_ID, id, idItems.get(id).getClass().getName(), clazz.getName());
		}
		try {
			// ここで初めて初期化することでNamespacedKeyに変換できないIDが指定されても処理が止まらないようにする
			instance.getKey();

			// 最後にItemModuleのスコープが正常か(ArmorItemなのにWeaponItemを要求しているモジュールが指定されていないかなど)を確認する
			for (Class<? extends ItemModule<?>> moduleClass : instance.getModules()) {
				ItemModule<?> module = moduleRegistry.get(moduleClass);
				if (module == null) {
					// モジュール取得失敗
					return new RegisterResult(RegisterResultState.NOT_FOUND_MODULE, clazz.getName(), moduleClass.getName());
				}
				// モジュールが要求する型 (例: EquipmentItem.class)
				Class<?> targetType = module.getTargetType();

				// チェック: itemType(clazz) は targetType の一種か？
				// 例: WeaponItem は EquipmentItem の一種なので true
				if (!targetType.isAssignableFrom(clazz)) {
					return new RegisterResult(RegisterResultState.MODULE_SCOPE_ERROR, clazz.getName(), moduleClass.getName(), module.getTargetType().getName());
				}
			}
		} catch (IllegalArgumentException e) {
			return new RegisterResult(RegisterResultState.ILLEGAL_ID, id, clazz.getName(), e.getMessage());
		}
		items.put(clazz, instance);
		idItems.put(id, instance);
		return new RegisterResult(RegisterResultState.SUCCESS);
	}

	/**
	 * 入力されたクラスに対応する共有インスタンスを取得する。
	 */
	public @Nullable <T extends ShardItem> T get(@NotNull Class<T> clazz) {
		ShardItem item = items.get(clazz);
		if (item == null) {
			return null;
		}
		return clazz.cast(item);
	}

	/**
	 * 入力されたIDに対応する共有インスタンスを取得する。
	 */
	public @Nullable ShardItem get(String id) {
		return idItems.get(id);
	}

	/**
	 * 登録されている全てのアイテムインスタンスを返す（不変ビュー）。
	 */
	@Unmodifiable
	public Collection<ShardItem> getAllItems() {
		return Collections.unmodifiableCollection(items.values());
	}

	public enum RegisterResultState {
		SUCCESS(true, null),
		DUPLICATE_ID(false, "指定されたIDは重複しています。ID: %,d  クラス1: '%s', クラス2: '%s'"),
		ALREADY_REGISTERED(false, "クラスはすでに登録されています。クラス: '%s'"),
		ILLEGAL_ID(false, "指定されたIDは無効です。 ID: %,d  クラス: '%s', エラー: %s"),
		INITIALIZATION_FAILED(false, "アイテムデータの初期化に失敗しました。クラス: '%s"),
		NOT_FOUND_MODULE(false, "指定されたItemModuleをRegistryから見つけられませんでした。 クラス: '%s', モジュール: '%s'"),
		MODULE_SCOPE_ERROR(false, "不適切なデータを要求するItemModuleが登録されています。 クラス: '%s', モジュール: '%s' (要求: %s)")
		;

		@Getter
		private final boolean isSuccess;
		@Nullable
		private final String message;

		RegisterResultState(boolean isSuccess, @Nullable String message) {
			this.isSuccess = isSuccess;
			this.message = message;
		}

		private String getMessage(Object... args) {
			if (message == null) {
				return null;
			}
			return message.formatted(args);
		}
	}

	public record RegisterResult(RegisterResultState state, @Nullable String message) {
		private RegisterResult(RegisterResultState state) {
			this(state, state.getMessage());
		}

		private RegisterResult(RegisterResultState state, Object... args) {
			this(state, state.getMessage(args));
		}

		public boolean isSuccess() {
			return state.isSuccess();
		}
	}
}
