package DIV.attributelib;

import DIV.attributelib.api.StandardAttributes;
import DIV.attributelib.command.AlibCommand;
import DIV.attributelib.core.AttributeEngine;
import DIV.attributelib.damage.DamageListener;
import DIV.attributelib.damage.DamageTypes;
import DIV.attributelib.damage.DatapackInstaller;
import DIV.attributelib.damage.ElementRegistry;
import DIV.attributelib.damage.HealListener;
import DIV.attributelib.equip.EquipmentSyncListener;
import DIV.attributelib.listener.CleanupListener;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * attributelib — カスタム属性・ダメージ計算・装備表示の基盤ライブラリ。
 * 利用側プラグインは {@link DIV.attributelib.api.Attributes} を入口にする。
 */
public final class Attributelib extends JavaPlugin {

    private static Attributelib instance;

    private AttributeEngine engine;
    private ElementRegistry elements;
    private DIV.attributelib.sideboard.SideboardManager sideboard;

    @Override
    public void onLoad() {
        instance = this;
        // onEnable ではなく onLoad で生成: 利用側が自分の onLoad から触っても壊れないように
        engine = new AttributeEngine(this);
        elements = new ElementRegistry(engine);
        // データパックはワールドロード前(=onLoad)に配備しないとその起動で読まれない
        try {
            DatapackInstaller.install(this);
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        // config.yml を用意し、バージョン差/破損があれば管理者の変更を引き継いで再生成する
        DIV.attributelib.core.ConfigMigrator.run(this);
        raiseVanillaCaps();

        StandardAttributes.init(this, engine, elements);
        engine.registerBridges();
        DIV.attributelib.api.Conditions.initStandard(this);

        getServer().getPluginManager().registerEvents(new CleanupListener(engine), this);
        getServer().getPluginManager().registerEvents(new DamageListener(engine, elements, armorOverflow()), this);
        getServer().getPluginManager().registerEvents(new HealListener(engine), this);
        getServer().getPluginManager().registerEvents(new EquipmentSyncListener(engine), this);

        // サイドバー(標準ジャンルがブリッジ属性を参照するため registerBridges 後に生成)
        sideboard = new DIV.attributelib.sideboard.SideboardManager(this, engine);
        getServer().getPluginManager().registerEvents(sideboard, this);
        var sideboardCommand = new DIV.attributelib.sideboard.SideboardCommand(sideboard);
        PluginCommand sideboardPluginCommand = getCommand("sideboard");
        if (sideboardPluginCommand == null) {
            throw new IllegalStateException("plugin.yml に sideboard コマンドが定義されていません");
        }
        sideboardPluginCommand.setExecutor(sideboardCommand);
        sideboardPluginCommand.setTabCompleter(sideboardCommand);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // PAPI が居る時だけ拡張クラスをロードする(softdepend なのでクラス参照をここに隔離)
            var expansion = new DIV.attributelib.papi.AttributelibExpansion(this, engine);
            expansion.register();
            getServer().getPluginManager().registerEvents(expansion, this); // 退出時のスナップショット掃除
            getLogger().info("PlaceholderAPI 連携を登録しました (%attributelib_...%)");
        }

        List<NamespacedKey> missing = DamageTypes.missingTypes();
        if (!missing.isEmpty()) {
            getLogger().warning("カスタムダメージタイプが未登録です: " + missing);
            getLogger().warning("データパックは配備済みのため、サーバーを再起動すると有効になります(初回のみ)");
        }

        AlibCommand command = new AlibCommand(this, engine);
        PluginCommand pluginCommand = getCommand("attributelib");
        if (pluginCommand == null) {
            throw new IllegalStateException("plugin.yml に attributelib コマンドが定義されていません");
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    /** config の caps: セクションに従ってバニラ属性のハード上限を解放する。 */
    private void raiseVanillaCaps() {
        var section = getConfig().getConfigurationSection("caps");
        if (section == null) {
            return;
        }
        if (!section.getBoolean("enabled", true)) {
            getLogger().info("バニラ属性の上限解放(reflection)は無効化されています (caps.enabled: false)");
            return;
        }
        var values = section.getValues(false);
        values.remove("enabled"); // enabled はマスタートグル。属性キーではないので除外する
        DIV.attributelib.core.VanillaCaps.raiseFromConfig(this, values);
    }

    /** config の armor-overflow: セクションから上限超過軽減設定を組む(無効なら null)。 */
    private DIV.attributelib.damage.DamagePipeline.ArmorOverflow armorOverflow() {
        if (!getConfig().getBoolean("armor-overflow.enabled", true)) {
            return null;
        }
        double threshold = getConfig().getDouble("armor-overflow.threshold", 100);
        double rate = getConfig().getDouble("armor-overflow.reduction-per-point", 0.008);
        double max = getConfig().getDouble("armor-overflow.max-reduction", 0.95);
        return new DIV.attributelib.damage.DamagePipeline.ArmorOverflow(threshold, rate, max);
    }

    @Override
    public void onDisable() {
        // 永続データは書き込み透過で常に PDC 反映済み。キャッシュを捨てるだけでよい
        if (engine != null) {
            engine.clearCache();
        }
        instance = null;
    }

    /** プラグイン未ロード時は null(ファサード側で分かりやすい例外に変換する)。 */
    public static Attributelib instance() {
        return instance;
    }

    public AttributeEngine engine() {
        return engine;
    }

    public ElementRegistry elements() {
        return elements;
    }

    public DIV.attributelib.core.ConditionRegistry conditions() {
        return engine.conditions();
    }

    public DIV.attributelib.sideboard.SideboardManager sideboard() {
        return sideboard;
    }
}
