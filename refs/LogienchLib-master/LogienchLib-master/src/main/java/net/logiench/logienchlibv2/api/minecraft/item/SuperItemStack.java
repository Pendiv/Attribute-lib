package net.logiench.logienchlibv2.api.minecraft.item;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.logiench.logienchlibv2.ConfigState;
import net.logiench.logienchlibv2.LogienchLib;
import net.logiench.logienchlibv2.SoftDependCheck;
import net.logiench.logienchlibv2.api.cache.master.player.MapData;
import net.logiench.logienchlibv2.api.minecraft.data.ContainerKey;
import net.logiench.logienchlibv2.api.minecraft.data.DataContainer;
import net.logiench.logienchlibv2.api.minecraft.player.EditionCheck;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.text.LoreList;
import net.logiench.logienchlibv2.cacheKeys.PlayerDataKey;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * SuperItemStackクラスは、MinecraftのItemStackに対して、ItemMetaなどを介してでないとアクセスできないような、様々な操作を提供するクラスです。<br>
 * アイテムの表示名、説明文、エンチャントなど、多彩な機能があり、全てのアイテムの生成に活用することができます
 */
@SuppressWarnings({"unused"})
public class SuperItemStack implements Cloneable {

	/**
	 * LogienchLib内で使用されている、アイテムを表示限定のモードか判定するためのデータです
	 */
	public static final ContainerKey<Byte, Boolean> SHOW_ONLY = new ContainerKey<>(PersistentDataType.BOOLEAN, LogienchLib.getInstance(), "show");
	/**
	 * LogienchLib内で使用されている、アイテムの権限設定を取得、判定するためのデータです
	 */
	public static final ContainerKey<Byte, Byte> ITEM_PERMISSION = new ContainerKey<>(PersistentDataType.BYTE, LogienchLib.getInstance(), "op");

	/**
	 * LogienchLib内で使用されている、アイテムの移動制限をかけるためのデータです
	 */
	public static final ContainerKey<Byte, Boolean> MOVE_LOCK = new ContainerKey<>(PersistentDataType.BOOLEAN, LogienchLib.getInstance(), "move");

	/**
	 * ItemMetaが存在することが確認されたItemStackのみコンストラクタでの代入が完了する
	 */
	@NotNull
	private ItemStack item;

	/**
	 * nullを含む全てのマテリアルを拡張します
	 *
	 * @param material 拡張するマテリアル
	 * @return null もしくは SuperItemStack
	 */
	@Contract("null -> null")
	@Nullable
	public static SuperItemStack safeInit(@Nullable Material material) {
		if (material == null) {
			return null;
		}
		return safeInit(new ItemStack(material));
	}

	/**
	 * nullを含む全てのアイテムを拡張します
	 *
	 * @param item 拡張するアイテム
	 * @return null もしくは SuperItemStack
	 */
	@Contract("null -> null")
	@Nullable
	public static SuperItemStack safeInit(@Nullable ItemStack item) {
		if (item == null || item.getItemMeta() == null) {
			return null;
		}
		return init(item);
	}

	/**
	 * マテリアルをSuperItemStackに拡張します
	 *
	 * @param material 拡張するマテリアル
	 * @return SuperItemStack
	 *
	 * @throws IllegalArgumentException 与えられたMaterialにItemMetaがなかった場合
	 */
	@NotNull
	public static SuperItemStack init(@NotNull Material material) throws IllegalArgumentException {
		return new SuperItemStack(material);
	}

	/**
	 * アイテムをSuperItemStackに拡張します
	 *
	 * @param item 拡張するアイテム
	 * @return SuperItemStack
	 *
	 * @throws IllegalArgumentException 与えられたItemStackにItemMetaがなかった場合
	 */
	@NotNull
	public static SuperItemStack init(@NotNull ItemStack item) throws IllegalArgumentException {
		return new SuperItemStack(item);
	}

	/**
	 * BE, JE専用のヘッド作成を自動で切り替えて作成します
	 *
	 * @param player 作成するプレイヤー
	 * @return SuperItemStack
	 */
	@Contract("null -> null; !null -> !null")
	public static SuperItemStack head(@Nullable Player player) {
		if (player == null) {
			return null;
		}
		return head(player.getUniqueId());
	}

	/**
	 * BE, JE専用のヘッド作成を自動で切り替えて作成します<br>
	 * オフラインの統合版のプレイヤーは {@link #jeHead(OfflinePlayer)} となります<br>
	 * NBTAPIがインストールされていない場合も {@link #jeHead(OfflinePlayer)} となります
	 *
	 * @param uuid 作成するプレイヤーのUUID
	 * @return SuperItemStack
	 */
	@Contract("null -> null")
	public static SuperItemStack head(@Nullable UUID uuid) {
		if (uuid == null) {
			return null;
		}
		SuperItemStack head = null;
		if (EditionCheck.isBedrockPlayer(uuid) && SoftDependCheck.NBTAPI && ConfigState.isGetBedrockPlayerSkin()) {
			head = beHead(uuid);
		}
		if (head != null) {
			return head;
		}
		return jeHead(uuid);
	}

	/**
	 * JE専用のヘッド作成を作成します<br>
	 * BEのプレイヤーのスキンは反映されません
	 *
	 * @param player 作成するプレイヤー
	 * @return SuperItemStack
	 */
	@Contract("null -> null; !null -> !null")
	public static SuperItemStack jeHead(@Nullable OfflinePlayer player) {
		if (player == null) {
			return null;
		}
		SuperItemStack item = init(Material.PLAYER_HEAD);
		item.editItemMeta(SkullMeta.class, meta -> meta.setOwningPlayer(player));
		return item;
	}

	/**
	 * JE専用のヘッド作成を作成します<br>
	 * BEのプレイヤーのスキンは反映されません
	 *
	 * @param uuid 作成するプレイヤーのUUID
	 * @return SuperItemStack
	 */
	@Contract("null -> null; !null -> !null")
	public static SuperItemStack jeHead(@Nullable UUID uuid) {
		if (uuid == null) {
			return null;
		}
		return jeHead(Bukkit.getOfflinePlayer(uuid));
	}

	/**
	 * BE専用のヘッド作成を作成します<br>
	 * JEのプレイヤーの場合はnullになります<br>
	 * {@link #beHead(UUID)} がnullになった場合は {@link #jeHead(OfflinePlayer)} になります
	 *
	 * @param player 作成するプレイヤー
	 * @return SuperItemStack
	 */
	@Contract("null -> null")
	@Nullable
	public static SuperItemStack beHead(@Nullable Player player) {
		if (player == null) {
			return null;
		}
		SuperItemStack beHead = null;
		if (SoftDependCheck.NBTAPI) {
			beHead = beHead(player.getUniqueId());
		}
		if (beHead != null) {
			return beHead;
		}
		return jeHead(player);
	}

	/**
	 * BE専用のヘッド作成を作成します<br>
	 * JEのプレイヤーの場合、または configの<code>get-bedrock-player-skin</code>がfalseの場合はnullになります
	 *
	 * @param uuid 作成するプレイヤーのUUID
	 * @return SuperItemStack
	 *
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 */
	@Contract("null -> null")
	@Nullable
	public static SuperItemStack beHead(@Nullable UUID uuid) {
		if (uuid == null) {
			return null;
		}
		FloodgatePlayer connection = FloodgateApi.getInstance().getPlayer(uuid);
		if (connection == null) {
			return null;
		}
		return customHeadFromBase64(connection.getJavaUsername(), MapData.get(uuid, PlayerDataKey.BE_SKIN_DATA, PlayerDataKey.SkinKey.VALUE));
	}

