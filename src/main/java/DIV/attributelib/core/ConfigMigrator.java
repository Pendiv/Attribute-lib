package DIV.attributelib.core;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * config.yml の「意向尊重」マイグレーション。
 *
 * <p>起動時に config を点検し、バージョン差({@code config-version})または YAML 破損を検出したら、
 * 同梱デフォルト(最新のキー+コメント)を土台に、<b>管理者が変えた値・足したキーだけ</b>を引き継いだ
 * config を再生成する。元ファイルは {@code config_backup_<時刻>.yml} に退避してから上書きするため、
 * 設定内容は失われない。フォーマットとコメントは常にデフォルト(最新)から作り直される。</p>
 *
 * <p>他プラグインが API 経由で登録する属性は config.yml には現れない(実行時のもの)ため、
 * このマイグレーションの対象外。キー構成を変えたら同梱 config.yml の {@code config-version} と
 * {@link #CURRENT_VERSION} を上げること。</p>
 */
public final class ConfigMigrator {

    /**
     * 同梱 config.yml の現行バージョン。<b>キー構成を変えたら同梱 config.yml の
     * {@code config-version} と必ず一緒に上げること</b>(片方だけだと
     * {@code ConfigMigratorTest.bundledVersionMatchesConstant} が落ちる)。
     */
    public static final int CURRENT_VERSION = 2;

    private static final String CONFIG_NAME = "config.yml";
    private static final String VERSION_KEY = "config-version";

    private ConfigMigrator() {
    }

    /**
     * onEnable から呼ぶ入口。config.yml を用意し、必要ならマイグレーションして reloadConfig する。
     * 失敗してもプラグイン起動は止めない(警告ログのみ。既存 config はそのまま使われる)。
     */
    public static void run(Plugin plugin) {
        plugin.saveDefaultConfig(); // 無ければ最新デフォルトを書く(この場合マイグレーション不要)
        Logger log = plugin.getLogger();
        Path configPath = plugin.getDataFolder().toPath().resolve(CONFIG_NAME);

        String defaultYaml = readResource(plugin, CONFIG_NAME);
        if (defaultYaml == null) {
            log.warning("jar 内に " + CONFIG_NAME + " がありません。マイグレーションをスキップします");
            return;
        }

        String userYaml;
        try {
            userYaml = Files.readString(configPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warning(CONFIG_NAME + " を読めませんでした。マイグレーションをスキップします: " + e.getMessage());
            return;
        }

        MigrationResult result;
        try {
            result = migrate(defaultYaml, userYaml);
        } catch (IllegalStateException e) {
            log.severe("マイグレーションに失敗しました(同梱デフォルトが不正): " + e.getMessage());
            return;
        }
        if (!result.changed()) {
            return; // バージョン一致・破損なし → そのまま
        }

        // 元ファイルを退避してから新内容を書く(意向尊重: 何も失わない)
        try {
            // ミリ秒まで含めて同一秒の二重マイグレーションでもバックアップ名が衝突しないようにする
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path backup = plugin.getDataFolder().toPath().resolve("config_backup_" + stamp + ".yml");
            Files.copy(configPath, backup);
            Files.writeString(configPath, result.merged(), StandardCharsets.UTF_8);
            plugin.reloadConfig();
            log.info("config.yml を最新フォーマットへ更新しました(意向尊重マイグレーション)。"
                    + "元ファイルは " + backup.getFileName() + " に退避済み。");
            if (result.broken()) {
                log.warning("旧 config.yml は YAML として壊れていたため、値を引き継げずデフォルトで再生成しました"
                        + "(元ファイルは退避済み。インデントはタブではなくスペースで書いてください)。");
            }
        } catch (IOException e) {
            log.severe("config.yml のマイグレーション書き込みに失敗しました: " + e.getMessage());
        }
    }

    /**
     * マイグレーションの純粋ロジック(Bukkit 実行状態に依存せず単体テスト可能)。
     *
     * <p>デフォルト(最新キー+コメント)を土台に、ユーザーの葉キーの値を上書きする。
     * {@code config-version} は常にデフォルト側を採用し、デフォルトでセクションのパスに
     * ユーザーがスカラーを置いていた場合(型衝突)はデフォルト構造を優先する。
     * ユーザー独自のキー(例: {@code caps} への属性追加)は保持される。</p>
     *
     * @param defaultYaml 同梱デフォルト config の中身
     * @param userYaml    現行ユーザー config の中身
     * @return 書き換えが必要かどうか + 新しい config 内容
     * @throws IllegalStateException 同梱デフォルトが YAML として壊れている(=ビルド不良)場合
     */
    public static MigrationResult migrate(String defaultYaml, String userYaml) {
        YamlConfiguration def = load(defaultYaml);
        if (def == null) {
            throw new IllegalStateException("同梱デフォルト config.yml が YAML として壊れています");
        }
        int currentVersion = def.getInt(VERSION_KEY, CURRENT_VERSION);

        YamlConfiguration user = load(userYaml);
        boolean broken = user == null; // パース不能 → 値を救えないのでデフォルトで再生成

        if (!broken && user.getInt(VERSION_KEY, 0) == currentVersion) {
            return new MigrationResult(false, false, userYaml); // 最新・正常 → 変更不要
        }

        if (!broken) {
            for (String key : user.getKeys(true)) {
                if (user.isConfigurationSection(key)) {
                    continue; // セクション(コンテナ)は中の葉で扱う
                }
                if (VERSION_KEY.equals(key)) {
                    continue; // バージョンは常に最新(デフォルト)を採用
                }
                if (def.isConfigurationSection(key)) {
                    continue; // 型衝突(デフォルトはセクション)→ デフォルト構造を優先
                }
                // 既存キーは値を引き継ぎ、ユーザー独自キーは追加(デフォルトのコメントは残る)
                def.set(key, user.get(key));
            }
        }
        return new MigrationResult(true, broken, def.saveToString());
    }

    /** YAML をコメント込みで読む。パース不能なら null。 */
    private static YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        try {
            config.loadFromString(yaml);
            return config;
        } catch (InvalidConfigurationException e) {
            return null;
        }
    }

    private static String readResource(Plugin plugin, String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                return null;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
                return sb.toString();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @param changed 書き換えが必要か(false なら元のまま)
     * @param broken  旧ファイルが YAML として壊れていたか(値を引き継げずデフォルト再生成)
     * @param merged  新しい config の中身(changed=false の時は元の内容)
     */
    public record MigrationResult(boolean changed, boolean broken, String merged) {
    }
}
