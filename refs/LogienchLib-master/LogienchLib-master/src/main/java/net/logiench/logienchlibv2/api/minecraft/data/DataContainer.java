package net.logiench.logienchlibv2.api.minecraft.data;

import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@SuppressWarnings("unused")
public class DataContainer implements Cloneable {
	private static final PersistentDataAdapterContext ADAPTER_CONTEXT = new ItemStack(Material.STONE).getItemMeta().getPersistentDataContainer().getAdapterContext();

	private PersistentDataContainer container;

	public DataContainer() {
		this.container = createNewContainer();
	}

	public DataContainer(@NotNull PersistentDataContainer container) {
		this.container = container;
	}

	public DataContainer(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			throw new IllegalStateException("this item has not ItemMeta " + item.getType());
		}
		this.container = meta.getPersistentDataContainer();
	}

	public DataContainer(@NotNull SuperItemStack item) {
		this(item.build());
	}

	public DataContainer(@NotNull ItemMeta itemMeta) {
		this(itemMeta.getPersistentDataContainer());
	}

	public DataContainer(@NotNull Entity entity) {
		this(entity.getPersistentDataContainer());
	}


	@NotNull
	public PersistentDataContainer getPDC() {
		return container;
	}

	@NotNull
	public <T, Z> DataContainer set(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
		container.set(key, type, value);
		return this;
	}

	@NotNull
	public <T, Z> DataContainer set(@NotNull ContainerKey<T, Z> key, @NotNull Z value) {
		return set(key.key(), key.type(), value);
	}

	@NotNull
	public <T, Z> DataContainer setOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @Nullable Z value, @NotNull Z defaultValue) {
		set(key, type, value != null ? value : defaultValue);
		return this;
	}

	@NotNull
	public <T, Z> DataContainer setOrDefault(@NotNull ContainerKey<T, Z> key, @Nullable Z value, @NotNull Z defaultValue) {
		return setOrDefault(key.key(), key.type(), value, defaultValue);
	}


	/**
	 * 型を無視して特定のキーがあるかを確認します
	 */
	public boolean has(@NotNull NamespacedKey key) {
		return container.has(key);
	}

	/**
	 * 特定のキーとそれに一致した型があるかを確認します
	 */
	public boolean has(@NotNull NamespacedKey key, @NotNull PersistentDataType<?, ?> type) {
		return container.has(key, type);
	}

	/**
	 * 特定のキーがあるかを確認します
	 *
	 * @param typeCheck true なら{@link ContainerKey}に保存されている型を持つキー、falseならそのキーを持っているか
	 */
	public boolean has(@NotNull ContainerKey<?, ?> key, boolean typeCheck) {
		return typeCheck ? has(key.key(), key.type()) : has(key.key());
	}

	/**
	 * 型を無視して特定のキーがあるかを確認します
	 */
	public boolean has(@NotNull ContainerKey<?, ?> key) {
		return has(key, false);
	}


	@Nullable
	public <T, Z> Z get(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
		return container.get(key, type);
	}

	@Nullable
	public <T, Z> Z get(@NotNull ContainerKey<T, Z> key) {
		return get(key.key(), key.type());
	}

	@NotNull
	public <T, Z> Z getOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z defaultValue) {
		return container.getOrDefault(key, type, defaultValue);
	}

	@NotNull
	public <T, Z> Z getOrDefault(@NotNull ContainerKey<T, Z> key, @NotNull Z defaultValue) {
		return getOrDefault(key.key(), key.type(), defaultValue);
	}


	@NotNull
	public DataContainer remove(@NotNull NamespacedKey key) {
		container.remove(key);
		return this;
	}

	@NotNull
	public DataContainer remove(@NotNull ContainerKey<?, ?> key) {
		return remove(key.key());
	}


	/**
	 * 新しいタグコンテナを作成し、DataContainerに変換し返します。
	 * もともとあったデータは上書きされ、破棄されます。
	 *
	 * @param key 作成するコンテナのキー
	 * @return 新しく作成されたコンテナ
	 */
	@NotNull
	public DataContainer createContainer(@NotNull NamespacedKey key) {
		PersistentDataContainer newContainer = createNewContainer();
		container.set(key, PersistentDataType.TAG_CONTAINER, newContainer);
		return new DataContainer(newContainer);
	}

	/**
	 * 新しいタグコンテナを作成し、DataContainerに変換し返します。
	 * もともとあったデータは上書きされ、破棄されます。
	 *
	 * @param key 作成するコンテナのキー
	 * @return 新しく作成されたコンテナ
	 */
	@NotNull
	public DataContainer createContainer(@NotNull ContainerKey<PersistentDataContainer, PersistentDataContainer> key) {
		return createContainer(key.key());
	}


	/**
	 * タグコンテナを取得し、DataContainerに変換し返します
	 *
	 * @param key 取得するコンテナのキー
	 * @return 変換されたDataContainer 存在しない場合はnull
	 */
	@Nullable
	public DataContainer getContainer(@NotNull NamespacedKey key) {
		PersistentDataContainer container = get(key, PersistentDataType.TAG_CONTAINER);
		if (container == null) {
			return null;
		}
		return new DataContainer(container);
	}

	/**
	 * タグコンテナを取得し、DataContainerに変換し返します
	 *
	 * @param key 取得するコンテナのキー
	 * @return 変換されたDataContainer 存在しない場合はnull
	 */
	@Nullable
	public DataContainer getContainer(@NotNull ContainerKey<PersistentDataContainer, PersistentDataContainer> key) {
		return getContainer(key.key());
	}


	/**
	 * タグコンテナを取得し、DataContainerに変換し返します。
	 * データが存在しない場合は新しく作成されます。
	 *
	 * @param key 取得するコンテナのキー
	 * @return 変換されたDataContainer
	 */
	@NotNull
	public DataContainer getOrCreateContainer(@NotNull NamespacedKey key) {
		PersistentDataContainer container = get(key, PersistentDataType.TAG_CONTAINER);
		if (container == null) {
			container = createNewContainer();
		}
		return new DataContainer(container);
	}

	/**
	 * タグコンテナを取得し、DataContainerに変換し返します。
	 * データが存在しない場合は新しく作成されます。
	 *
	 * @param key 取得するコンテナのキー
	 * @return 変換されたDataContainer
	 */
	@NotNull
	public DataContainer getOrCreateContainer(@NotNull ContainerKey<PersistentDataContainer, PersistentDataContainer> key) {
		return getOrCreateContainer(key.key());
	}


	public boolean isEmpty() {
		return container.isEmpty();
	}

	@NotNull
	public Set<NamespacedKey> getKeys() {
		return container.getKeys();
	}


	/**
	 * 自信を<code>other</code>にコピーします
	 *
	 * @param other   コピーする対象
	 * @param replace 同じキーを置き換えるかどうか
	 * @return self
	 */
	@NotNull
	public DataContainer copyTo(PersistentDataContainer other, boolean replace) {
		container.copyTo(other, replace);
		return this;
	}

	/**
	 * 自信を<code>other</code>にコピーします
	 *
	 * @param other   コピーする対象
	 * @param replace 同じキーを置き換えるかどうか
	 * @return self
	 */
	@NotNull
	public DataContainer copyTo(DataContainer other, boolean replace) {
		return copyTo(other.getPDC(), replace);
	}


	@Override
	@NotNull
	public DataContainer clone() {
		try {
			DataContainer clone = (DataContainer) super.clone();
			PersistentDataContainer newContainer = createNewContainer();
			container.copyTo(newContainer, true);
			clone.container = newContainer;
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new Error(e);
		}
	}

	/**
	 * 新しい{@link PersistentDataContainer}を作成します
	 *
	 * @return 作成されたPersistentDataContainer
	 */
	public static PersistentDataContainer createNewContainer() {
		return ADAPTER_CONTEXT.newPersistentDataContainer();
	}
}
