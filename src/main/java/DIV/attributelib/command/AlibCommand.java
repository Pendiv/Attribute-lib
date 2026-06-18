package DIV.attributelib.command;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.Condition;
import DIV.attributelib.api.Conditions;
import DIV.attributelib.api.DamageElements;
import DIV.attributelib.api.DamageLib;
import DIV.attributelib.api.ItemAttributes;
import DIV.attributelib.api.ItemModifier;
import DIV.attributelib.api.ModifierHandle;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.api.Vanilla;
import DIV.attributelib.api.VanillaAttributes;
import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.core.Modifier;
import DIV.attributelib.damage.DamageTypes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * デバッグコマンド。実機検証用で、権限は attributelib.debug(OP 既定)。
 *
 * <pre>
 * /alib list                                  登録済み属性の一覧
 * /alib dump                                  視線の先のエンティティ(いなければ自分)の属性状態
 * /alib setbase &lt;ns:id&gt; &lt;value&gt;
 * /alib add &lt;ns:id&gt; &lt;ADD|MULTIPLY|SET&gt; &lt;value&gt; [durationTicks]
 * /alib clear                                 このコマンドで付けたモディファイアを全除去
 * /alib smoke                                 自己診断(コンソール可。PDC往復・期限切れまで実機検証)
 * </pre>
 */
public final class AlibCommand implements CommandExecutor, TabCompleter {

    private static final String DEBUG_SOURCE = "attributelib:debug";
    private static final String SMOKE_SOURCE = "attributelib:smoke";
    private static final int TARGET_RANGE = 10;
    private static final double SMOKE_DEFAULT = 1.5;

    private final Plugin plugin;
    private final AttributeEngine engine;

