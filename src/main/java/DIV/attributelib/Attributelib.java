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
        StandardAttributes.init(this, engine, elements);
        engine.registerBridges();
        DIV.attributelib.api.Conditions.initStandard(this);

        getServer().getPluginManager().registerEvents(new CleanupListener(engine), this);
        getServer().getPluginManager().registerEvents(new DamageListener(engine, elements), this);
        getServer().getPluginManager().registerEvents(new HealListener(engine), this);
        getServer().getPluginManager().registerEvents(new EquipmentSyncListener(engine), this);

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
}
