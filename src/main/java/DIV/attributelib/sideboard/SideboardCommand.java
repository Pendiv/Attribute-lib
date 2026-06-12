package DIV.attributelib.sideboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code /sideboard} — ステータスサイドバーの表示設定(全プレイヤー使用可)。
 *
 * <pre>
 * /sideboard <ジャンル1> [ジャンル2] [ジャンル3]   選択順に上から表示
 * /sideboard off                                   非表示
 * /sideboard                                       現在の設定と使い方
 * </pre>
 */
public final class SideboardCommand implements CommandExecutor, TabCompleter {

    private final SideboardManager manager;

    public SideboardCommand(SideboardManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return true;
        }
        if (args.length == 0) {
            List<String> saved = manager.savedSelection(player);
            sender.sendMessage("現在: " + (manager.isShowing(player) ? "表示中" : "非表示")
                    + (saved.isEmpty() ? "" : " (" + String.join(" → ", saved) + ")"));
            sender.sendMessage("使い方: /sideboard <ジャンル1> [ジャンル2] [ジャンル3] | on | off");
            sender.sendMessage("ジャンル: " + String.join(", ", manager.genreIds()));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            manager.hide(player);
            sender.sendMessage("サイドバーを非表示にしました(/sideboard on で復帰)");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("on")) {
            if (manager.showSaved(player)) {
                sender.sendMessage("サイドバーを表示: " + String.join(" → ", manager.savedSelection(player)));
            } else {
                sender.sendMessage("保存された選択がありません。/sideboard <ジャンル...> で選んでください");
            }
            return true;
        }
        if (args.length > SideboardManager.MAX_GENRES) {
            sender.sendMessage("ジャンルは最大 " + SideboardManager.MAX_GENRES + " つまでです");
            return true;
        }
        // 順序維持の重複排除 + 検証
        Set<String> ids = new LinkedHashSet<>();
        for (String arg : args) {
            String id = arg.toLowerCase(Locale.ROOT);
            if (!manager.hasGenre(id)) {
                sender.sendMessage("不明なジャンルです: " + arg
                        + " (利用可能: " + String.join(", ", manager.genreIds()) + ")");
                return true;
            }
            ids.add(id);
        }
        manager.show(player, List.copyOf(ids));
        sender.sendMessage("サイドバーを表示: " + String.join(" → ", ids));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || args.length > SideboardManager.MAX_GENRES) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>(manager.genreIds());
        if (args.length == 1) {
            candidates.add("on");
            candidates.add("off");
        }
        // 既に指定済みのジャンルは候補から外す
        for (int i = 0; i < args.length - 1; i++) {
            candidates.remove(args[i].toLowerCase(Locale.ROOT));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(c -> c.startsWith(prefix)).toList();
    }
}