    public AlibCommand(Plugin plugin, AttributeEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "dump" -> dump(sender);
            case "setbase" -> setBase(sender, args);
            case "add" -> add(sender, args);
            case "clear" -> clear(sender);
            case "conditions" -> conditions(sender);
            case "item" -> item(sender, args);
            case "itemclear" -> itemClear(sender);
            case "smoke" -> smoke(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void list(CommandSender sender) {
        var registered = engine.registered();
        if (registered.isEmpty()) {
            sender.sendMessage("登録済みの属性はありません");
            return;
        }
        sender.sendMessage("登録済み属性 (" + registered.size() + "件):");
        for (AttributeType type : registered) {
            StringBuilder line = new StringBuilder("  " + type.key() + "  default=" + type.defaultValue());
            if (type.min() != -Double.MAX_VALUE || type.max() != Double.MAX_VALUE) {
                line.append("  range=[").append(type.min()).append(", ").append(type.max()).append(']');
            }
            if (engine.isBridged(type.key())) {
                line.append("  [vanilla]");
            }
            sender.sendMessage(line.toString());
        }
    }

    private void dump(CommandSender sender) {
        LivingEntity target = resolveTarget(sender);
        if (target == null) {
            return;
        }
        sender.sendMessage("=== " + target.getType() + " " + target.getUniqueId() + " ===");
        Map<NamespacedKey, Double> base = engine.baseView(target);
        List<Modifier> modifiers = engine.modifierView(target);
        for (AttributeType type : engine.registered()) {
            boolean hasBase = base.containsKey(type.key());
            long modCount = modifiers.stream().filter(m -> m.attribute().equals(type.key())).count();
            if (!hasBase && modCount == 0) {
                continue;
            }
            sender.sendMessage("  " + type.key() + " = " + engine.get(target, type)
                    + " (base=" + engine.getBase(target, type) + (hasBase ? "" : " [default]")
                    + ", modifiers=" + modCount + ")");
        }
        if (modifiers.isEmpty()) {
            sender.sendMessage("  モディファイアなし");
            return;
        }
        long now = target.getWorld().getGameTime();
        for (Modifier m : modifiers) {
            sender.sendMessage("  - " + m.attribute() + " " + m.operation() + " " + m.value()
                    + " source=" + m.sourceId()
                    + (m.expiresAt() == Modifier.PERMANENT ? "" : " 残り" + (m.expiresAt() - now) + "tick")
                    + (m.persistent() ? "" : " [transient]"));
        }
    }

    private void setBase(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("使い方: /alib setbase <ns:id> <value>");
            return;
        }
        AttributeType type = parseType(sender, args[1]);
        Double value = parseDouble(sender, args[2]);
        LivingEntity target = resolveTarget(sender);
        if (type == null || value == null || target == null) {
            return;
        }
        engine.setBase(target, type, value);
        sender.sendMessage(type.key() + " の base を " + value + " に設定 → 最終値 " + engine.get(target, type));
    }

    private void add(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 6) {
            sender.sendMessage("使い方: /alib add <ns:id> <ADD|MULTIPLY|SET> <value> [durationTicks] [条件]");
            return;
        }
        AttributeType type = parseType(sender, args[1]);
        Operation operation;
        try {
            operation = Operation.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("演算は ADD / MULTIPLY / SET のいずれかです: " + args[2]);
            return;
        }
        Double value = parseDouble(sender, args[3]);
        long duration = 0;
        if (args.length >= 5) {
            try {
                duration = Long.parseLong(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("durationTicks が数値ではありません(無期限は 0): " + args[4]);
                return;
            }
            if (duration < 0) {
                sender.sendMessage("durationTicks は 0 以上が必要です: " + duration);
                return;
            }
        }
        Condition condition = null;
        if (args.length == 6) {
            condition = parseCondition(sender, args[5]);
            if (condition == null) {
                return;
            }
        }
        LivingEntity target = resolveTarget(sender);
        if (type == null || value == null || target == null) {
            return;
        }
        engine.add(target, type, DEBUG_SOURCE, operation, value, duration, true,
                condition != null ? condition.key() : null);
        sender.sendMessage(type.key() + " に " + operation + " " + value
                + (duration > 0 ? " (" + duration + "tick)" : "")
                + (condition != null ? " [条件: " + condition.key() + "]" : "")
                + " を付与 → 最終値 " + engine.get(target, type));
    }

    private void conditions(CommandSender sender) {
        var registered = engine.conditions().registered();
        sender.sendMessage("登録済み条件 (" + registered.size() + "件):");
        for (Condition condition : registered) {
            sender.sendMessage("  " + condition.key());
        }
    }

    private Condition parseCondition(CommandSender sender, String arg) {
        NamespacedKey key = NamespacedKey.fromString(arg);
        Condition condition = key != null ? engine.conditions().byKey(key) : null;
        if (condition == null) {
            sender.sendMessage("未登録の条件です: " + arg + " (/alib conditions で確認)");
        }
        return condition;
    }

    private void clear(CommandSender sender) {
        LivingEntity target = resolveTarget(sender);
        if (target == null) {
            return;
        }
        engine.removeBySource(target, DEBUG_SOURCE);
        sender.sendMessage("デバッグ由来のモディファイアを除去しました");
    }

    /**
     * 自己診断。一時的な ArmorStand を立てて、計算式・チケット・REPLACE・
     * PDC 書き込み→キャッシュ破棄→再読込・時限失効までを実機で順に検証する。
     * コンソールから実行できるため、CI 的なスモークテストとして使える。
     */
    private void smoke(CommandSender sender) {
        World world = Bukkit.getWorlds().getFirst();
        // プレイヤー不在環境ではスポーンチャンクのエンティティセクションがアンロードされて
        // 検証用エンティティがワールドから外れてしまうため、テスト中だけチケットで保持する
        world.getChunkAt(world.getSpawnLocation()).addPluginChunkTicket(plugin);
        ArmorStand stand = world.spawn(world.getSpawnLocation(), ArmorStand.class, s -> {
            s.setMarker(true);
            s.setInvisible(true);
            s.setPersistent(false);
        });
        List<String> failures = new ArrayList<>();
        try {
            AttributeType type = smokeType();

            check(failures, "初期値は default", engine.get(stand, type), SMOKE_DEFAULT);

            engine.setBase(stand, type, 10);
            check(failures, "setBase", engine.get(stand, type), 10);

            ModifierHandle addHandle = engine.add(stand, type, SMOKE_SOURCE, Operation.ADD, 5, 0, true);
            check(failures, "ADD +5", engine.get(stand, type), 15);

            engine.add(stand, type, SMOKE_SOURCE, Operation.MULTIPLY, 2, 0, true);
            check(failures, "MULTIPLY ×2", engine.get(stand, type), 30);

            ModifierHandle setHandle = engine.add(stand, type, SMOKE_SOURCE, Operation.SET, 99, 0, false);
            check(failures, "SET は全てに勝つ", engine.get(stand, type), 99);

            setHandle.remove();
            check(failures, "チケット remove", engine.get(stand, type), 30);

            // PDC 往復: キャッシュを捨てて PDC から再構築させる(persistent のみ生き残る)
            engine.evict(stand.getUniqueId());
            check(failures, "PDC 再読込後も persistent が生存", engine.get(stand, type), 30);

            engine.removeBySource(stand, SMOKE_SOURCE);
            check(failures, "removeAll(REPLACE 運用)", engine.get(stand, type), 10);

            // 条件付き(カスタム属性): 読み取りの瞬間に評価される
            smokeToggle = false;
            Condition toggle = smokeToggleCondition();
            ModifierHandle conditional = engine.add(stand, type, SMOKE_SOURCE,
                    Operation.ADD, 7, 0, true, toggle.key());
            check(failures, "条件不成立中は乗らない", engine.get(stand, type), 10);
            smokeToggle = true;
            check(failures, "条件成立で即時反映(読み取り時評価)", engine.get(stand, type), 17);
            smokeToggle = false;
            conditional.remove();

            engine.add(stand, type, SMOKE_SOURCE, Operation.ADD, 100, 1, true);
            check(failures, "時限 ADD +100 (1tick)", engine.get(stand, type), 110);

            smokeDamage(sender, failures);
        } catch (Exception e) {
            failures.add("例外発生: " + e);
            stand.remove();
            report(sender, failures);
            return;
        }

        // 装備同期の検証。装備変更の「検出」は entity tick 駆動でプレイヤー不在だと
        // 走らないため(実測 ticksLived=0)、ここでは合成イベントをバスに流して
        // リスナーの配線と同期ロジックを検証する(バニラ側の発火はゲーム内で確認可能)
        Zombie equipZombie = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
            z.setAI(false);
            z.setSilent(true);
            z.setPersistent(false);
        });
        try {
            ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
            ItemAttributes.add(sword, smokeType(), Operation.ADD, 50, EquipmentSlotGroup.MAINHAND);
            ItemAttributes.add(sword, smokeType(), Operation.ADD, 999, EquipmentSlotGroup.HEAD);

            // lore 自動更新(層4): 付与でセクション生成(2グループ = 6行)、clear で完全に消える
            List<net.kyori.adventure.text.Component> lore =
                    sword.hasItemMeta() ? sword.getItemMeta().lore() : null;
            check(failures, "lore セクション生成(空行+ヘッダ+行 ×2グループ)",
                    lore == null ? 0 : lore.size(), 6);
            ItemStack cleared = sword.clone();
            ItemAttributes.clear(cleared);
            if (cleared.hasItemMeta() && cleared.getItemMeta().lore() != null
                    && !cleared.getItemMeta().lore().isEmpty()) {
                failures.add("clear 後も lore が残っています");
            }

            equipZombie.getEquipment().setItemInMainHand(sword);
            callEquipChanged(equipZombie, EquipmentSlot.HAND, ItemStack.empty(), sword);
            check(failures, "装備同期: MAINHAND +50(HEAD 分は除外)",
                    engine.get(equipZombie, smokeType()), SMOKE_DEFAULT + 50);

            equipZombie.getEquipment().setItemInMainHand(null);
            callEquipChanged(equipZombie, EquipmentSlot.HAND, sword, ItemStack.empty());
            check(failures, "装備解除で消える(ゴースト強化なし)",
                    engine.get(equipZombie, smokeType()), SMOKE_DEFAULT);

            // バニラ属性のコンポーネント書き込みで基礎ステが消えないこと(base-wipe 検証、企画書 §8)
            ItemStack vanillaSword = ItemStack.of(Material.DIAMOND_SWORD);
            ItemAttributes.setVanilla(vanillaSword, Attribute.ATTACK_DAMAGE,
                    new NamespacedKey("attributelib", "smoke_vanilla"),
                    AttributeModifier.Operation.ADD_NUMBER, 3, EquipmentSlotGroup.MAINHAND);
            var component = vanillaSword.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            boolean hasBase = false;
            boolean hasOurs = false;
            if (component != null) {
                for (var entry : component.modifiers()) {
                    String key = entry.modifier().getKey().toString();
                    hasBase |= key.equals("minecraft:base_attack_damage");
                    hasOurs |= key.equals("attributelib:smoke_vanilla");
                }
            }
            if (!hasBase) {
                failures.add("バニラ属性付与で基礎攻撃力が消えた(base-wipe)");
            }
            if (!hasOurs) {
                failures.add("バニラ属性モディファイアが付与されていない");
            }
        } catch (Exception e) {
            failures.add("例外発生(装備検証): " + e);
        } finally {
            equipZombie.remove();
        }