	/**
	 * nameとスキンのurlを指定したjsonのbase64からカスタムヘッドを作成します
	 *
	 * @param name   スキンの名前
	 * @param base64 スキンのbase64
	 *               <pre><code>{"textures": {"SKIN": {"url": "http://textures.minecraft.net/texture/5d323c79086084514a31a88530092dc0a73cdbfd41efaa14bd7f0de055747c57"}}}</code></pre>
	 * @return SuperItemStack
	 *
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 */
	@Contract("_, null -> null; _, !null -> !null")
	public static SuperItemStack customHeadFromBase64(@Nullable String name, @Nullable String base64) {
		if (base64 == null) {
			return null;
		}
		return new SuperItemStack(name, base64);
	}

	/**
	 * @return SuperItemStack
	 *
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 * @see #customHeadFromBase64(String, String)
	 */
	@Contract("null -> null; !null -> !null")
	public static SuperItemStack customHeadFromBase64(@Nullable String base64) {
		return customHeadFromBase64(null, base64);
	}

	/**
	 * nameとスキンのurlからカスタムヘッドを作成します
	 *
	 * @param name スキンの名前
	 * @param url  スキンのURL
	 *             例: http://textures.minecraft.net/texture/5d323c79086084514a31a88530092dc0a73cdbfd41efaa14bd7f0de055747c57
	 * @return SuperItemStack
	 *
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 */
	@Contract("_, null -> null; _, !null -> !null")
	public static SuperItemStack customHeadFromUrl(@Nullable String name, @Nullable String url) {
		if (url == null) {
			return null;
		}
		return customHeadFromBase64(name, getBase64TextureValue(url));
	}

	/**
	 * @return SuperItemStack
	 *
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 * @see #customHeadFromUrl(String, String)
	 */
	@Contract("null -> null; !null -> !null")
	public static SuperItemStack customHeadFromUrl(@Nullable String url) {
		return customHeadFromUrl(null, url);
	}

