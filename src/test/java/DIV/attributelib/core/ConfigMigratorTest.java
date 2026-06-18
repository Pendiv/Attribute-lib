package DIV.attributelib.core;

import DIV.attributelib.core.ConfigMigrator.MigrationResult;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {

    private static final String DEFAULT = """
            # ヘッダー意向尊重コメント
            config-version: 1
            # caps の説明コメント
            caps:
              max_health: 100000
              armor: 1000
            armor-overflow:
              enabled: true
              threshold: 100
              max-reduction: 0.95
            """;

    private static YamlConfiguration reload(String yaml) throws InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        return y;
    }

    @Test
    @DisplayName("最新バージョンかつ正常なら変更しない")
    void sameVersionNoChange() {
        MigrationResult result = ConfigMigrator.migrate(DEFAULT, DEFAULT);
        assertFalse(result.changed());
        assertFalse(result.broken());
    }

    @Test
    @DisplayName("バージョン差: 管理者の変更値・追加キーを引き継ぎつつ新キーを補完")
    void migratesPreservingUserIntent() throws InvalidConfigurationException {
        // config-version 無し(旧)、max_health を変更、caps に独自キー追加、enabled を変更、
        // threshold を削除(=新デフォルトから補完されるべき)、max-reduction 無し
        String user = """
                caps:
                  max_health: 200000
                  armor: 1000
                  attack_damage: 5000
                armor-overflow:
                  enabled: false
                """;
        MigrationResult result = ConfigMigrator.migrate(DEFAULT, user);
        assertTrue(result.changed());
        assertFalse(result.broken());

        YamlConfiguration merged = reload(result.merged());
        assertEquals(1, merged.getInt("config-version"), "バージョンは最新に");
        assertEquals(200000, merged.getInt("caps.max_health"), "変更値を引き継ぐ");
        assertEquals(5000, merged.getInt("caps.attack_damage"), "独自追加キーを保持");
        assertFalse(merged.getBoolean("armor-overflow.enabled"), "変更値を引き継ぐ");
        assertEquals(100, merged.getInt("armor-overflow.threshold"), "削られた既定キーを補完");
        assertEquals(0.95, merged.getDouble("armor-overflow.max-reduction"), 1e-9, "新規既定キーを補完");
        assertTrue(result.merged().contains("意向尊重"), "デフォルトのコメントが再生成される");
    }

    @Test
    @DisplayName("旧ファイルが YAML 破損: 値を救えずデフォルトで再生成(broken=true)")
    void brokenYamlRegeneratesFromDefault() throws InvalidConfigurationException {
        String broken = "caps:\n  max_health: [unclosed\n  : : :\n";
        MigrationResult result = ConfigMigrator.migrate(DEFAULT, broken);
        assertTrue(result.changed());
        assertTrue(result.broken());

        YamlConfiguration merged = reload(result.merged());
        assertEquals(1, merged.getInt("config-version"));
        assertEquals(100000, merged.getInt("caps.max_health"), "破損時はデフォルト値");
        assertTrue(result.merged().contains("意向尊重"));
    }

    @Test
    @DisplayName("型衝突: デフォルトがセクションのパスにユーザーのスカラーは無視(構造優先)")
    void typeConflictKeepsDefaultStructure() throws InvalidConfigurationException {
        // caps をスカラーにしてしまった壊れかけ config(パースは通る)
        String user = """
                config-version: 0
                caps: 5
                armor-overflow:
                  enabled: false
                """;
        MigrationResult result = ConfigMigrator.migrate(DEFAULT, user);
        assertTrue(result.changed());

        YamlConfiguration merged = reload(result.merged());
        // caps はデフォルトのセクション構造のまま(スカラー 5 で潰されない)
        assertTrue(merged.isConfigurationSection("caps"), "caps はセクションのまま");
        assertEquals(100000, merged.getInt("caps.max_health"));
        assertFalse(merged.getBoolean("armor-overflow.enabled"), "正常な変更は引き継ぐ");
    }

    @Test
    @DisplayName("同梱 config.yml の config-version が CURRENT_VERSION と一致する(bump 忘れ検知)")
    void bundledVersionMatchesConstant() throws Exception {
        try (var in = getClass().getResourceAsStream("/config.yml")) {
            assertTrue(in != null, "同梱 config.yml が classpath にあること");
            String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            YamlConfiguration cfg = reload(yaml);
            assertEquals(ConfigMigrator.CURRENT_VERSION, cfg.getInt("config-version"),
                    "config.yml の config-version と ConfigMigrator.CURRENT_VERSION は一緒に上げること");
        }
    }

    @Test
    @DisplayName("caps トグル追加 (v1→v2): 管理者の max_health を保持し enabled を補完")
    void capsToggleMigrationPreservesAdminValue() throws InvalidConfigurationException {
        String defaultV2 = """
                config-version: 2
                caps:
                  enabled: true
                  max_health: 10000
                  armor: 1000
                """;
        String userV1 = """
                config-version: 1
                caps:
                  max_health: 100000
                  armor: 1000
                """;
        MigrationResult result = ConfigMigrator.migrate(defaultV2, userV1);
        assertTrue(result.changed());
        assertFalse(result.broken());

        YamlConfiguration merged = reload(result.merged());
        assertEquals(2, merged.getInt("config-version"), "バージョンは最新に");
        assertTrue(merged.getBoolean("caps.enabled"), "新トグル enabled が補完される");
        assertEquals(100000, merged.getInt("caps.max_health"), "管理者の値を保持(既定 10000 に戻さない)");
    }
}
