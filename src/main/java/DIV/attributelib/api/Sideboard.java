package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import DIV.attributelib.sideboard.SideboardManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Function;

/**
 * ステータスサイドバー(/sideboard)のジャンル登録 API。
 *
 * <p>プレイヤーは {@code /sideboard <ジャンル...>} で最大3ジャンルを選び、
 * 選択順に上から表示される。選択はプレイヤー PDC に保存され、再ログイン後も維持される。</p>
 *
 * <p>利用側プラグインは独自ジャンルを追加できる:</p>
 * <pre>{@code
 * Sideboard.registerGenre(this, "growth", Component.text("成長"), List.of(
 *         Sideboard.line(Component.text("レベル", NamedTextColor.YELLOW),
 *                 player -> Component.text(getLevel(player))),
 *         Sideboard.attributeLine(Component.text("回避率", NamedTextColor.YELLOW), MY_DODGE)));
 * }</pre>
 */
public final class Sideboard {

    /**
     * サイドバーの1行。
     *
     * @param id           行の識別子(削除・差し替えに使う。不要なら null)
     * @param label        行ラベル(色込み)
     * @param value        プレイヤー → 表示値(メインスレッドで refreshTicks ごとに呼ばれる。軽い処理のみ)
     * @param refreshTicks 更新間隔(tick)。既定 20(1秒)。座標やクールダウンなど
     *                     随時更新したい行は小さく(最短 2 = 0.1秒)。2tick 単位に丸められる
     */
    public record Line(String id, Component label, Function<Player, Component> value, long refreshTicks) {

        public Line {
            if (refreshTicks <= 0) {
                refreshTicks = 20;
            }
            // タスク粒度(2tick)の倍数に丸め、[0.1秒, 60秒] に収める
            refreshTicks = Math.clamp((refreshTicks / 2) * 2, 2, 1200);
        }

        /** 既定の更新間隔(1秒)の行。 */
        public Line(String id, Component label, Function<Player, Component> value) {
            this(id, label, value, 20);
        }
    }

    private Sideboard() {
    }

    private static SideboardManager manager() {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.sideboard();
    }

    /**
     * ジャンルを登録する。
     *
     * @param id    コマンドで指定する識別子(小文字英数。プラグインを跨いで一意であること)
     * @param title 見出し(色込み)
     * @param lines 行(1ジャンル数行まで。サイドバー全体で15行を超えた分は表示されない)
     */
    public static void registerGenre(Plugin plugin, String id, Component title, List<Line> lines) {
        manager().registerGenre(plugin, id, title, lines);
    }

    /**
     * 既存ジャンルの末尾に行を追加する(標準ジャンルにも追加できる)。
     *
     * <pre>{@code
     * // ステータスに現在座標を追加
     * Sideboard.addLine("status", Sideboard.line("position",
     *         Component.text("座標", NamedTextColor.GREEN),
     *         p -> Component.text(p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ())));
     * }</pre>
     *
     * @throws IllegalArgumentException ジャンルが存在しない場合
     */
    public static void addLine(String genreId, Line line) {
        manager().addLine(genreId, line);
    }

    /**
     * 行 ID を指定して既存ジャンルから行を取り除く。標準行の差し替えにも使える:
     *
     * <pre>{@code
     * // 標準の難易度行を自前の危険度に差し替え(例: EnhancedMobs)
     * Sideboard.removeLine("user", "difficulty");
     * Sideboard.addLine("user", Sideboard.line("difficulty",
     *         Component.text("危険度", NamedTextColor.WHITE), p -> ...));
     * }</pre>
     *
     * @return 取り除けたら true(該当 ID が無ければ false)
     */
    public static boolean removeLine(String genreId, String lineId) {
        return manager().removeLine(genreId, lineId);
    }

    /** 任意の値を表示する行(ID なし = 後から個別削除できない)。 */
    public static Line line(Component label, Function<Player, Component> value) {
        return new Line(null, label, value);
    }

    /** ID 付きの行。 */
    public static Line line(String id, Component label, Function<Player, Component> value) {
        return new Line(id, label, value);
    }

    /**
     * 更新間隔指定付きの行。クールダウン・座標などの随時更新向け。
     *
     * <pre>{@code
     * // 0.1秒(2tick)更新のスキルCT表示
     * Sideboard.addLine("combat", Sideboard.line("weapon_ct",
     *         Component.text("スキルCT", NamedTextColor.YELLOW), p -> ..., 2));
     * }</pre>
     */
    public static Line line(String id, Component label, Function<Player, Component> value, long refreshTicks) {
        return new Line(id, label, value, refreshTicks);
    }

    /** 属性の最終値を表示する行(％表示などの書式は属性定義に従う)。 */
    public static Line attributeLine(Component label, AttributeType type) {
        return attributeLine(null, label, type);
    }

    /** ID 付きの属性行。 */
    public static Line attributeLine(String id, Component label, AttributeType type) {
        return new Line(id, label, player -> Component.text(
                SideboardManager.formatAttribute(type, Attributes.get(player, type))));
    }
}