	protected static String getBase64TextureValue(@NotNull String url) {
		return Base64.getEncoder().encodeToString(
			("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes()
		);
	}

	/**
	 * 指定されたMaterialを使って新しいSuperItemStackを作成します。
	 *
	 * @param material アイテムの種類
	 * @throws IllegalArgumentException 与えられたMaterialにItemMetaがなかった場合
	 */
	protected SuperItemStack(@NotNull Material material) throws IllegalArgumentException {
		this(new ItemStack(material));
	}

	/**
	 * ItemStackをSuperItemStackに拡張します
	 *
	 * @param item 拡張するアイテム
	 * @throws IllegalArgumentException 与えられたItemStackにItemMetaがなかった場合
	 */
	protected SuperItemStack(@NotNull ItemStack item) throws IllegalArgumentException {
		if (item.getItemMeta() == null) {
			throw new IllegalArgumentException("this item has not itemMeta : " + item.getType());
		}
		this.item = item;
	}

	/**
	 * base64からプレイヤーヘッドを取得する
	 * <pre><code>
	 * 		//sample
	 *  	SuperItemStack.init(Base64.getEncoder().encodeToString(
	 * 				("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes()
	 * 		))
	 * </code></pre>
	 *
	 * @param base64 プレイヤーのスキンテクスチャurl
	 * @throws IllegalStateException NBTAPIがインストールされていない場合
	 */
	protected SuperItemStack(@Nullable String name, @NotNull String base64) throws IllegalStateException {
		this(Material.PLAYER_HEAD);
		if (SoftDependCheck.NBTAPI) {
			NBT.modifyComponents(item, nbt -> {
				ReadWriteNBT profile = nbt.getOrCreateCompound("profile");
				ReadWriteNBT properties = profile.getCompoundList("properties").addCompound();
				if (name != null) {
					profile.setString("name", name);
				}
				properties.setString("name", "textures");
				properties.setString("value", base64);
			});
		} else {
			throw new IllegalStateException("NBTAPI is not enabled");
		}
	}

	// build

	/**
	 * アイテムをビルドします<br>
	 * 正確には内部のアイテムを取得しているだけなので処理的な意味はありません
	 *
	 * @return 作成されたアイテム
	 */
	@NotNull
	public ItemStack build() {
		return item;
	}

	/**
	 * アイテムを対象のプレイヤーに与えます。
	 * インベントリが埋まっていた場合はプレイヤーの位置にエンティティとして召喚します。
	 *
	 * @param p 対象のエンティティ
	 */
	public void give(@NotNull Player p) {
		GiveOrSpawn.give(p, build());
	}

	// clone

	/**
	 * クローンを生成します<br>
	 * 内部に保存されている{@link ItemStack}も同様にクローンされ、完全に別のものとして動きます
	 *
	 * @return cloneされたこのオブジェクト
	 */
	@NotNull
	@Override
	public SuperItemStack clone() {
		SuperItemStack item;
		try {
			item = (SuperItemStack) super.clone();
			item.item = this.item.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		return item;
	}

	// itemMeta setter, getter, reset

	/**
	 * アイテムの{@link ItemMeta}を直接指定します
	 *
	 * @param itemMeta 指定するアイテムメタ
	 * @return self
	 */
	@NotNull
	public SuperItemStack setItemMeta(@Nullable ItemMeta itemMeta) {
		item.setItemMeta(itemMeta);
		return this;
	}

	/**
	 * アイテムの{@link ItemMeta}を編集し、適応します
	 *
	 * @param function 編集する関数
	 * @return self
	 */
	@NotNull
	public SuperItemStack editItemMeta(@NotNull Consumer<ItemMeta> function) {
		item.editMeta(function);
		return this;
	}

	/**
	 * アイテムの{@link ItemMeta}が特定のクラスを継承したものだった場合にアップキャストし、編集、適応します
	 *
	 * @param function 編集する関数
	 * @return self
	 */
	@NotNull
	public <T extends ItemMeta> SuperItemStack editItemMeta(@NotNull Class<T> clazz, @NotNull Consumer<T> function) {
		item.editMeta(clazz, function);
		return this;
	}

	/**
	 * このアイテムの{@link ItemMeta}を取得します
	 *
	 * @return self
	 */
	@NotNull
	public ItemMeta getItemMeta() {
		return item.getItemMeta();
	}

	/**
	 * このアイテムの{@link ItemMeta}をデフォルト(初期状態)にリセットします
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack resetItemMeta() {
		setItemMeta(Bukkit.getItemFactory().getItemMeta(getType()));
		return this;
	}

	/**
	 * このアイテムの最大スタック数を取得します
	 *
	 * @return 最大スタック数
	 */
	public int getMaxStackSize() {
		return item.getMaxStackSize();
	}

	// name  setter, getter

	/**
	 * アイテムの表示名を{@link Component}で設定します
	 *
	 * @param component 表示名として設定する。nullを指定すると表示名が削除されます。
	 * @return self
	 */
	@NotNull
	public SuperItemStack name(@Nullable Component component) {
		item.editMeta(meta -> meta.displayName(component));
		return this;
	}

	/**
	 * アイテムの表示名を{@link ComponentUtil}経由の{@link Component}で設定します。
	 *
	 * @param value 表示名として設定する文字列。nullを指定すると表示名が削除されます。
	 * @return self
	 */
	@NotNull
	public SuperItemStack setName(@Nullable String value) {
		return name(ComponentUtil.text(value));
	}

	/**
	 * アイテムの表示名を{@link ComponentUtil}経由の{@link Component}で設定します。
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  表示名として設定する文字列。nullを指定すると表示名が削除されます。
	 * @return self
	 */
	@NotNull
	public SuperItemStack setName(char target, @Nullable String value) {
		return name(ComponentUtil.text(target, value));
	}

	/**
	 * アイテムの表示名を{@link Component}で取得します<br>
	 * アイテム名が存在しない場合はデフォルトのアイテム名が取得できます
	 *
	 * @return このアイテムの名前
	 */
	@NotNull
	public Component name() {
		return item.displayName();
	}

	/**
	 * アイテムの表示名を{@link ComponentUtil}を経由し{@link String}で取得します<br>
	 * アイテム名が存在しない場合はデフォルトのアイテム名が取得できます
	 *
	 * @return このアイテムの名前
	 */
	@NotNull
	public String getName() {
		return ComponentUtil.string(name());
	}

	// lore setter(Component, \n, Array, List), getter

	/**
	 * loreを{@link Component}の{@link List}で設定します
	 *
	 * @param value 設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack lore(@Nullable List<Component> value) {
		item.lore(value);
		return this;
	}

	/**
	 * loreを{@link String}を{@link ComponentUtil}を経由し、<code>\n</code>で改行したloreで設定します
	 *
	 * @param lore 設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLore(@Nullable String lore) {
		return lore(ComponentUtil.textList(lore));
	}

	/**
	 * loreを{@link String}を{@link ComponentUtil}を経由し、<code>\n</code>で改行したloreで設定します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param lore   設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLore(char target, @Nullable String lore) {
		return lore(ComponentUtil.textList(target, lore));
	}

	/**
	 * loreを{@link String}を{@link ComponentUtil}を経由し、配列で改行したloreで設定します
	 *
	 * @param lore 設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLore(String... lore) {
		return lore(ComponentUtil.textList(lore));
	}

	/**
	 * loreを{@link String}を{@link ComponentUtil}を経由し、配列で改行したloreで設定します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param lore   設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLore(char target, String... lore) {
		return lore(ComponentUtil.textList(target, lore));
	}


	/**
	 * loreを{@link String}の{@link List}を{@link ComponentUtil}を経由し、{@link List}で改行したloreで設定します
	 *
	 * @param lore 設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	public SuperItemStack setLore(@Nullable List<String> lore) {
		return lore(ComponentUtil.textList(lore));
	}

	/**
	 * loreを{@link String}の{@link List}を{@link ComponentUtil}を経由し、{@link List}で改行したloreで設定します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param lore   設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLore(char target, @Nullable List<String> lore) {
		return lore(ComponentUtil.textList(lore));
	}

	/**
	 * loreを{@link Component}の{@link List}で取得します
	 *
	 * @return 取得したlore、loreが存在しない場合nullが返されます
	 */
	@Nullable
	public List<Component> lore() {
		return item.lore();
	}

	/**
	 * nullにならないloreの{@link List}を取得します
	 *
	 * @return 取得したlore、loreが存在しない場合にも空の{@link List}が返されます
	 */
	@NotNull
	public List<Component> notNullLore() {
		return toNotNullList(lore());
	}

	/**
	 * loreを{@link ComponentUtil}経由の{@link String}の{@link List}で取得します
	 *
	 * @return 取得したlore、loreが存在しない場合nullが返されます
	 */
	@Nullable
	@Unmodifiable
	public List<String> getLore() {
		return ComponentUtil.string(lore());
	}

	/**
	 * loreを{@link ComponentUtil}経由の{@link String}の{@link List}で取得します
	 *
	 * @return 取得したlore、loreが存在しない場合にも空の{@link List}が返されます
	 */
	@NotNull
	@Unmodifiable
	public List<String> getNotNullLore() {
		return ComponentUtil.notNullString(lore());
	}

	/**
	 * 編集に便利な{@link LoreList}の状態で取得します。
	 *
	 * @return 取得した編集可能なLoreList
	 */
	@NotNull
	public LoreList getLoreList() {
		List<Component> lore = lore();
		if (lore == null) {
			return LoreList.create();
		}
		return LoreList.createComponent(lore);
	}

	/**
	 * {@link LoreList}を適応します。
	 * 内部的には{@link #lore(List)}を呼び出しています。
	 *
	 * @param loreList 設定するlore。nullの場合loreが削除されます
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLoreList(@Nullable LoreList loreList) {
		return lore(loreList);
	}

	/**
	 * loreを{@link ComponentUtil}経由の{@link String}を<code>\n</code>で接続したloreで取得します
	 *
	 * @return 取得したlore、loreが存在しない場合nullが返されます
	 */
	@Nullable
	public String getStringLore() {
		return Strings.join(lore(), '\n');
	}

	/**
	 * このアイテムがloreを持っているか取得します
	 *
	 * @return loreを持っている場合は true そうでなければ false
	 */
	public boolean hasLore() {
		return lore() != null;
	}

	/**
	 * loreのサイズを取得します
	 *
	 * @return loreのサイズ
	 */
	public int getLoreSize() {
		return notNullLore().size();
	}

	// loreAtIndex setter, getter

	/**
	 * 指定したindexのloreを{@link Component}で変更します<br>
	 * 指定したindexが現在のloreの長さの範囲外だった場合、その場所までの空のloreが作成されます
	 *
	 * @param index 設定するloreの位置
	 * @param value 設定する内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack loreAtIndex(int index, Component value) {
		List<Component> lore = lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		if (lore.size() <= index) {
			for (int i = lore.size(); i <= index; i++) {
				lore.add(Component.empty());
			}
		}
		lore.set(index, value);
		lore(lore);
		return this;
	}

	/**
	 * 指定したindexのloreを{@link ComponentUtil}経由の{@link String}で変更します<br>
	 * 指定したindexが現在のloreの長さの範囲外だった場合、その場所までの空のloreが作成されます
	 *
	 * @param index 設定するloreの位置
	 * @param value 設定する内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLoreAtIndex(int index, String value) {
		return loreAtIndex(index, ComponentUtil.text(value));
	}

	/**
	 * 指定したindexのloreを{@link ComponentUtil}経由の{@link String}で変更します<br>
	 * 指定したindexが現在のloreの長さの範囲外だった場合、その場所までの空のloreが作成されます
	 *
	 * @param index  設定するloreの位置
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  設定する内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLoreAtIndex(int index, char target, String value) {
		return loreAtIndex(index, ComponentUtil.text(target, value));
	}

	/**
	 * 指定したindexのloreを{@link Component}で取得します<br>
	 * 指定したindexが現在のloreの長さの範囲外、もしくはloreが存在しなかった場合、nullが返されます
	 *
	 * @param index 取得するloreの位置
	 * @return 取得したloreの内容、指定位置が要素数を超えているかloreがない場合nullになります
	 */
	@Nullable
	public Component loreAtIndex(int index) {
		List<Component> lore = lore();
		if (lore == null || lore.size() <= index) {
			return null;
		}
		return lore.get(index);
	}

	/**
	 * 指定したindexのloreを{@link ComponentUtil}経由の{@link String}で取得します<br>
	 * 指定したindexが現在のloreの長さの範囲外、もしくはloreが存在しなかった場合、nullが返されます
	 *
	 * @param index 取得するloreの位置
	 * @return 取得したloreの内容、指定位置が要素数を超えているかloreがない場合nullになります
	 */
	@Nullable
	public String getLoreAtIndex(int index) {
		return ComponentUtil.string(loreAtIndex(index));
	}

	// lore adder(void, index, ... value)

	/**
	 * {@link Component}のloreを現在あるloreの一番最後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addComponentLore(@NotNull List<Component> value) {
		List<Component> lore = lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.addAll(value);
		lore(lore);
		return this;
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(@NotNull List<String> value) {
		addComponentLore(ComponentUtil.textList(value));
		return this;
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(char target, @NotNull List<String> value) {
		addComponentLore(ComponentUtil.textList(target, value));
		return this;
	}

	/**
	 * {@link Component}のloreを現在あるloreの一番最後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(@NotNull Component value) {
		return addComponentLore(List.of(value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(@NotNull String value) {
		return addComponentLore(ComponentUtil.textList(value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(char target, @NotNull String value) {
		return addComponentLore(ComponentUtil.textList(target, value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(@NotNull String... value) {
		return addComponentLore(ComponentUtil.textList(value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを現在あるloreの一番最後に追加します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(char target, @NotNull String... value) {
		return addComponentLore(ComponentUtil.textList(target, value));
	}

	/**
	 * {@link Component}のloreを指定された位置の後に追加します
	 *
	 * @param index loreを追加する位置
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addComponentLore(int index, @NotNull List<Component> value) {
		List<Component> lore = lore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.addAll(Math.min(index, lore.size()), value);
		lore(lore);
		return this;
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを指定された位置の後に追加します
	 *
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(int index, @NotNull List<String> value) {
		addComponentLore(index, ComponentUtil.textList(value));
		return this;
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}をloreを指定された位置の後に追加します
	 *
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(int index, char target, @NotNull List<String> value) {
		addComponentLore(index, ComponentUtil.textList(target, value));
		return this;
	}

	/**
	 * {@link Component}のloreを指定された位置の後に追加します
	 *
	 * @param index loreを追加する位置
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(int index, @NotNull Component value) {
		return addComponentLore(index, List.of(value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}のloreを指定された位置の後に追加します
	 *
	 * @param index loreを追加する位置
	 * @param value 追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(int index, @NotNull String... value) {
		return addComponentLore(index, ComponentUtil.textList(value));
	}

	/**
	 * {@link ComponentUtil}経由の{@link String}のloreを指定された位置の後に追加します
	 *
	 * @param index  loreを追加する位置
	 * @param target 色などを指定する<code>§</code>の代わりとなる文字
	 * @param value  追加するloreの内容
	 * @return self
	 */
	@NotNull
	public SuperItemStack addLore(int index, char target, @NotNull String... value) {
		return addComponentLore(index, ComponentUtil.textList(target, value));
	}

	// lore remove(index, all, fromEnd)

	/**
	 * 指定された位置のloreを削除し、{@link Component}で取得します
	 *
	 * @param index loreを削除する位置
	 * @return 削除されたlore、指定位置が要素数を超えているかloreがない場合削除失敗しnullになります
	 */
	@Nullable
	public Component removeComponentLoreAtIndex(int index) {
		List<Component> lore = lore();
		if (lore == null || lore.size() <= index) {
			return null;
		}
		Component result = lore.remove(index);
		lore(lore);
		return result;
	}

	/**
	 * 指定された位置のloreを削除し、{@link ComponentUtil}経由の{@link String}で取得します
	 *
	 * @param index loreを削除する位置
	 * @return 削除されたlore、指定位置が要素数を超えているかloreがない場合削除失敗しnullになります
	 */
	@Nullable
	public String removeLoreAtIndex(int index) {
		return ComponentUtil.string(removeComponentLoreAtIndex(index));
	}

	/**
	 * 一番下のloreを削除し、{@link Component}で取得します
	 *
	 * @return 削除されたlore、loreがない場合nullになります
	 */
	@Nullable
	public Component removeComponentLoreFromEnd() {
		List<Component> lore = lore();
		if (lore == null) {
			return null;
		}
		Component result = lore.removeLast();
		lore(lore);
		return result;
	}

	/**
	 * 一番下のloreを削除し、{@link ComponentUtil}経由の{@link String}で取得します
	 *
	 * @return 削除されたlore、loreがない場合削除失敗しnullになります
	 */
	@Nullable
	public String removeLoreFromEnd() {
		return ComponentUtil.string(removeComponentLoreFromEnd());
	}

	/**
	 * 一番下から指定個数のloreを削除し、{@link Component}の{@link List}で取得します
	 *
	 * @param length 一番下から削除する個数
	 * @return 削除されたlore、loreが存在しないかlengthが0以下の場合削除失敗しnullになります
	 */
	@Nullable
	@Unmodifiable
	public List<Component> removeComponentLoreFromEnd(int length) {
		List<Component> lore = lore();
		if (lore == null || length <= 0) {
			return null;
		}
		List<Component> components = lore.subList(lore.size() - length, lore.size());
		List<Component> result = List.copyOf(components);
		components.clear();
		lore(lore);
		return result;
	}

	/**
	 * 一番下から指定個数のloreを削除し、{@link Component}の{@link List}で取得します
	 *
	 * @param length 一番下から削除する個数
	 * @return 削除されたlore、nullになることはなく、削除失敗しても必ず{@link List}になります
	 */
	@NotNull
	@Unmodifiable
	public List<Component> removeNotNullComponentLoreFromEnd(int length) {
		return toNotNullList(removeComponentLoreFromEnd(length));
	}

	/**
	 * 一番下から指定個数のloreを削除し、{@link ComponentUtil}経由の{@link String}の{@link List}で取得します
	 *
	 * @return 削除されたlore、loreが存在しないかlengthが0以下の場合削除失敗しnullになります
	 */
	@Nullable
	@Unmodifiable
	public List<String> removeLoreFromEnd(int length) {
		return ComponentUtil.string(removeComponentLoreFromEnd(length));
	}

	/**
	 * 一番下から指定個数のloreを削除し、{@link ComponentUtil}経由の{@link String}の{@link List}で取得します
	 *
	 * @return 削除されたlore、nullになることはなく、必ず{@link List}になります
	 */
	@NotNull
	@Unmodifiable
	public List<String> removeNotNullLoreFromEnd(int length) {
		return ComponentUtil.notNullString(removeComponentLoreFromEnd(length));
	}

	/**
	 * 全てのloreを削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeLore() {
		lore(null);
		return this;
	}

	// amount setter, getter

	/**
	 * アイテムの個数を指定します
	 *
	 * @param amount アイテムの個数
	 * @return self
	 */
	@NotNull
	public SuperItemStack setAmount(int amount) {
		item.setAmount(amount);
		return this;
	}

	/**
	 * アイテムの個数を指定します<br>
	 * 最大値がアイテムの最大スタック数に制限されます
	 *
	 * @param amount アイテムの個数
	 * @return self
	 */
	@NotNull
	public SuperItemStack setAmountOrMax(int amount) {
		item.setAmount(Math.min(amount, getMaxStackSize()));
		return this;
	}

	/**
	 * アイテムの個数を取得します
	 *
	 * @return アイテムの個数
	 */
	public int getAmount() {
		return item.getAmount();
	}

	// amount add, subtract

	/**
	 * 指定された数をアイテムに追加します
	 *
	 * @param amount 追加する数
	 * @return self
	 */
	@NotNull
	public SuperItemStack add(int amount) {
		item.add(amount);
		return this;
	}

	/**
	 * アイテムの数を1つ増加させます
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack add() {
		item.add();
		return this;
	}

	/**
	 * 指定された数をアイテムから削除します<br>
	 * 0になった場合、アイテムは削除されます
	 *
	 * @param amount 追加する数
	 * @return self
	 */
	@NotNull
	public SuperItemStack subtract(int amount) {
		item.subtract(amount);
		return this;
	}

	/**
	 * アイテムの数を1つ減少させます<br>
	 * 0になった場合、アイテムは削除されます
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack subtract() {
		item.subtract();
		return this;
	}

	// item clone & setAmount

	/**
	 * このクラスをクローンし、指定したアイテムの個数に設定します
	 *
	 * @param amount アイテムの個数
	 * @return cloneされたこのオブジェクト
	 */
	@NotNull
	public SuperItemStack asQuantity(int amount) {
		SuperItemStack clone = clone();
		clone.setAmount(amount);
		return clone;
	}

	/**
	 * このクラスをクローンし、アイテムの個数を1つに設定します
	 *
	 * @return cloneされたこのオブジェクト
	 */
	@NotNull
	public SuperItemStack asOne() {
		return asQuantity(1);
	}

	// itemType setter, getter

	/**
	 * アイテムのタイプ{@link Material}を設定します。
	 *
	 * @param material 設定する新しいアイテムのタイプ
	 * @return self
	 */
	@Deprecated
	@NotNull
	public SuperItemStack setType(@NotNull Material material) {
		item.setType(material);
		return this;
	}

	/**
	 * アイテムのタイプ{@link Material}を取得します
	 *
	 * @return アイテムのタイプ
	 */
	@NotNull
	public Material getType() {
		return item.getType();
	}

	// itemDamage setter, getter

	/**
	 * 耐久値がどれだけ減っているかを設定します
	 *
	 * @param damage 設定するダメージ値
	 * @return self
	 */
	@NotNull
	public SuperItemStack setDamage(int damage) {
		item.editMeta(Damageable.class, damageable -> damageable.setDamage(damage));
		return this;
	}

	/**
	 * 耐久値を取得します
	 *
	 * @return 耐久値がある場合はその値、ない場合は0を返します
	 */
	public int getDamage() {
		if (getItemMeta() instanceof Damageable damageable) {
			return damageable.getDamage();
		}
		return 0;
	}

	// itemUnbreakable setter, getter

	/**
	 * 耐久性が減少するかを指定します
	 *
	 * @param unbreakable 耐久値が減少するか
	 * @return self
	 */
	@NotNull
	public SuperItemStack setUnbreakable(boolean unbreakable) {
		item.editMeta(meta -> meta.setUnbreakable(unbreakable));
		return this;
	}

	/**
	 * 耐久値を減少しないように設定します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack setUnbreakable() {
		setUnbreakable(true);
		return this;
	}

	/**
	 * 耐久値が減少するかどうかを取得します
	 *
	 * @return 耐久値が減少しないなら true そうでなければ false
	 */
	public boolean isUnbreakable() {
		return getItemMeta().isUnbreakable();
	}

	// item destroy

	/**
	 * このアイテムを破壊します<br>
	 * このメソッドを実行した時点で破壊されます
	 */
	public void destroy() {
		setAmount(0);
	}

	// itemPotion setter, adder, getter

	/**
	 * ベースとなるポーションの種類{@link PotionType}を設定します
	 *
	 * @param type 設定するポーションの種類
	 * @return self
	 */
	@NotNull
	public SuperItemStack setPotionBaseType(@NotNull PotionType type) {
		item.editMeta(PotionMeta.class, potionMeta -> potionMeta.setBasePotionType(type));
		return this;
	}

	/**
	 * ポーションの色を設定します
	 *
	 * @param color 設定するポーションの色
	 * @return self
	 */
	@NotNull
	public SuperItemStack setPotionColor(@NotNull Color color) {
		item.editMeta(PotionMeta.class, potionMeta -> potionMeta.setColor(color));
		return this;
	}

	/**
	 * 指定されたポーションの種類と色を設定します<br>
	 * {@link #setPotionBaseType(PotionType)}と{@link #setPotionColor(Color)}の2つをまとめて呼び出すことができます
	 *
	 * @param type  設定するポーションの種類。
	 * @param color 設定するポーションの色
	 * @return 更新されたSuperItemStackオブジェクト。
	 */
	@NotNull
	public SuperItemStack setPotion(@NotNull PotionType type, @NotNull Color color) {
		item.editMeta(PotionMeta.class, potionMeta -> {
			potionMeta.setBasePotionType(type);
			potionMeta.setColor(color);
		});
		return this;
	}

	/**
	 * カスタムエフェクト効果をこのポーションに追加します
	 *
	 * @param effects 追加するエフェクト
	 * @return self
	 */
	@NotNull
	public SuperItemStack addPotionCustomEffect(@NotNull PotionEffect... effects) {
		item.editMeta(PotionMeta.class, potionMeta -> {
			for (PotionEffect effect : effects) {
				potionMeta.addCustomEffect(effect, true);
			}
		});
		return this;
	}

	/**
	 * ベースとなるポーションの種類{@link PotionType}を取得します
	 *
	 * @return 取得したポーションの種類
	 */
	@Nullable
	public PotionType getPotionBaseType() {
		if (getItemMeta() instanceof PotionMeta potionMeta) {
			return potionMeta.getBasePotionType();
		}
		return null;
	}

	/**
	 * ポーションの色を取得します
	 *
	 * @return 取得したポーションの色
	 */
	@Nullable
	public Color getPotionColor() {
		if (getItemMeta() instanceof PotionMeta potionMeta) {
			return potionMeta.getColor();
		}
		return null;
	}

	/**
	 * ポーションに追加されているカスタムエフェクトをすべて取得します
	 *
	 * @return 取得したカスタムエフェクト
	 */
	@NotNull
	public List<PotionEffect> getPotionCustomEffect() {
		if (getItemMeta() instanceof PotionMeta potionMeta) {
			return potionMeta.getCustomEffects();
		}
		return List.of();
	}

	// enchant glow setter

	/**
	 * このメソッドは、アイテムにエンチャントの輝き効果をつけるかどうかを設定します
	 *
	 * @param glow アイテムにエンチャントの輝きを追加する場合はtrue、解除する場合はfalseを指定します。
	 * @return self
	 */
	@NotNull
	public SuperItemStack setEnchantGlow(Boolean glow) {
		editItemMeta(i -> i.setEnchantmentGlintOverride(glow));
		return this;
	}

	/**
	 * アイテムにエンチャントの輝き効果を追加します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack setEnchantGlow() {
		return setEnchantGlow(true);
	}

	/**
	 * アイテムのエンチャントの輝き効果をリセットします
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack resetEnchantGlow() {
		return setEnchantGlow(null);
	}

	/**
	 * アイテムにエンチャントの輝き効果があるかどうかを判定します
	 *
	 * @return エンチャントの輝き効果を持っているなら true そうでなければ false
	 */
	public boolean isEnchantGlow() {
		return hasEnchants() || getItemMeta().getEnchantmentGlintOverride();
	}

	/**
	 * 指定されたエンチャント{@link Enchantment}とそのレベルを現在のアイテムに追加します<br>
	 * このエンチャントはレベル制限やアイテムの制限を回避します
	 *
	 * @param enchant 追加するエンチャント
	 * @param level   追加するエンチャントのレベル
	 * @return self
	 */
	@NotNull
	public SuperItemStack addEnchant(@NotNull Enchantment enchant, int level) {
		item.addUnsafeEnchantment(enchant, level);
		return this;
	}

	/**
	 * 指定されたエンチャント{@link Enchantment}とそのレベルの{@link Map}を現在のアイテムに追加します<br>
	 * このエンチャントはレベル制限やアイテムの制限を回避します
	 *
	 * @param enchants 追加するエンチャントとレベル
	 * @return self
	 */
	@NotNull
	public SuperItemStack addEnchants(@NotNull Map<Enchantment, Integer> enchants) {
		item.addUnsafeEnchantments(enchants);
		return this;
	}

	/**
	 * 指定されたエンチャント{@link Enchantment}とそのレベルを現在のアイテムに追加します<br>
	 * このエンチャントはレベル制限やアイテムの制限に縛られます
	 *
	 * @param enchant 追加するエンチャント
	 * @param level   追加するエンチャントのレベル
	 * @return self
	 */
	@NotNull
	public SuperItemStack addSafeEnchant(@NotNull Enchantment enchant, int level) {
		item.addEnchantment(enchant, level);
		return this;
	}

	/**
	 * 指定されたエンチャント{@link Enchantment}とそのレベルの{@link Map}を現在のアイテムに追加します<br>
	 * このエンチャントはレベル制限やアイテムの制限に縛られます
	 *
	 * @param enchants 追加するエンチャントとレベル
	 * @return self
	 */
	@NotNull
	public SuperItemStack addSafeEnchants(@NotNull Map<Enchantment, Integer> enchants) {
		item.addEnchantments(enchants);
		return this;
	}

	/**
	 * このアイテムが持つ{@link Enchantment}とそのレベルの{@link Map}を取得します
	 *
	 * @return エンチャントとレベルのMap
	 */
	@NotNull
	public Map<Enchantment, Integer> getEnchants() {
		return getItemMeta().getEnchants();
	}

	/**
	 * 指定された{@link Enchantment}のレベルを取得します。
	 *
	 * @param enchantment 対象のエンチャント
	 * @return エンチャントのレベル、そのエンチャントが付与されていない場合は0
	 */
	public int getEnchantLevel(@NotNull Enchantment enchantment) {
		return getItemMeta().getEnchantLevel(enchantment);
	}

	/**
	 * アイテムが{@link Enchantment}を持っているかどうかを取得します
	 *
	 * @return エンチャントを持っているなら true そうでなければ false
	 */
	public boolean hasEnchants() {
		return getItemMeta().hasEnchants();
	}

	/**
	 * 指定された{@link Enchantment}がアイテムに付与されているかどうかを確認します
	 *
	 * @param enchantment 確認するエンチャント
	 * @return 指定されたエンチャントが付与されているなら true そうでなければ false
	 */
	public boolean hasEnchant(@NotNull Enchantment enchantment) {
		return getItemMeta().hasEnchant(enchantment);
	}

	/**
	 * 指定された{@link Enchantment}を削除します
	 *
	 * @param enchants 削除するエンチャント
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeEnchants(@NotNull Enchantment... enchants) {
		item.editMeta(meta -> {
			for (Enchantment enchant : enchants) {
				meta.removeEnchant(enchant);
			}
		});
		return this;
	}

	/**
	 * アイテムに付与されたすべての{@link Enchantment}を削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeAllEnchants() {
		item.editMeta(ItemMeta::removeEnchantments);
		return this;
	}

	// item showOnly

	/**
	 * アイテムが表示専用のものかどうかを指定します<br>
	 * 表示専用アイテムになると移動やクリック時にそのアイテムは自動で削除されるようになります
	 *
	 * @param showOnly 表示専用アイテムにするか
	 * @return self
	 */
	@NotNull
	public SuperItemStack setShowOnly(boolean showOnly) {
		if (showOnly) {
			setItemData(SHOW_ONLY, true);
		} else {
			removeItemData(SHOW_ONLY);
		}
		return this;
	}

	/**
	 * アイテムを表示専用に設定します<br>
	 * 表示専用アイテムになると移動やクリック時にそのアイテムは自動で削除されるようになります
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack setShowOnly() {
		return setShowOnly(true);
	}

	/**
	 * アイテムの表示専用を削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeShowOnly() {
		return setShowOnly(false);
	}

	/**
	 * アイテムが表示専用か取得します
	 *
	 * @return 表示専用なら true そうでなければ false
	 */
	public boolean isShowOnly() {
		return hasItemData(SHOW_ONLY);
	}

	// item moveLock

	/**
	 * アイテムに移動制限をかけるか指定します<br>
	 * 移動制限をかけると、プレイヤーのインベントリから出すことができなくなります
	 *
	 * @param moveLock アイテムに移動制限をかけるか
	 * @return self
	 */
	@NotNull
	public SuperItemStack setMoveLock(boolean moveLock) {
		if (moveLock) {
			setItemData(MOVE_LOCK, true);
		} else {
			removeItemData(MOVE_LOCK);
		}
		return this;
	}

	/**
	 * アイテムに移動制限を設定します<br>
	 * 表示専用アイテムになると移動やクリック時にそのアイテムは自動で削除されるようになります
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack setMoveLock() {
		return setMoveLock(true);
	}

	/**
	 * アイテムに移動制限を削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeMoveLock() {
		return setMoveLock(false);
	}

	/**
	 * アイテムが移動制限されているか取得します
	 *
	 * @return 表示専用なら true そうでなければ false
	 */
	public boolean isMoveLock() {
		return hasItemData(MOVE_LOCK);
	}

	/**
	 * 指定した権限レベルをアイテムに設定します
	 *
	 * @param permission アイテムに設定する権限レベル
	 * @return self
	 */
	@NotNull
	public SuperItemStack setItemPermission(@NotNull ItemPermission permission) {
		return setItemPermission(permission.getLevel());
	}

	// item permission setter, getter

	/**
	 * 指定した権限レベルをアイテムに設定します。
	 *
	 * @param level 設定するレベル。0から3の間でなければなりません
	 *              <ul>
	 *              <b>制限レベル</b>
	 *              <li>0 なし</li>
	 *              <li>1 Creative、Spectator もしくは OP</li>
	 *              <li>2 OP</li>
	 *              <li>3 Creative、Spectator かつ OP</li>
	 *              </ul>
	 * @return self
	 */
	@NotNull
	public SuperItemStack setItemPermission(@Range(from = 0, to = 3) int level) {
		if (level == 0) {
			removeItemPermission();
		} else {
			setItemData(ITEM_PERMISSION, (byte) level);
		}
		return this;
	}

	/**
	 * アイテムの権限レベルを取得します
	 *
	 * @return 権限レベル
	 */
	public int getItemPermission() {
		return getItemDataOrDefault(ITEM_PERMISSION, (byte) 0);
	}

	/**
	 * アイテムから権限レベルを削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeItemPermission() {
		removeItemData(ITEM_PERMISSION);
		return this;
	}

	/**
	 * アイテムの修理コストを設定します
	 *
	 * @param cost 修理コストの値
	 * @return self
	 */
	@NotNull
	public SuperItemStack setRepairCost(int cost) {
		item.editMeta(Repairable.class, meta -> meta.setRepairCost(cost));
		return this;
	}

	/**
	 * 革装備の色を設定します
	 *
	 * @param color 設定する色
	 * @return self
	 */
	@NotNull
	public SuperItemStack setLeatherColor(Color color) {
		item.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(color));
		return this;
	}

	// itemData adder, has, remove, getter

	/**
	 * 指定された{@link NamespacedKey}と値のペアをこのアイテムのデータコンテナに追加します
	 *
	 * @param type  データのタイプ
	 * @param key   データのキー
	 * @param value 保存する値
	 * @return self
	 *
	 * @deprecated 内部で呼び出されているのはsetメソッドで、addという名称は適切ではないため、変更されました。
	 */
	@Deprecated
	@NotNull
	public <T, Z> SuperItemStack addItemData(@NotNull PersistentDataType<T, Z> type, @NotNull NamespacedKey key, @NotNull Z value) {
		item.editMeta(meta -> meta.getPersistentDataContainer().set(key, type, value));
		return this;
	}

	/**
	 * 指定された{@link ContainerKey}をこのアイテムのデータコンテナに追加します
	 *
	 * @param containerKey データのキーとタイプ
	 * @param value        保存する値
	 * @return self
	 *
	 * @deprecated 内部で呼び出されているのはsetメソッドで、addという名称は適切ではないため、変更されました。
	 */
	@Deprecated
	@NotNull
	public <T, Z> SuperItemStack addItemData(@NotNull ContainerKey<T, Z> containerKey, @NotNull Z value) {
		return addItemData(containerKey.type(), containerKey.key(), value);
	}

	/**
	 * 指定された{@link NamespacedKey}と値のペアをこのアイテムのデータコンテナに設定します
	 *
	 * @param type  データのタイプ
	 * @param key   データのキー
	 * @param value 保存する値
	 * @return self
	 */
	@NotNull
	public <T, Z> SuperItemStack setItemData(@NotNull PersistentDataType<T, Z> type, @NotNull NamespacedKey key, @NotNull Z value) {
		item.editMeta(meta -> meta.getPersistentDataContainer().set(key, type, value));
		return this;
	}

	/**
	 * 指定された{@link ContainerKey}をこのアイテムのデータコンテナに設定します
	 *
	 * @param containerKey データのキーとタイプ
	 * @param value        保存する値
	 * @return self
	 */
	@NotNull
	public <T, Z> SuperItemStack setItemData(@NotNull ContainerKey<T, Z> containerKey, @NotNull Z value) {
		return setItemData(containerKey.type(), containerKey.key(), value);
	}

	/**
	 * 指定された{@link NamespacedKey}がこのアイテムのデータコンテナにあるかどうかを判定します
	 *
	 * @param key データのキー
	 * @return 指定されたデータがあるなら true そうでなければ false
	 */
	public boolean hasItemData(@NotNull NamespacedKey key) {
		return getItemMeta().getPersistentDataContainer().has(key);
	}

	/**
	 * 指定された{@link ContainerKey}がこのアイテムのデータコンテナにあるかどうかを判定します
	 *
	 * @param containerKey データのキー
	 * @return 指定されたデータがあるなら true そうでなければ false
	 */
	public boolean hasItemData(@NotNull ContainerKey<?, ?> containerKey) {
		return getItemMeta().getPersistentDataContainer().has(containerKey.key());
	}

	/**
	 * 指定された{@link NamespacedKey}をこのアイテムのデータコンテナから削除します
	 *
	 * @param keys データのキー
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeItemData(@NotNull NamespacedKey... keys) {
		item.editMeta(meta -> {
			PersistentDataContainer container = meta.getPersistentDataContainer();
			for (NamespacedKey key : keys) {
				container.remove(key);
			}
		});
		return this;
	}

	/**
	 * 指定された{@link ContainerKey}をこのアイテムのデータコンテナから削除します
	 *
	 * @param containerKey データのキー
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeItemData(@NotNull ContainerKey<?, ?>... containerKey) {
		return removeItemData(Arrays.stream(containerKey).map(ContainerKey::key).toArray(NamespacedKey[]::new));
	}

	/**
	 * このアイテムのデータコンテのから全てのデータを削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeAllItemData() {
		return removeItemData(getItemDataKeys().toArray(NamespacedKey[]::new));
	}

	/**
	 * 指定された{@link PersistentDataType}と{@link NamespacedKey}に基づいて、アイテムのデータを取得します。
	 *
	 * @param type データの種類
	 * @param key  データのキー
	 * @return キーが存在すれば保存されていた値、無ければnull
	 */
	@Nullable
	public <T, Z> Z getItemData(@NotNull PersistentDataType<T, Z> type, @NotNull NamespacedKey key) {
		return getItemMeta().getPersistentDataContainer().get(key, type);
	}

	/**
	 * 指定された{@link ContainerKey}に基づいて、アイテムのデータを取得します。
	 *
	 * @param containerKey データの種類とキー
	 * @return キーが存在すれば保存されていた値、無ければnull
	 */
	@Nullable
	public <T, Z> Z getItemData(@NotNull ContainerKey<T, Z> containerKey) {
		return getItemData(containerKey.type(), containerKey.key());
	}

	/**
	 * 指定された{@link PersistentDataType}と{@link NamespacedKey}に基づいて、アイテムのデータを取得します。
	 *
	 * @param type         データの種類
	 * @param key          データのキー
	 * @param defaultValue データが存在しなかった場合の値
	 * @return キーが存在すれば保存されていた値、無ければ<code>defaultValue</code>
	 */
	@NotNull
	public <T, Z> Z getItemDataOrDefault(@NotNull PersistentDataType<T, Z> type, @NotNull NamespacedKey key, @NotNull Z defaultValue) {
		Z value = getItemData(type, key);
		return value == null ? defaultValue : value;
	}

	/**
	 * 指定された{@link ContainerKey}に基づいて、アイテムのデータを取得します。
	 *
	 * @param containerKey データの種類とキー
	 * @param defaultValue データが存在しなかった場合の値
	 * @return キーが存在すれば保存されていた値、無ければ<code>defaultValue</code>
	 */
	@NotNull
	public <T, Z> Z getItemDataOrDefault(@NotNull ContainerKey<T, Z> containerKey, @NotNull Z defaultValue) {
		return getItemDataOrDefault(containerKey.type(), containerKey.key(), defaultValue);
	}

	/**
	 * 指定されたアイテムのデータキーを全て取得します
	 *
	 * @return 全てのアイテムデータのキー
	 */
	@NotNull
	public Set<NamespacedKey> getItemDataKeys() {
		return getItemMeta().getPersistentDataContainer().getKeys();
	}

	/**
	 * {@link PersistentDataContainer}を取得します
	 *
	 * @return 取得したPersistentDataContainer
	 */
	@NotNull
	public PersistentDataContainer getPersistentDataContainer() {
		return item.getItemMeta().getPersistentDataContainer();
	}

	/**
	 * アイテムのデータコンテナをセットします
	 *
	 * @param container 変更する内容
	 * @param replace   データを置き換えるか
	 * @return self
	 */
	@NotNull
	public SuperItemStack setPersistentDataContainer(@NotNull PersistentDataContainer container, boolean replace) {
		item.editMeta(meta -> container.copyTo(meta.getPersistentDataContainer(), replace));
		return this;
	}

	/**
	 * {@link DataContainer}を取得します
	 *
	 * @return 取得したDataContainer
	 */
	@NotNull
	public DataContainer getDataContainer() {
		return new DataContainer(item);
	}

	/**
	 * {@link DataContainer}の内容でセットします
	 *
	 * @param container 変更する内容
	 * @param replace   データを置き換えるか
	 * @return self
	 */
	@NotNull
	public SuperItemStack setDataContainer(@NotNull DataContainer container, boolean replace) {
		item.editMeta(meta -> container.copyTo(meta.getPersistentDataContainer(), replace));
		return this;
	}

	/**
	 * 指定した{@link ItemFlag}を追加します
	 *
	 * @param flags 追加するアイテムフラグ
	 * @return self
	 */
	@NotNull
	public SuperItemStack addItemFlags(@NotNull ItemFlag... flags) {
		item.addItemFlags(flags);
		return this;
	}

	/**
	 * 指定された{@link ItemFlag}が存在するかどうかを確認します。
	 *
	 * @param flag 確認するアイテムフラグ
	 * @return 指定されたフラグが存在するなら true そうでなければ false
	 */
	public boolean hasItemFlag(@NotNull ItemFlag flag) {
		return item.hasItemFlag(flag);
	}

	/**
	 * 指定された{@link ItemFlag}を削除します
	 *
	 * @param flags 削除するアイテムフラグ
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeItemFlag(@NotNull ItemFlag... flags) {
		item.removeItemFlags(flags);
		return this;
	}

	/**
	 * カスタムモデルデータを設定します。
	 *
	 * @param model カスタムモデルのID
	 * @return self
	 */
	@NotNull
	public SuperItemStack setCustomModelData(@Nullable Integer model) {
		item.editMeta(meta -> meta.setCustomModelData(model));
		return this;
	}

	/**
	 * カスタムモデルデータを取得します
	 *
	 * @return カスタムモデルデータが指定されている場合はその数値、されていない場合はnull
	 */
	@Nullable
	public Integer getCustomModelData() {
		ItemMeta itemMeta = getItemMeta();
		if (itemMeta.hasCustomModelData()) {
			return itemMeta.getCustomModelData();
		}
		return null;
	}

	/**
	 * カスタムモデルデータを削除します
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack removeCustomModelData() {
		getItemMeta().setCustomModelData(null);
		return this;
	}


	/**
	 * アイテムのインベントリに対して処理を行います<br>
	 * 関数の返り値がtrueの場合インベントリに対して行った変更が保存されます<br>
	 * falseの場合は変更は保存されません
	 *
	 * @param function アイテムのインベントリを渡される関数
	 * @return self
	 */
	@NotNull
	public SuperItemStack onItemInventory(@NotNull Predicate<Inventory> function) {
		item.editMeta(BlockStateMeta.class, meta -> {
			if (meta.getBlockState() instanceof Container inventoryItem) {
				if (function.test(inventoryItem.getInventory())) {
					meta.setBlockState(inventoryItem);
				}
			}
		});
		return this;
	}

	/**
	 * 指定されたアイテムのインベントリを取得します<br>
	 * 変更は保存されません
	 *
	 * @return アイテムのインベントリが存在する場合はそれを返し、存在しない場合はnullを返します。
	 */
	@Nullable
	public Inventory getItemInventory() {
		if (getItemMeta() instanceof BlockStateMeta blockStateMeta) {
			if (blockStateMeta.getBlockState() instanceof Container inventoryItem) {
				return inventoryItem.getInventory();
			}
		}
		return null;
	}

	/**
	 * 指定されたアイテムのインベントリを取得します<br>
	 * 変更は保存されません
	 *
	 * @return アイテムのインベントリが存在する場合はそれを返し、存在しない場合はnullを返します。
	 */
	@Nullable
	public SuperItemStack setItemInventory(@NotNull Inventory inventory) {
		return onItemInventory(i -> {
			i.setContents(inventory.getContents());
			return true;
		});
	}

	/**
	 * 指定されたアイテムのインベントリをクリアします
	 *
	 * @return self
	 */
	@NotNull
	public SuperItemStack clearItemInventory() {
		onItemInventory(inventory -> {
			inventory.clear();
			return true;
		});
		return this;
	}

	/**
	 * アイテムのインベントリにあるアイテム全てに対して処理を行います
	 *
	 * @param function Integer(そのインベントリのindex)とそのスロットにあるSuperItemStackが渡されます
	 * @param deep     アイテムのインベントリの中のインベントリを持つアイテムに対しても同様に無限入れ子で処理を行うか
	 * @return self
	 */
	@NotNull
	public SuperItemStack onInventoryItems(@NotNull BiConsumer<Integer, SuperItemStack> function, boolean deep) {
		onItemInventory(inv -> {
			int index = -1;
			for (SuperItemStack item : Arrays.stream(inv.getContents()).map(SuperItemStack::safeInit).toList()) {
				index++;
				if (item == null) {
					continue;
				}
				item.onInventoryItems(index, function, deep);
			}
			return true;
		});
		return this;
	}

	private void onInventoryItems(int index, @NotNull BiConsumer<Integer, SuperItemStack> function, boolean deep) {
		function.accept(index, this);
		if (deep) {
			onInventoryItems(function, true);
		}
	}

	/**
	 * アイテムを文字列に変換します
	 *
	 * @return 文字列に変換したItemStack
	 */
	@NotNull
	@Override
	public String toString() {
		YamlConfiguration yml = new YamlConfiguration();
		yml.set("i", build());
		return Objects.requireNonNull(yml.getString("i"));
	}

	/**
	 * 文字列からアイテムを取得します
	 */
	@Contract("null -> null")
	@Nullable
	public static SuperItemStack parseString(@Nullable String string) {
		return safeInit(parseStringItem(string));
	}

	/**
	 * 文字列からアイテムを取得します
	 */
	@Nullable
	public static ItemStack parseStringItem(@Nullable String string) {
		if (string == null) {
			return null;
		}
		YamlConfiguration yml = new YamlConfiguration();
		yml.set("i", string);
		ConfigurationSection select = yml.getConfigurationSection("i");
		if (select != null) {
			return ItemStack.deserialize(select.getValues(true));
		}
		return null;
	}

	/**
	 * このメソッドは、指定されたオブジェクトがこのアイテムと等しいかどうかを比較します
	 *
	 * @param obj 比較対象のオブジェクト
	 * @return 指定されたオブジェクトが同じアイテム場合は true、そうでなければ false
	 */
	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof SuperItemStack itemStack)) {
			return false;
		}
		if (itemStack == this) {
			return true;
		}
		return itemStack.build().equals(build());
	}

	@Override
	public int hashCode() {
		return build().hashCode();
	}

	public boolean isSimilar(@Nullable ItemStack item) {
		if (item == null || item.isEmpty()) {
			return false;
		}
		return this.item.isSimilar(item);
	}

	/**
	 * 与えられたアイテムが個数以外同じか判定します
	 */
	public boolean isSimilar(@Nullable SuperItemStack item) {
		if (item == null) {
			return false;
		}
		return isSimilar(item.build());
	}

	/**
	 * 与えられたItemStackが{@link #getAmount()}, {@link #getDamage()}, {@link #isShowOnly()}, {@link #getItemPermission()} を無視し、同じアイテムかどうかを判定します。
	 *
	 * @param item 比較対象のItemStack
	 * @return 与えられたItemStackが同じアイテムの場合は true、そうでなければ false
	 */
	public boolean isSameItem(@Nullable ItemStack item) {
		if (item == null || item.isEmpty()) {
			return false;
		}
		return isSameItem(SuperItemStack.safeInit(item));
	}

	/**
	 * 与えられたSuperItemStackが{@link #getAmount()}, {@link #getDamage()}, {@link #isShowOnly()}, {@link #getItemPermission()} を無視し、同じアイテムかどうかを判定します
	 *
	 * @param item 比較対象のSuperItemStack
	 * @return 与えられたSuperItemStackが同じアイテムの場合は true、そうでなければ false
	 */
	public boolean isSameItem(@Nullable SuperItemStack item) {
		return isSameItem(item, true, true, false, false, false, false, true, true);
	}

	/**
	 * 引数の条件に従い、2つのアイテムを比較、判定します
	 *
	 * @param item                 比較対象となるアイテム
	 * @param bypassAmount         アイテムの個数を無視するか
	 * @param bypassDamage         アイテムの耐久値を無視するか
	 * @param bypassEnchant        アイテムのエンチャントを無視するか
	 * @param bypassName           アイテムの名前を無視するか
	 * @param bypassLore           アイテムの説明文を無視するか
	 * @param bypassDataContainer  アイテムのプラグイン用データ置き場を無視するか
	 * @param bypassShowOnly       アイテムの表示のみ設定を無視するか <b>(checkDataContainerがtrueの場合常時true)</b>
	 * @param bypassItemPermission アイテムの権限設定を無視するか <b>(checkDataContainerがtrueの場合常時true)</b>
	 * @return 引数の条件に従い、2つのアイテムが一致していたら true、そうでなければ false
	 */
	public boolean isSameItem(@Nullable SuperItemStack item, boolean bypassAmount, boolean bypassDamage, boolean bypassEnchant, boolean bypassName, boolean bypassLore, boolean bypassDataContainer, boolean bypassShowOnly, boolean bypassItemPermission) {
		if (item == null) {
			return false;
		}
		SuperItemStack item1 = clone();
		SuperItemStack item2 = item.clone();
		if (bypassAmount) {
			item1.setAmount(1);
			item2.setAmount(1);
		}
		if (bypassDamage) {
			item1.setDamage(0);
			item2.setDamage(0);
		}
		if (bypassEnchant) {
			item1.removeAllEnchants();
			item2.removeAllEnchants();
		}
		if (bypassName) {
			item1.setName(null);
			item2.setName(null);
		}
		if (bypassLore) {
			item1.lore(null);
			item2.lore(null);
		}
		if (bypassDataContainer) {
			item1.removeAllItemData();
			item2.removeAllItemData();
		} else {
			if (bypassShowOnly) {
				item1.removeShowOnly();
				item2.removeShowOnly();
			}
			if (bypassItemPermission) {
				item1.removeItemPermission();
				item2.removeItemPermission();
			}
		}
		return item1.equals(item2);
	}

	@NotNull
	private <T> List<T> toNotNullList(@Nullable List<T> items) {
		if (items != null) {
			return items;
		}
		return List.of();
	}
}
