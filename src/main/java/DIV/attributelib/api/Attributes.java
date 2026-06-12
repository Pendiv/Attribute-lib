package DIV.attributelib.api;

import DIV.attributelib.Attributelib;
import DIV.attributelib.core.AttributeEngine;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

/**
 * attributelib のメイン API。利用側プラグインの操作はすべて1行で完結する。
 *
 * <pre>{@code
 * // onEnable で登録(plugin.yml に depend: [attributelib] を忘れずに)
 * static AttributeType DODGE;
 * DODGE = Attributes.register(this, "dodge_chance", 0.0, 0.0, 100.0);
 *
 * // 読み取り(期限切れ処理・再計算は全部こちらでやる)
 * double chance = Attributes.get(mob, DODGE);
 *
 * // 付与と解除
 * Attributes.setBase(mob, DODGE, 5.0);
 * ModifierHandle buff = Attributes.add(mob, DODGE, "myplugin:trait/swift", Operation.ADD, 10.0);
 * buff.remove();
 *
 * // 時限(200tick)。スケジューラ管理は不要、読み取り時に自動失効する
 * Attributes.add(mob, DODGE, "myplugin:potion", Operation.MULTIPLY, 1.5, 200);
 *
 * // 装備同期などの REPLACE 運用: 同じ sourceId をまとめて剥がして付け直す
 * Attributes.removeAll(player, "myplugin:item/mainhand");
 * }</pre>
 *
 * <p><b>規約</b>: 全 API はメインスレッド専用。sourceId は {@code "プラグイン名:用途"} 形式を
 * 推奨('|' は使用不可)。</p>
 */
public final class Attributes {

    private Attributes() {
    }

    private static AttributeEngine engine() {
        Attributelib plugin = Attributelib.instance();
        if (plugin == null) {
            throw new IllegalStateException(
                    "attributelib がまだロードされていません。plugin.yml の depend に attributelib を追加してください");
        }
        return plugin.engine();
    }

    // ---- 登録 ----

    /**
     * カスタム属性を登録する。利用側プラグインの onEnable で呼び、返り値を定数として保持すること。
     *
     * @param plugin       登録元プラグイン(属性IDの名前空間になるため衝突しない)
     * @param id           属性ID(小文字英数と ._- のみ)
     * @param defaultValue base 未設定エンティティでの基礎値
     * @throws IllegalStateException 同じプラグインが同じ ID を二重登録した場合
     */
    public static AttributeType register(Plugin plugin, String id, double defaultValue) {
        return engine().register(plugin, id, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /** 範囲付き登録。最終値が [min, max] にクランプされる(確率属性 0..1 など)。 */
    public static AttributeType register(Plugin plugin, String id, double defaultValue, double min, double max) {
        return engine().register(plugin, id, defaultValue, min, max);
    }

    /**
     * 表示情報付き登録。
     *
     * @param displayName    lore 表示などで使う表示名(null なら ID から自動生成)
     * @param percentDisplay true なら値を百分率で表示(確率・割合系の属性向け。0.25 → +25%)
     */
    public static AttributeType register(Plugin plugin, String id, double defaultValue, double min, double max,
                                         net.kyori.adventure.text.Component displayName, boolean percentDisplay) {
        return engine().register(plugin, id, defaultValue, min, max, displayName, percentDisplay);
    }

    // ---- 読み取り ----

    /** 最終値(= clamp((base + ΣADD) × ΠMULTIPLY)、SET があれば最後の SET)。 */
    public static double get(LivingEntity entity, AttributeType type) {
        return engine().get(entity, type);
    }

    /** 基礎値(未設定なら属性の defaultValue)。 */
    public static double getBase(LivingEntity entity, AttributeType type) {
        return engine().getBase(entity, type);
    }

    // ---- 変更 ----

    /** 基礎値を設定する(PDC に保存される)。 */
    public static void setBase(LivingEntity entity, AttributeType type, double value) {
        engine().setBase(entity, type, value);
    }

    /** 基礎値の明示設定を解除し defaultValue に戻す。 */
    public static void resetBase(LivingEntity entity, AttributeType type) {
        engine().resetBase(entity, type);
    }

    /** 無期限の永続モディファイアを付与する(再起動を跨いで残る)。 */
    public static ModifierHandle add(LivingEntity entity, AttributeType type,
                                     String sourceId, Operation operation, double value) {
        return engine().add(entity, type, sourceId, operation, value, 0, true);
    }

    /**
     * 時限の永続モディファイアを付与する。期限は対象ワールドの gameTime 基準なので
     * 再起動・チャンクアンロードを跨いでも正しく失効する。
     *
     * @param durationTicks 失効までの tick 数(正の値)
     */
    public static ModifierHandle add(LivingEntity entity, AttributeType type,
                                     String sourceId, Operation operation, double value, long durationTicks) {
        requirePositiveDuration(durationTicks);
        return engine().add(entity, type, sourceId, operation, value, durationTicks, true);
    }

    /**
     * 一時モディファイアを付与する。PDC に保存されず、チャンクアンロードや再起動で消える。
     * 装備由来など「元から再生成できる」補正はこちらを使うこと(ゴースト強化防止)。
     */
    public static ModifierHandle addTransient(LivingEntity entity, AttributeType type,
                                              String sourceId, Operation operation, double value) {
        return engine().add(entity, type, sourceId, operation, value, 0, false);
    }

    /** 時限の一時モディファイアを付与する。 */
    public static ModifierHandle addTransient(LivingEntity entity, AttributeType type,
                                              String sourceId, Operation operation, double value, long durationTicks) {
        requirePositiveDuration(durationTicks);
        return engine().add(entity, type, sourceId, operation, value, durationTicks, false);
    }

    /** 指定 sourceId のモディファイアを(全属性から)まとめて取り除く。REPLACE 運用の前半。 */
    public static void removeAll(LivingEntity entity, String sourceId) {
        engine().removeBySource(entity, sourceId);
    }

    private static void requirePositiveDuration(long durationTicks) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "durationTicks は正の値が必要です(無期限なら duration なしのオーバーロードを使用): " + durationTicks);
        }
    }
}
