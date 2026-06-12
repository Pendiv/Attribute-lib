package DIV.attributelib.sideboard;

import DIV.attributelib.api.AttributeType;
import DIV.attributelib.api.Sideboard;
import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.api.Vanilla;
import DIV.attributelib.core.AttributeEngine;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ステータスサイドバーの中枢。ジャンルのレジストリ、プレイヤーごとの選択(PDC 永続)、
 * 描画の周期更新(1秒、表示中のプレイヤーがいる間だけ稼働)を持つ。
 *
 * <p>描画は Paper のスコアボード API(行ごとの customName + 数字非表示)で行い、
 * 外部プラグインに依存しない。サイドバーの上限15行を超えた分は表示されない
 * (まず区切りの空行を諦め、それでも超える分は末尾から切る)。</p>
 */
public final class SideboardManager implements Listener {

    private static final int MAX_LINES = 15;
    /** タスク粒度(=高速行の最短更新間隔、0.1秒)。 */
    private static final long BASE_PERIOD = 2L;
    /** 全体再描画の間隔(構成変更・通常行の更新)。 */
    private static final long FULL_REFRESH = 20L;
    public static final int MAX_GENRES = 3;

    private static final DecimalFormat FORMAT =
            new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final Plugin plugin;
    private final AttributeEngine engine;
    private final NamespacedKey selectionKey;
    private final NamespacedKey hiddenKey = new NamespacedKey("attributelib", "sideboard_hidden");
    private final Map<String, Genre> genres = new LinkedHashMap<>();
    private final Map<UUID, PlayerBoard> active = new HashMap<>();
    private BukkitTask refreshTask;

    /** lines は可変(外部プラグインが行を追加・削除できる)。 */
    private record Genre(String id, Component title, List<Sideboard.Line> lines) {
    }

    private static final class PlayerBoard {
        final Player player;
        final List<String> genreIds;
        final Scoreboard board;
        final Objective objective;
        /** 表示行 index → 値行(見出し・空行は null)。全体再描画時に更新される。 */
        List<Sideboard.Line> slots = List.of();
        /** 高速更新行(refreshTicks < FULL_REFRESH)を表示中か。 */
        boolean hasFastLines;

        PlayerBoard(Player player, List<String> genreIds, Scoreboard board, Objective objective) {
            this.player = player;
            this.genreIds = genreIds;
            this.board = board;
            this.objective = objective;
        }
    }

    /** 標準ジャンルがブリッジ属性を参照するため、registerBridges 後に生成すること。 */
    public SideboardManager(Plugin plugin, AttributeEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
        this.selectionKey = new NamespacedKey("attributelib", "sideboard");
        registerStandardGenres();
    }

    // ---- ジャンルレジストリ ----

    public void registerGenre(Plugin owner, String id, Component title, List<Sideboard.Line> lines) {
        AttributeEngine.checkMainThread();
        String key = id.toLowerCase(Locale.ROOT);
        if (genres.putIfAbsent(key, new Genre(key, title, new ArrayList<>(lines))) != null) {
            throw new IllegalStateException("サイドバージャンルが二重登録されました: " + key);
        }
    }

    /** 既存ジャンルの末尾に行を追加する。 */
    public void addLine(String genreId, Sideboard.Line line) {
        AttributeEngine.checkMainThread();
        Genre genre = genres.get(genreId.toLowerCase(Locale.ROOT));
        if (genre == null) {
            throw new IllegalArgumentException("ジャンルが存在しません: " + genreId
                    + " (登録済み: " + String.join(", ", genres.keySet()) + ")");
        }
        genre.lines().add(line);
    }

    /** 行 ID 指定で既存ジャンルから行を取り除く。 */
    public boolean removeLine(String genreId, String lineId) {
        AttributeEngine.checkMainThread();
        Genre genre = genres.get(genreId.toLowerCase(Locale.ROOT));
        if (genre == null) {
            return false;
        }
        return genre.lines().removeIf(line -> lineId.equals(line.id()));
    }

    public Collection<String> genreIds() {
        return Collections.unmodifiableCollection(genres.keySet());
    }

    public boolean hasGenre(String id) {
        return genres.containsKey(id.toLowerCase(Locale.ROOT));
    }

    // ---- 表示制御 ----