        // バニラブリッジの検証(別ゾンビ。フェーズ2で時限失効も見る)
        Zombie bridgeZombie = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
            z.setAI(false);
            z.setSilent(true);
            z.setPersistent(false);
        });
        try {
            double baseMax = bridgeZombie.getAttribute(Attribute.MAX_HEALTH).getValue();
            engine.add(bridgeZombie, Vanilla.MAX_HEALTH, SMOKE_SOURCE, Operation.ADD, 10, 0, true);
            check(failures, "ブリッジ: max_health +10 がバニラ値に反映",
                    bridgeZombie.getAttribute(Attribute.MAX_HEALTH).getValue(), baseMax + 10);

            // 再ロード相当: キャッシュ破棄+バニラモディファイア喪失(transient のアンロード)を
            // 模擬し、状態の再構築で永続データから復元されること
            engine.evict(bridgeZombie.getUniqueId());
            VanillaAttributes.remove(bridgeZombie, Attribute.MAX_HEALTH, AttributeEngine.BRIDGE_KEY);
            engine.touch(bridgeZombie);
            check(failures, "ブリッジ: 再ロード後も永続データから復元",
                    bridgeZombie.getAttribute(Attribute.MAX_HEALTH).getValue(), baseMax + 10);

            // 時限ブリッジ(1tick): フェーズ2で deadline タスクによる自動復帰を確認
            double baseArmor = bridgeZombie.getAttribute(Attribute.ARMOR).getValue();
            engine.add(bridgeZombie, Vanilla.ARMOR, SMOKE_SOURCE, Operation.ADD, 5, 1, true);
            check(failures, "ブリッジ: 時限 armor +5", bridgeZombie.getAttribute(Attribute.ARMOR).getValue(), baseArmor + 5);

            // 条件付きブリッジ: 条件反転は周期タスクが反映する(ここでは直接呼んで決定論的に検証)
            smokeToggle = true;
            engine.add(bridgeZombie, Vanilla.SCALE, SMOKE_SOURCE, Operation.ADD, 1, 0, true,
                    smokeToggleCondition().key());
            check(failures, "条件付きブリッジ: 成立中は反映",
                    bridgeZombie.getAttribute(Attribute.SCALE).getValue(), 2);
            smokeToggle = false;
            engine.refreshConditionalBridges();
            check(failures, "条件付きブリッジ: 不成立で書き戻し",
                    bridgeZombie.getAttribute(Attribute.SCALE).getValue(), 1);

            // エフェクト条件: 保持しているエフェクトだけ成立する
            bridgeZombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
            if (!engine.conditions().isActive(Conditions.hasEffect(PotionEffectType.SPEED).key(), bridgeZombie)) {
                failures.add("エフェクト条件: SPEED 保持中なのに不成立");
            }
            if (engine.conditions().isActive(Conditions.hasEffect(PotionEffectType.POISON).key(), bridgeZombie)) {
                failures.add("エフェクト条件: 未保持の POISON が成立");
            }

            // AND 合成: 全成立のときだけ成立する
            Condition and = smokeAndCondition();
            smokeToggle = true;
            if (!engine.conditions().isActive(and.key(), bridgeZombie)) {
                failures.add("AND 合成: 両条件成立なのに不成立");
            }
            smokeToggle = false;
            if (engine.conditions().isActive(and.key(), bridgeZombie)) {
                failures.add("AND 合成: 片側不成立なのに成立");
            }
        } catch (Exception e) {
            failures.add("例外発生(ブリッジ検証): " + e);
        }

        // フェーズ2(2tick 後): 時限モディファイアが gameTime 進行で自動失効していること
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                AttributeType type = smokeType();
                check(failures, "時限失効(遅延判定)", engine.get(stand, type), 10);

                engine.removeBySource(stand, SMOKE_SOURCE);
                engine.resetBase(stand, type);
                check(failures, "resetBase で default に復帰", engine.get(stand, type), SMOKE_DEFAULT);

            } catch (Exception e) {
                failures.add("例外発生(フェーズ2): " + e);
            } finally {
                stand.remove();
            }

            // フェーズ3(さらに 4tick 後): ブリッジの時限失効。
            // プレイヤー不在環境ではエンティティのワールド除去→再追加で deadline タスクが
            // キャンセル→再武装されるため(自己修復)、発火に余裕を持たせて検証する
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    check(failures, "ブリッジ: 時限 armor が deadline タスクで失効",
                            bridgeZombie.getAttribute(Attribute.ARMOR).getValue(), 2);
                    engine.removeBySource(bridgeZombie, SMOKE_SOURCE);
                    check(failures, "ブリッジ: removeAll で復元",
                            bridgeZombie.getAttribute(Attribute.MAX_HEALTH).getValue(), 20);
                } catch (Exception e) {
                    failures.add("例外発生(フェーズ3): " + e);
                } finally {
                    bridgeZombie.remove();
                    world.getChunkAt(world.getSpawnLocation()).removePluginChunkTicket(plugin);
                }
                report(sender, failures);
            }, 4L);
        }, 2L);
    }

    /** 合成の装備変更イベントをバスに流す(smoke 用。プレイヤー不在では tick 検出が走らないため)。 */
    private void callEquipChanged(LivingEntity entity, EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        EntityEquipmentChangedEvent.EquipmentChange change = new EntityEquipmentChangedEvent.EquipmentChange() {
            @Override
            public ItemStack oldItem() {
                return oldItem;
            }

            @Override
            public ItemStack newItem() {
                return newItem;
            }
        };
        Bukkit.getPluginManager().callEvent(new EntityEquipmentChangedEvent(entity, Map.of(slot, change)));
    }

    /**
     * 手持ちアイテムのカスタム属性を表示/追加する。
     * {@code /alib item} で表示、{@code /alib item <ns:id> <op> <value> [slotGroup]} で追加。
     */
    private void item(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty()) {
            sender.sendMessage("メインハンドにアイテムを持ってください");
            return;
        }
        if (args.length == 1) {
            List<ItemModifier> modifiers = ItemAttributes.get(held);
            if (modifiers.isEmpty()) {
                sender.sendMessage("このアイテムにカスタム属性はありません");
                return;
            }
            sender.sendMessage(held.getType() + " のカスタム属性 (" + modifiers.size() + "件):");
            for (ItemModifier m : modifiers) {
                sender.sendMessage("  - " + m.attribute() + " " + m.operation() + " " + m.value()
                        + " [" + m.slot() + "]");
            }
            return;
        }
        if (args.length != 4 && args.length != 5) {
            sender.sendMessage("使い方: /alib item [<ns:id> <ADD|MULTIPLY|SET> <value> [slotGroup]]");
            return;
        }
        AttributeType type = parseType(sender, args[1]);
        Operation operation;
        try {
            operation = Operation.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("演算は ADD / MULTIPLY / SET のいずれかです: " + args[2]);
            return;
        }
        Double value = parseDouble(sender, args[3]);
        EquipmentSlotGroup slot = EquipmentSlotGroup.MAINHAND;
        if (args.length == 5) {
            slot = EquipmentSlotGroup.getByName(args[4].toLowerCase(Locale.ROOT));
            if (slot == null) {
                sender.sendMessage("不明なスロットグループです: " + args[4]
                        + " (mainhand/offhand/hand/head/chest/legs/feet/armor/body/any)");
                return;
            }
        }
        if (type == null || value == null) {
            return;
        }
        ItemAttributes.add(held, type, operation, value, slot);
        player.getInventory().setItemInMainHand(held);
        sender.sendMessage(held.getType() + " に " + type.key() + " " + operation + " " + value
                + " [" + slot + "] を付与しました(持ち替えると反映)");
    }

    private void itemClear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.isEmpty()) {
            sender.sendMessage("メインハンドにアイテムを持ってください");
            return;
        }
        ItemAttributes.clear(held);
        player.getInventory().setItemInMainHand(held);
        sender.sendMessage("カスタム属性を全て除去しました");
    }

    /**
     * 層2(ダメージパイプライン)の実機検証。AI 無効のゾンビに DamageLib で実ダメージを与え、
     * 標準属性が期待通り効くことを体力差分で確認する。
     */
    private void smokeDamage(CommandSender sender, List<String> failures) {
        if (!DamageTypes.missingTypes().isEmpty()) {
            sender.sendMessage("[attributelib] smoke: ダメージタイプ未登録のためダメージ検証をスキップ(再起動後に再実行)");
            return;
        }
        World world = Bukkit.getWorlds().getFirst();
        Zombie zombie = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
            z.setAI(false);
            z.setSilent(true);
            z.setPersistent(false);
            z.setRemoveWhenFarAway(false);
        });
        try {
            double health = zombie.getHealth();

            // magic は防具(ゾンビの armor 2)を素通りするため入力値がそのまま base になる
            DamageLib.magic(null, zombie, 4);
            check(failures, "magic 4 ダメージ", health - zombie.getHealth(), 4);
            health = zombie.getHealth();

            zombie.setNoDamageTicks(0);
            engine.setBase(zombie, StandardAttributes.MAGIC_RESIST, 0.75);
            DamageLib.magic(null, zombie, 4);
            check(failures, "magic_resist 0.75 で 4 → 1", health - zombie.getHealth(), 1);
            health = zombie.getHealth();

            zombie.setNoDamageTicks(0);
            engine.setBase(zombie, StandardAttributes.DAMAGE_TAKEN, 0.5);
            DamageLib.trueDamage(null, zombie, 2);
            check(failures, "確定ダメージは damage_taken を無視", health - zombie.getHealth(), 2);
            health = zombie.getHealth();

            zombie.setNoDamageTicks(0);
            DamageLib.magic(null, zombie, 4);
            check(failures, "magic_resist 0.75 × damage_taken 0.5 で 4 → 0.5",
                    health - zombie.getHealth(), 0.5);

            // 元素宣誓は防具を尊重するようになったため、正確な期待値で検証する前に armor を 0 にする
            org.bukkit.attribute.AttributeInstance zombieArmor = zombie.getAttribute(Attribute.ARMOR);
            if (zombieArmor != null) {
                zombieArmor.setBaseValue(0);
            }

            // 元素: 宣誓ダメージ(fire_resist 0.25 と既設定の damage_taken 0.5 が両方効く)
            zombie.setNoDamageTicks(0);
            engine.setBase(zombie, StandardAttributes.FIRE_RESIST, 0.25);
            health = zombie.getHealth();
            DamageLib.deal(DamageElements.FIRE, null, zombie, 4);
            check(failures, "fire 宣誓 4 → 1.5 (fire_resist0.25 × damage_taken0.5)",
                    health - zombie.getHealth(), 1.5);

            // 元素: バニラ雷タイプのマッピング
            zombie.setNoDamageTicks(0);
            engine.setBase(zombie, StandardAttributes.LIGHTNING_RESIST, 0.5);
            health = zombie.getHealth();
            DamageType lightningType = DamageTypes.resolve(NamespacedKey.minecraft("lightning_bolt"));
            zombie.damage(4, DamageSource.builder(lightningType).build());
            check(failures, "バニラ雷 4 → 1.0 (lightning_resist0.5 × damage_taken0.5)",
                    health - zombie.getHealth(), 1.0);

            // 貫通100%: armor20 の相手への物理攻撃が「裸と同じ最終値」に着地する
            // (防具持ちの方がダメージが出る逆転バグの再発検知)
            Zombie attacker = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
                z.setAI(false);
                z.setSilent(true);
                z.setPersistent(false);
            });
            try {
                engine.setBase(attacker, StandardAttributes.ARMOR_PENETRATION, 1);
                org.bukkit.attribute.AttributeInstance victimArmor = zombie.getAttribute(Attribute.ARMOR);
                if (victimArmor != null) {
                    victimArmor.setBaseValue(20);
                }
                engine.resetBase(zombie, StandardAttributes.DAMAGE_TAKEN);
                zombie.setNoDamageTicks(0);
                health = zombie.getHealth();
                DamageType mobAttack = DamageTypes.resolve(NamespacedKey.minecraft("mob_attack"));
                zombie.damage(4, DamageSource.builder(mobAttack)
                        .withCausingEntity(attacker).withDirectEntity(attacker).build());
                checkApprox(failures, "貫通100%: armor20 でも裸と同値(4)", health - zombie.getHealth(), 4, 0.05);
                if (victimArmor != null) {
                    victimArmor.setBaseValue(2);
                }
                engine.setBase(zombie, StandardAttributes.DAMAGE_TAKEN, 0.5);
            } finally {
                attacker.remove();
            }

            // 元素宣誓が防具を尊重 / dealPiercing は防具を完全素通り(新挙動の検証)。
            // 専用の被害者ゾンビ(満タン)を立て、HP 枯渇や状態漏れを避ける。
            Zombie armoredVictim = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
                z.setAI(false);
                z.setSilent(true);
                z.setPersistent(false);
                z.setRemoveWhenFarAway(false);
            });
            Zombie elemAttacker = world.spawn(world.getSpawnLocation(), Zombie.class, z -> {
                z.setAI(false);
                z.setSilent(true);
                z.setPersistent(false);
            });
            try {
                org.bukkit.attribute.AttributeInstance vArmor = armoredVictim.getAttribute(Attribute.ARMOR);
                if (vArmor != null) {
                    vArmor.setBaseValue(20);
                }
                double vHealth;

                // (1) deal は防具を尊重: armor20 で 4 が削れ、裸の 4 未満になる
                armoredVictim.setNoDamageTicks(0);
                vHealth = armoredVictim.getHealth();
                DamageLib.deal(DamageElements.FIRE, elemAttacker, armoredVictim, 4);
                double dealLoss = vHealth - armoredVictim.getHealth();
                if (!(dealLoss < 4 - 1e-6)) {
                    failures.add("deal は防具を尊重するはず: armor20 で 4 が削れていない (loss=" + dealLoss + ")");
                }

                // (2) dealPiercing は防具を完全素通り: armor20 でも裸と同値(4)
                armoredVictim.setNoDamageTicks(0);
                vHealth = armoredVictim.getHealth();
                DamageLib.dealPiercing(DamageElements.FIRE, elemAttacker, armoredVictim, 4);
                checkApprox(failures, "dealPiercing: armor20 でも裸と同値(4)",
                        vHealth - armoredVictim.getHealth(), 4, 0.05);

                // (3) ARMOR_PENETRATION 100% なら deal も裸と同値に戻る
                engine.setBase(elemAttacker, StandardAttributes.ARMOR_PENETRATION, 1);
                armoredVictim.setNoDamageTicks(0);
                vHealth = armoredVictim.getHealth();
                DamageLib.deal(DamageElements.FIRE, elemAttacker, armoredVictim, 4);
                checkApprox(failures, "deal + 貫通100%: armor20 でも裸と同値(4)",
                        vHealth - armoredVictim.getHealth(), 4, 0.05);
            } finally {
                armoredVictim.remove();
                elemAttacker.remove();
            }

            // heal_multiplier はバニラ回復経路(EntityRegainHealthEvent)に効く。
            // Paper の heal() API はイベントを発火しないため(実機確認済み)、
            // ここではイベントを直接発火してリスナーの補正を検証する
            engine.setBase(zombie, StandardAttributes.HEAL_MULTIPLIER, 0.5);
            EntityRegainHealthEvent regain = new EntityRegainHealthEvent(
                    zombie, 4, EntityRegainHealthEvent.RegainReason.MAGIC);
            Bukkit.getPluginManager().callEvent(regain);
            check(failures, "heal_multiplier 0.5 で +4 → +2", regain.getAmount(), 2);

            engine.setBase(zombie, StandardAttributes.HEAL_MULTIPLIER, 0);
            EntityRegainHealthEvent blocked = new EntityRegainHealthEvent(
                    zombie, 4, EntityRegainHealthEvent.RegainReason.MAGIC);
            Bukkit.getPluginManager().callEvent(blocked);
            if (!blocked.isCancelled()) {
                failures.add("heal_multiplier 0 で回復イベントがキャンセルされていません");
            }
        } catch (Exception e) {
            failures.add("例外発生(ダメージ検証): " + e);
        } finally {
            zombie.remove();
        }
    }

    /** smoke 用のトグル条件(決定論的に成立/不成立を切り替えて検証する)。 */
    private static boolean smokeToggle;

    private Condition smokeToggleCondition() {
        NamespacedKey key = new NamespacedKey("attributelib", "smoke_toggle");
        Condition existing = engine.conditions().byKey(key);
        if (existing != null) {
            return existing;
        }
        return engine.conditions().register(plugin, "smoke_toggle",
                net.kyori.adventure.text.Component.text("トグル"), entity -> smokeToggle);
    }

    /** smoke 用 AND 合成条件(トグル × SPEED エフェクト)。二重登録ガード付き。 */
    private Condition smokeAndCondition() {
        NamespacedKey key = new NamespacedKey("attributelib", "smoke_and");
        Condition existing = engine.conditions().byKey(key);
        if (existing != null) {
            return existing;
        }
        return Conditions.allOf(plugin, "smoke_and",
                smokeToggleCondition(), Conditions.hasEffect(PotionEffectType.SPEED));
    }

    private AttributeType smokeType() {
        NamespacedKey key = new NamespacedKey(plugin, "smoke_test");
        AttributeType existing = engine.byKey(key);
        return existing != null ? existing
                : engine.register(plugin, "smoke_test", SMOKE_DEFAULT, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private void check(List<String> failures, String label, double actual, double expected) {
        if (actual != expected) {
            failures.add(label + ": 期待 " + expected + " / 実際 " + actual);
        }
    }

    /** NMS の float 演算を経由する値の検証用(許容誤差付き)。 */
    private void checkApprox(List<String> failures, String label, double actual, double expected, double epsilon) {
        if (Math.abs(actual - expected) > epsilon) {
            failures.add(label + ": 期待 " + expected + "±" + epsilon + " / 実際 " + actual);
        }
    }

    private void report(CommandSender sender, List<String> failures) {
        if (failures.isEmpty()) {
            sender.sendMessage("[attributelib] smoke: 全チェック OK");
            return;
        }
        sender.sendMessage("[attributelib] smoke: " + failures.size() + " 件失敗");
        failures.forEach(f -> sender.sendMessage("  NG " + f));
    }

    private LivingEntity resolveTarget(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return null;
        }
        Entity looked = player.getTargetEntity(TARGET_RANGE);
        return looked instanceof LivingEntity living ? living : player;
    }

    private AttributeType parseType(CommandSender sender, String arg) {
        NamespacedKey key = NamespacedKey.fromString(arg);
        AttributeType type = key != null ? engine.byKey(key) : null;
        if (type == null) {
            sender.sendMessage("未登録の属性です: " + arg + " (/alib list で確認)");
        }
        return type;
    }

    private Double parseDouble(CommandSender sender, String arg) {
        try {
            double value = Double.parseDouble(arg);
            if (!Double.isFinite(value)) {
                sender.sendMessage("有限値を指定してください: " + arg);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            sender.sendMessage("数値ではありません: " + arg);
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "dump", "setbase", "add", "clear", "conditions",
                    "item", "itemclear", "smoke"), args[0]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("item")) {
            return filter(List.of("mainhand", "offhand", "hand", "head", "chest", "legs", "feet",
                    "armor", "body", "any"), args[4]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setbase") || args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("item"))) {
            List<String> keys = new ArrayList<>();
            for (AttributeType type : engine.registered()) {
                keys.add(type.key().toString());
            }
            return filter(keys, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("item"))) {
            return filter(List.of("ADD", "MULTIPLY", "SET"), args[2]);
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("add")) {
            List<String> keys = new ArrayList<>();
            for (Condition condition : engine.conditions().registered()) {
                keys.add(condition.key().toString());
            }
            return filter(keys, args[5]);
        }
        return List.of();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
