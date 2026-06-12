package net.logiench.shardCore.listener;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.text.ComponentUtil;
import net.logiench.logienchlibv2.api.minecraft.time.Task;
import net.logiench.logienchlibv2.api.minecraft.time.Timer;
import net.logiench.shardCore.ShardCore;
import net.logiench.shardCore.core.damage._indicator.DamageIndicator;
import net.logiench.shardCore.core.item.base.def.ShardItem;
import net.logiench.shardCore.core.item.system._weapon.BowAttack;
import net.logiench.shardCore.core.item.system.data.ItemDataHandler;
import net.logiench.shardCore.core.item.system.generator.ItemGenerator;
import net.logiench.shardCore.core.item.system.loader.ItemInspector;
import net.logiench.shardCore.core.item.system.loader.ItemLoader;
import net.logiench.shardCore.core.item.system.module.context.ContextKey;
import net.logiench.shardCore.core.item.system.module.context.ReadContext;
import net.logiench.shardCore.core.item.system.module.params.GenerationParameters;
import net.logiench.shardCore.core.item.system.module.params.UpdateParameters;
import net.logiench.shardCore.core.itemRequirement.base.ItemRequirement;
import net.logiench.shardCore.core.menu.MenuFactory;
import net.logiench.shardCore.core.menu.main.MainMenu;
import net.logiench.shardCore.core.mob.system.generator.MobGenerator;
import net.logiench.shardCore.core.player.system.PlayerSessionManager;
import net.logiench.shardCore.core.player.system._PlayerCharacterManager;
import net.logiench.shardCore.core.player.system.stash.PlayerStashManager;
import net.logiench.shardCore.core.skill.system.SkillManager;
import net.logiench.shardCore.data.item.def.equipment.armor.chestplate.ObsidianChestplate;
import net.logiench.shardCore.data.item.def.gem.TestGem;
import net.logiench.shardCore.data.item.module.gem.GemKeys;
import net.logiench.shardCore.data.item.module.gem.GemModule;
import net.logiench.shardCore.data.item.module.prefix.PrefixKeys;
import net.logiench.shardCore.data.item.module.requirement.EquipmentReqModule;
import net.logiench.shardCore.data.item.module.requirement.RequirementsKeys;
import net.logiench.shardCore.data.item.module.stats.StatsKeys;
import net.logiench.shardCore.data.itemRequirement.MinLevelReqType;
import net.logiench.shardCore.data.mob.def.TestMob;
import net.logiench.shardCore.data.stats.keys.CoreStats;
import net.logiench.shardCore.db.DatabaseManager;
import net.logiench.shardCore.db.repository.Job;
import net.logiench.shardCore.db.service.PlayerStashContent;
import net.logiench.shardCore.db.service.StashItemData;
import net.logiench.shardCore.event.PlayerStatsUpdateEvent;
import net.logiench.shardCore.loader.KtsItemLoader;
import net.logiench.shardCore.register.GemRegistry;
import net.logiench.shardCore.register.ItemRegistry;
import net.logiench.shardCore.register.RequirementRegistry;
import net.logiench.shardLib.api.ShardLibProvider;
import net.logiench.shardLib.api.player.PlayerAttributeAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EventListener implements Listener {
	private final Injector injector;

	@Inject
	public EventListener(Injector injector) {
		this.injector = injector;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onServerLoad(ServerLoadEvent ev) {
		// すべてのワールドに対する設定
		Bukkit.getWorlds().forEach(world -> {
			// 自動回復されるとデータHPとの見た目がズレてしまうから無効化
			world.setGameRule(GameRule.NATURAL_REGENERATION, false);
		});

		// スキルのための毎ティック処理
		SkillManager skillManager = injector.getInstance(SkillManager.class);
		Timer.on(skillManager::onTick, 1, 1);

		// 激重処理の完了待機。必ずこのメソッドの一番下に配置すること
		injector.getInstance(KtsItemLoader.class).waitForCompletion();
		// これはItemの新規登録をロックするから、Loaderの処理が完了するwaitForCompletionの下に配置すること
		injector.getInstance(ItemRegistry.class).setImmutable();
	}

	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent ev) {
		Player p = ev.getPlayer();

		DatabaseManager database = injector.getInstance(DatabaseManager.class);
		/*database.executeAsync(TargetDatabase.MAIN, () -> {
			injector.getInstance(PlayerBaseRepository.class).upsert(new PlayerBaseEntity(p.getUniqueId(), ))
			PlayerBaseEntity data = new PlayerBaseEntity(p.getUniqueId(), p.getName());
			manager.getRepository(PlayerBaseRepository.class).upsertName(conn, data);
		});*/
	}

	@EventHandler
	private void onPlayerSpawn(PlayerRespawnEvent ev) {
		ShardLibProvider.get().getPlayerAPI().getCharacterAPI(ev.getPlayer()).ifPresent(character -> {
			PlayerAttributeAPI api = character.getAttributeAPI();
			api.setBaseAttribute(CoreStats.HP, api.getFinalAttribute(CoreStats.MAX_HP));
			api.recalculateStats();
		});
	}

	@EventHandler
	private void onPlayerChat(AsyncChatEvent ev) {
		Player p = ev.getPlayer();
		switch (ComponentUtil.string(ev.message())) {
			case "1" -> {
				System.out.println(injector.getInstance(ItemRegistry.class).getAllItems().stream().map(i -> i.getName() + "[" + i.getItemType() + ", " + i.getMaterial() + "]").collect(Collectors.joining(" | ")));
			}
			case "2" -> {
				Task.on(() -> {
					/*_MobGenerator.SpawnResult result = injector.getInstance(_MobGenerator.Factory.class).create(TestMob.class).simpleSpawn(p.getLocation(), 1);
					AttributeAPI att = result.attributeAPI();
					Optional<AttributeDefinitionRegister> registerOptional = ShardLibProvider.get().getRegister().mob().attributes().get("test");
					if (result.entity() instanceof Mob mob) {
						Bukkit.getMobGoals().addGoal(mob, -99999999, new TestGoal(mob));
					}*/

					injector.getInstance(MobGenerator.class).spawn(TestMob.class, p.getLocation(), 1);
				});
			}

			case "3" -> {
				PlayerAttributeAPI api = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				api.setBaseAttribute("natural_damage", new Random().nextDouble(50));
				api.recalculateStats();
			}

			case "get" -> {
				ItemStack item = p.getInventory().getItemInMainHand();
				System.out.println("=".repeat(30));
				if (item.isEmpty()) {
					PlayerAttributeAPI api = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();

					api.recalculateStats();

					System.out.println(new Gson().toJson(api.getFinalAttributes()));
				} else {
					ShardLibProvider.get().getItemAPI().getItemData(SuperItemStack.safeInit(item)).ifPresentOrElse(i -> {
						System.out.println(new Gson().toJson(i.getBaseStats()));
					}, () -> {
						System.out.println("EMPTY");
					});
				}
				System.out.println("-".repeat(30));
			}

			case "4" -> {
				ItemLoader loader = ItemLoader.of(p.getInventory().getItemInMainHand());
				if (loader != null) {
					ItemInspector inspector = injector.getInstance(ItemInspector.class);
					ReadContext context = inspector.inspect(loader);

					Logger logger = ShardCore.getPLogger();
					logger.info("-------------- Listener --------------");
					for (ContextKey<?> key : List.of(GemKeys.CTX_GEM_SLOT_SIZE, PrefixKeys.CTX_PREFIX, StatsKeys.CTX_MAIN_STATS, StatsKeys.CTX_SUB_STATS, StatsKeys.CTX_UNIQUE_STATS)) {
						Object value = context.get(key);
						if (value == null) {
							logger.info("| " + key.key() + "  =  null");
							continue;
						}
						String v = value.toString();
						if (value instanceof Map<?, ?> map) {
							v = map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
						} else if (value instanceof List<?> list) {
							v = list.stream().map(Object::toString).collect(Collectors.joining(", "));
						}
						logger.info("| " + key.key() + "  =  " + v);
					}
					logger.info("--------------------------------------");
				}
			}
			case "5" -> {
				SuperItemStack item = SuperItemStack.safeInit(p.getInventory().getItemInMainHand());
				if (item != null) {
					injector.getInstance(ItemGenerator.class).appraise(item, null).printMessage().ifSuccess(i -> i.give(p));
				}
			}
			case "6" -> {
				Task.on(() -> {
					p.getNearbyEntities(10, 10, 10).forEach(entity -> {
						if (entity instanceof Mob mob) {
							mob.getPathfinder().findPath(p.getLocation()).getPoints().forEach(System.out::println);
						}
					});
				});
			}
			case "8" -> {
				GenerationParameters params = GenerationParameters.of();
				//				params.put(GenerationParameters.MODEL_COLORS, List.of(Color.RED));

				injector.getInstance(ItemGenerator.class).generateNew(ObsidianChestplate.class, params).printMessage().ifSuccess(i -> i.give(p));
			}
			case "9" -> {
				ItemStack item = ItemStack.of(Material.DIAMOND);
				item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().build());
				CustomModelData data = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
				System.out.println(data.floats().getClass().getSimpleName());
				data.floats().add(10f);
				item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, data);
				SuperItemStack.init(item).give(p);
			}
			case "gem" -> {
				SuperItemStack item = SuperItemStack.safeInit(p.getInventory().getItemInMainHand());
				if (item != null) {
					UpdateParameters uParams = UpdateParameters.of(
						Set.of(GemModule.class)
					);

					uParams.put(GemKeys.UDT_ADD_GEM, List.of(injector.getInstance(GemRegistry.class).get(TestGem.class)));

					injector.getInstance(ItemGenerator.class).update(item, uParams).printMessage().ifSuccess(i -> i.give(p));
				}
			}
			case "eq" -> {
				PlayerInventory inv = p.getInventory();
				ItemStack main = inv.getItemInMainHand();
				ItemStack off = inv.getItemInOffHand();
				System.out.printf("equals: %s, similar: %s\n", main.equals(off), main.isSimilar(off));
				p.sendMessage("§e> equals: %s, similar: %s".formatted(main.equals(off), main.isSimilar(off)));
			}
			case "lv-u" -> {
				PlayerAttributeAPI api = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				api.addBaseAttribute("level", 1);
				api.recalculateStats();
			}
			case "lv-d" -> {
				PlayerAttributeAPI api = ShardLibProvider.get().getPlayerAPI().getCharacterAPI(p).orElseThrow().getAttributeAPI();
				api.subtractBaseAttribute("level", 1);
				api.recalculateStats();
			}
			case "req" -> {
				UpdateParameters params = UpdateParameters.of(Set.of(EquipmentReqModule.class));
				params.put(RequirementsKeys.UDT_ADD_REQUIREMENTS, List.of(new ItemRequirement<>(injector.getInstance(RequirementRegistry.class).getType(MinLevelReqType.class), 10L)));
				//				params.put(GenerationParameters.MODEL_COLORS, List.of(Color.RED));

				injector.getInstance(ItemGenerator.class).update(SuperItemStack.safeInit(p.getInventory().getItemInMainHand()), params).printMessage().ifSuccess(i -> i.give(p));
			}
			case "load" -> {
				ItemInspector inspector = injector.getInstance(ItemInspector.class);
				ItemLoader loader = ItemLoader.of(p.getInventory().getItemInMainHand());
				ShardItem data = inspector.getItemData(loader);
				GenerationParameters params = inspector.getGenParams(loader);
				if (data == null || params == null) {
					return;
				}
				System.out.println(data.getId());
				System.out.println(injector.getInstance(ItemDataHandler.class).serializeParams(params));
				injector.getInstance(ItemGenerator.class).generateNew(data, params).printMessage().ifSuccess(i -> i.give(p));
			}
			case "profile" -> {
				injector.getInstance(PlayerSessionManager.class).loadProfile(p, Job.TEST1);
			}
			case "unload" -> {
				Task.on(() -> injector.getInstance(PlayerSessionManager.class).unloadProfile(p));
			}
			case "a" -> {
				PlayerStashContent content = injector.getInstance(PlayerStashManager.class).getStash(p.getUniqueId());
				if (content == null) {
					return;
				}
				content.getItems().forEach(i -> {
					System.out.println(i);
				});
			}
			case "b" -> {
				PlayerStashContent content = injector.getInstance(PlayerStashManager.class).getStash(p.getUniqueId());
				if (content == null) {
					return;
				}
				content.addItem(StashItemData.createNew(injector.getInstance(ItemRegistry.class).get(ObsidianChestplate.class), 4, 256));
			}
		}
	}

	@EventHandler
	private void onPlayerInteract(PlayerInteractEvent ev) {
		ItemStack item = ev.getItem();
		if (item == null) {
			return;
		}
		Player p = ev.getPlayer();
		System.out.println(item.getType());
		switch (item.getType()) {
			case BOW -> Task.on(() -> new BowAttack().attack(ev.getPlayer()));
			case COMPASS -> {
				MapView mapView = Bukkit.createMap(p.getWorld());
				for (MapRenderer renderer : mapView.getRenderers()) {
					mapView.removeRenderer(renderer);
				}
				mapView.addRenderer(new MapRenderer() {
					@Override
					public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
						for (int i = 0; i < 128; i++) {
							canvas.setPixelColor(i, i, Color.YELLOW);
						}
					}
				});
				ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
				MapMeta meta = (MapMeta) mapItem.getItemMeta();
				meta.setMapView(mapView);
				mapItem.setItemMeta(meta);

				SuperItemStack.init(mapItem).give(p);
			}
		}
	}

	@EventHandler
	private void onEntityDamageByEntity(EntityDamageByEntityEvent ev) {
		if (ev.getDamager() instanceof Arrow arrow) {
			if (arrow.getShooter() instanceof Player shooter) {
				shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 0.75f);
				DamageIndicator damageIndicator = new DamageIndicator((long) ev.getDamage());
				Random rand = new Random();
				damageIndicator.createIndicator(ev.getEntity().getLocation().add(rand.nextDouble(0.8d) - 0.4d, rand.nextDouble(0.5d) + 2.3, rand.nextDouble(0.8d) - 0.4d));
			}
		}
	}

	@EventHandler
	private void interact(PlayerInteractEvent ev) {
		if (ev.getPlayer().getInventory().getItemInMainHand().getType() == Material.STICK) {
			MenuFactory factory = injector.getInstance(MenuFactory.class);
			factory.create(MainMenu::new, ev.getPlayer()).open();
		}
	}

	@EventHandler
	private void interact(PlayerInteractEntityEvent ev) {
		if (ev.getRightClicked() instanceof Mob mob) {
			Bukkit.getLogger().info("=== Mob Goal Dump: " + mob.getType() + " ===");
			Bukkit.getMobGoals().getAllGoals(mob).forEach(goal -> {
				var key = goal.getKey();
				// 優先度やクラス名も一緒に出すと分かりやすい
				Bukkit.getLogger().info("Key: " + key + " | Class: " + goal.getClass().getSimpleName() + "   |    " + goal.getTypes());
			});
			Bukkit.getLogger().info("========================================");
		}
	}

	@EventHandler
	private void onPlayerStatsUpdate(PlayerStatsUpdateEvent ev) {
		Player p = ev.getPlayer();
		ItemGenerator generator = injector.getInstance(ItemGenerator.class);
		injector.getInstance(_PlayerCharacterManager.class).onCharacter(p, character -> {
			for (ItemStack item : p.getInventory().getContents()) {
				if (item == null) {
					continue;
				}
				generator.updateDynamicLore(character, item);
			}
		});
	}

	@EventHandler
	private void onShift(PlayerToggleSneakEvent ev) {
		if (ev.isSneaking()) {
			injector.getInstance(_PlayerCharacterManager.class).onCharacter(ev.getPlayer(), character -> {
				Bukkit.getPluginManager().callEvent(new PlayerStatsUpdateEvent(character));
			});
		}
		// プレイヤーのクラフトインベントリに配置する
		/*Player p = (Player) ev.getPlayer();
		p.getOpenInventory().getTopInventory().setItem(0, ItemStack.of(Material.WRITTEN_BOOK, 1));*/
	}

	@EventHandler
	private void onItemSpawn(ItemSpawnEvent ev) {
		// アイテムが消えるまでの時間
		/*int toRemoveItemSecond = 60;
		if (toRemoveItemSecond > 5 * 60) {
			throw new IllegalArgumentException();
		}
		ev.getEntity().setTicksLived((20 * 60 * 5) - toRemoveItemSecond * 20);*/

		// アイテムは1分で削除されるように、召喚時にはすでに4分経過したと指定する
		ev.getEntity().setTicksLived(20 * 60 * 4);
	}
}


