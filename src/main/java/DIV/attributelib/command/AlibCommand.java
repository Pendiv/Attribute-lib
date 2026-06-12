package DIV.attributelib.command;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.DamageElements;
import DIV.attributelib.api.DamageLib;
import DIV.attributelib.api.ModifierHandle;
import DIV.attributelib.api.Operation;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.core.Modifier;
import DIV.attributelib.damage.DamageTypes;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
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
        if (args.length != 4 && args.length != 5) {
            sender.sendMessage("使い方: /alib add <ns:id> <ADD|MULTIPLY|SET> <value> [durationTicks]");
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
        if (args.length == 5) {
            try {
                duration = Long.parseLong(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("durationTicks が数値ではありません: " + args[4]);
                return;
            }
            if (duration <= 0) {
                sender.sendMessage("durationTicks は正の値が必要です: " + duration);
                return;
            }
        }
        LivingEntity target = resolveTarget(sender);
        if (type == null || value == null || target == null) {
            return;
        }
        engine.add(target, type, DEBUG_SOURCE, operation, value, duration, true);
        sender.sendMessage(type.key() + " に " + operation + " " + value
                + (duration > 0 ? " (" + duration + "tick)" : "") + " を付与 → 最終値 " + engine.get(target, type));
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

            engine.add(stand, type, SMOKE_SOURCE, Operation.ADD, 100, 1, true);
            check(failures, "時限 ADD +100 (1tick)", engine.get(stand, type), 110);

            smokeDamage(sender, failures);
        } catch (Exception e) {
            failures.add("例外発生: " + e);
            stand.remove();
            report(sender, failures);
            return;
        }

        // 2tick 後: 時限モディファイアが gameTime 進行で自動失効していること
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                AttributeType type = smokeType();
                check(failures, "時限失効(遅延判定)", engine.get(stand, type), 10);

                engine.removeBySource(stand, SMOKE_SOURCE);
                engine.resetBase(stand, type);
                check(failures, "resetBase で default に復帰", engine.get(stand, type), SMOKE_DEFAULT);
            } catch (Exception e) {
                failures.add("例外発生(遅延フェーズ): " + e);
            } finally {
                stand.remove();
            }
            report(sender, failures);
        }, 2L);
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

            // 元素: 宣誓ダメージ(fire_resist 0.25 と既設定の damage_taken 0.5 が両方効く)
            zombie.setNoDamageTicks(0);
            engine.setBase(zombie, StandardAttributes.FIRE_RESIST, 0.25);
            health = zombie.getHealth();
            DamageLib.deal(DamageElements.FIRE, null, zombie, 4);
            check(failures, "fire 宣誓 4 → 1.5 (fire_resist0.25 × damage_taken0.5)",
                    health - zombie.getHealth(), 1.5);

            // 元素: バニラ雷タイプのマッピング(armor を 0 にして正確な値で検証)
            org.bukkit.attribute.AttributeInstance zombieArmor = zombie.getAttribute(Attribute.ARMOR);
            if (zombieArmor != null) {
                zombieArmor.setBaseValue(0);
            }
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
            return filter(List.of("list", "dump", "setbase", "add", "clear", "smoke"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setbase") || args[0].equalsIgnoreCase("add"))) {
            List<String> keys = new ArrayList<>();
            for (AttributeType type : engine.registered()) {
                keys.add(type.key().toString());
            }
            return filter(keys, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return filter(List.of("ADD", "MULTIPLY", "SET"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
