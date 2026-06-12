package DIV.attributelib.damage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * jar に同梱したデータパック(カスタムダメージタイプ定義)をメインワールドの
 * datapacks フォルダへ配備する。
 *
 * <p>データパックはワールドロード時に読み込まれるため、配備は onLoad
 * (ワールドロード前)に行う。既存ファイルと内容が一致すれば何もしない。
 * 内容が変わった場合(attributelib 更新時)は上書きし、その回のワールドロードに
 * 間に合わなければ onEnable 側のレジストリ検査が再起動を促す(企画書 §8-3)。</p>
 */
public final class DatapackInstaller {

    private static final String PACK_DIR_NAME = "attributelib";
    private static final List<String> FILES = List.of(
            "pack.mcmeta",
            "data/attributelib/damage_type/physical_pierce.json",
            "data/attributelib/damage_type/magic.json",
            "data/attributelib/damage_type/true_damage.json",
            "data/attributelib/damage_type/elemental.json",
            "data/minecraft/tags/damage_type/bypasses_armor.json",
            "data/minecraft/tags/damage_type/bypasses_effects.json",
            "data/minecraft/tags/damage_type/bypasses_enchantments.json",
            "data/minecraft/tags/damage_type/bypasses_resistance.json",
            "data/minecraft/tags/damage_type/bypasses_invulnerability.json",
            "data/minecraft/tags/damage_type/bypasses_cooldown.json",
            "data/minecraft/tags/damage_type/witch_resistant_to.json"
    );

    private DatapackInstaller() {
    }

    /**
     * @return 書き込んだ(新規+更新)ファイル数。0 なら配備済みで変更なし
     */
    public static int install(Plugin plugin) {
        Path packDir = mainWorldFolder().resolve("datapacks").resolve(PACK_DIR_NAME);
        int written = 0;
        for (String file : FILES) {
            try (InputStream in = plugin.getResource("datapack/" + file)) {
                if (in == null) {
                    throw new IllegalStateException("jar 内にデータパックリソースがありません: " + file);
                }
                byte[] content = in.readAllBytes();
                Path target = packDir.resolve(file);
                if (Files.exists(target) && Arrays.equals(Files.readAllBytes(target), content)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.write(target, content);
                written++;
            } catch (IOException e) {
                throw new IllegalStateException("データパックの配備に失敗しました: " + file, e);
            }
        }
        if (written > 0) {
            plugin.getLogger().info("データパックを配備しました (" + written + " ファイル): " + packDir);
        }
        return written;
    }

    /**
     * メインワールドのフォルダ。onLoad 時点ではワールドが未ロードのため
     * server.properties の level-name から解決する。
     */
    private static Path mainWorldFolder() {
        Path container = Bukkit.getWorldContainer().toPath();
        Path properties = container.resolve("server.properties");
        String levelName = "world";
        if (Files.exists(properties)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(properties)) {
                props.load(in);
                levelName = props.getProperty("level-name", "world");
            } catch (IOException e) {
                // 読めなければ既定値 "world" で続行(下の resolve が外れても
                // onEnable のレジストリ検査が警告する)
            }
        }
        return container.resolve(levelName);
    }
}