    /** 選択を適用して表示を開始する(PDC に保存)。 */
    public void show(Player player, List<String> genreIds) {
        AttributeEngine.checkMainThread();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("attributelib_side", Criteria.DUMMY,
                Component.text("ステータス", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        PlayerBoard playerBoard = new PlayerBoard(player, List.copyOf(genreIds), board, objective);
        active.put(player.getUniqueId(), playerBoard);
        player.getPersistentDataContainer().set(selectionKey, PersistentDataType.STRING,
                String.join(",", genreIds));
        player.getPersistentDataContainer().remove(hiddenKey);
        player.setScoreboard(board);
        render(playerBoard);

        if (refreshTask == null) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll,
                    BASE_PERIOD, BASE_PERIOD);
        }
    }

    /**
     * 表示を消す。選択は PDC に残るので {@link #showSaved} (/sideboard on) で復帰できる。
     */
    public void hide(Player player) {
        AttributeEngine.checkMainThread();
        active.remove(player.getUniqueId());
        player.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) 1);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 保存済みの選択で表示を再開する(/sideboard on)。
     *
     * @return 保存済み選択が無ければ false
     */
    public boolean showSaved(Player player) {
        List<String> saved = savedSelection(player);
        if (saved.isEmpty()) {
            return false;
        }
        show(player, saved);
        return true;
    }

    /** 現在表示中か。 */
    public boolean isShowing(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    /** PDC に保存された選択(表示状態は問わない)。 */
    public List<String> savedSelection(Player player) {
        String saved = player.getPersistentDataContainer().get(selectionKey, PersistentDataType.STRING);
        if (saved == null || saved.isEmpty()) {
            return List.of();
        }
        return List.of(saved.split(",")).stream().filter(this::hasGenre).toList();
    }

    // ---- 描画 ----

    /** タスクカウンタ(BASE_PERIOD ずつ進む)。 */
    private long tick;

    private void refreshAll() {
        if (active.isEmpty()) {
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
            return;
        }
        tick += BASE_PERIOD;
        boolean full = tick % FULL_REFRESH == 0;
        for (PlayerBoard board : List.copyOf(active.values())) {
            if (!board.player.isOnline()) {
                continue;
            }
            if (full) {
                render(board); // 全体再描画(構成変更・通常行)
            } else if (board.hasFastLines) {
                renderFastLines(board); // 高速行のみ部分更新
            }
        }
    }

    /** 全体再描画。スロット表(高速行の部分更新用)もここで作り直す。 */
    private void render(PlayerBoard playerBoard) {
        List<Slot> slots = buildSlots(playerBoard.player, playerBoard.genreIds);
        Objective objective = playerBoard.objective;
        List<Sideboard.Line> slotLines = new ArrayList<>(slots.size());
        boolean fast = false;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            Score score = objective.getScore("line" + i);
            score.setScore(slots.size() - i);
            score.customName(slot.component());
            slotLines.add(slot.line());
            fast |= slot.line() != null && slot.line().refreshTicks() < FULL_REFRESH;
        }
        // 行数が減った場合の残骸を消す
        for (int i = slots.size(); i <= MAX_LINES; i++) {
            playerBoard.board.resetScores("line" + i);
        }
        playerBoard.slots = slotLines;
        playerBoard.hasFastLines = fast;
    }

    /** 高速行だけを、その行の更新間隔に従って差し替える。 */
    private void renderFastLines(PlayerBoard playerBoard) {
        for (int i = 0; i < playerBoard.slots.size(); i++) {
            Sideboard.Line line = playerBoard.slots.get(i);
            if (line == null || line.refreshTicks() >= FULL_REFRESH || tick % line.refreshTicks() != 0) {
                continue;
            }
            playerBoard.objective.getScore("line" + i)
                    .customName(lineComponent(line, playerBoard.player));
        }
    }

    private record Slot(Component component, Sideboard.Line line) {
    }

    private List<Slot> buildSlots(Player player, List<String> genreIds) {
        List<Slot> slots = buildSlots(player, genreIds, true);
        if (slots.size() > MAX_LINES) {
            slots = buildSlots(player, genreIds, false); // まず空行区切りを諦める
        }
        return slots.size() > MAX_LINES ? slots.subList(0, MAX_LINES) : slots;
    }

    private List<Slot> buildSlots(Player player, List<String> genreIds, boolean separators) {
        List<Slot> slots = new ArrayList<>();
        for (String id : genreIds) {
            Genre genre = genres.get(id);
            if (genre == null) {
                continue; // 提供元プラグインが抜けたジャンルはスキップ
            }
            if (separators && !slots.isEmpty()) {
                slots.add(new Slot(Component.empty(), null));
            }
            slots.add(new Slot(genre.title(), null));
            for (Sideboard.Line line : genre.lines()) {
                slots.add(new Slot(lineComponent(line, player), line));
            }
        }
        return slots;
    }

    private Component lineComponent(Sideboard.Line line, Player player) {
        Component value;
        try {
            value = line.value().apply(player);
        } catch (Exception e) {
            value = Component.text("?", NamedTextColor.DARK_GRAY);
        }
        return Component.text("* ", NamedTextColor.DARK_GRAY)
                .append(line.label())
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(value.colorIfAbsent(NamedTextColor.WHITE));
    }

    // ---- ライフサイクル ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(hiddenKey)) {
            return; // off のままログアウトした人は復元しない
        }
        List<String> saved = savedSelection(player);
        if (!saved.isEmpty()) {
            show(player, saved);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

    // ---- 標準ジャンル ----

    private void registerStandardGenres() {
        registerGenre(plugin, "combat", Component.text("戦闘系:", NamedTextColor.GOLD), List.of(
                // 攻撃力と攻撃速度を1行に圧縮(15行予算の節約)
                new Sideboard.Line("attack", Component.text("攻撃力", NamedTextColor.YELLOW), player ->
                        Component.text(formatAttribute(Vanilla.ATTACK_DAMAGE, engine.get(player, Vanilla.ATTACK_DAMAGE))
                                + " (攻速 "
                                + formatAttribute(Vanilla.ATTACK_SPEED, engine.get(player, Vanilla.ATTACK_SPEED))
                                + ")")),
                attrLine("crit_chance", "会心率", NamedTextColor.YELLOW, StandardAttributes.CRIT_CHANCE),
                attrLine("crit_damage", "会心ダメージ", NamedTextColor.YELLOW, StandardAttributes.CRIT_DAMAGE),
                attrLine("damage_dealt", "与ダメージ", NamedTextColor.YELLOW, StandardAttributes.DAMAGE_DEALT),
                attrLine("damage_taken", "被ダメージ", NamedTextColor.YELLOW, StandardAttributes.DAMAGE_TAKEN)));

        registerGenre(plugin, "resist", Component.text("耐性:", NamedTextColor.GOLD), List.of(
                attrLine("physical", "物理", NamedTextColor.AQUA, StandardAttributes.PHYSICAL_RESIST),
                attrLine("fire", "炎", NamedTextColor.AQUA, StandardAttributes.FIRE_RESIST),
                attrLine("lightning", "雷", NamedTextColor.AQUA, StandardAttributes.LIGHTNING_RESIST)));

        registerGenre(plugin, "status", Component.text("ステータス:", NamedTextColor.GOLD), List.of(
                new Sideboard.Line("hp", Component.text("HP", NamedTextColor.GREEN), player ->
                        Component.text(FORMAT.format(player.getHealth()) + "/"
                                + FORMAT.format(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()))),
                attrLine("armor", "防御力", NamedTextColor.GREEN, Vanilla.ARMOR),
                attrLine("armor_toughness", "防具強度", NamedTextColor.GREEN, Vanilla.ARMOR_TOUGHNESS),
                // 高速更新(0.1秒)の実例。不要なら removeLine("status", "position") で消せる
                new Sideboard.Line("position", Component.text("座標", NamedTextColor.GREEN), player -> {
                    var location = player.getLocation();
                    return Component.text(location.getBlockX() + ", " + location.getBlockY()
                            + ", " + location.getBlockZ());
                }, 2)));

        registerGenre(plugin, "user", Component.text("ユーザー情報:", NamedTextColor.GOLD), List.of(
                new Sideboard.Line("mob_kills", Component.text("討伐数", NamedTextColor.WHITE), player ->
                        Component.text(player.getStatistic(Statistic.MOB_KILLS))),
                new Sideboard.Line("days", Component.text("経過日数", NamedTextColor.WHITE), player ->
                        Component.text(player.getWorld().getFullTime() / 24000 + "日")),
                new Sideboard.Line("difficulty", Component.text("難易度", NamedTextColor.WHITE), player ->
                        Component.text(difficultyName(player.getWorld().getDifficulty())))));
    }

    private static String difficultyName(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> "ピースフル";
            case EASY -> "イージー";
            case NORMAL -> "ノーマル";
            case HARD -> "ハード";
        };
    }

    private Sideboard.Line attrLine(String id, String label, NamedTextColor color, AttributeType type) {
        return new Sideboard.Line(id, Component.text(label, color),
                player -> Component.text(formatAttribute(type, engine.get(player, type))));
    }

    /** 属性値の表示書式(％属性は「25%」、それ以外は #.##)。 */
    public static String formatAttribute(AttributeType type, double value) {
        return type.percentDisplay() ? FORMAT.format(value * 100) + "%" : FORMAT.format(value);
    }
}
