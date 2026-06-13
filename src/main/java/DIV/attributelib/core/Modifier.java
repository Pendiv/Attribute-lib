package DIV.attributelib.core;

import DIV.attributelib.api.Operation;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.Nullable;

/**
 * 属性モディファイア1件(内部表現)。
 *
 * @param attribute  対象属性のキー
 * @param sourceId   付与元の識別子。"plugin:用途" 形式を推奨("enhancedmobs:trait/tank" 等)。
 *                   同じ sourceId をまとめて剥がす(REPLACE 運用)単位になる。
 * @param operation  演算種別
 * @param value      値
 * @param expiresAt  期限(対象エンティティのワールドの gameTime 基準の絶対 tick)。
 *                   {@link #PERMANENT} なら無期限。再起動でリセットされる
 *                   {@code Bukkit.getCurrentTick()} は使わないこと。
 * @param persistent true なら PDC に保存され再起動・再ロードを跨いで生存する。
 *                   false(transient)はメモリのみで、チャンクアンロードで消える。
 * @param condition  適用条件のキー(null = 無条件)。成立している間だけ計算に乗る。
 *                   述語はレジストリから評価時に引き直される。
 */
public record Modifier(
        NamespacedKey attribute,
        String sourceId,
        Operation operation,
        double value,
        long expiresAt,
        boolean persistent,
        @Nullable NamespacedKey condition
) {
    /** {@link #expiresAt} に入れると無期限になる値。 */
    public static final long PERMANENT = -1L;

    /** 無条件モディファイア。 */
    public Modifier(NamespacedKey attribute, String sourceId, Operation operation,
                    double value, long expiresAt, boolean persistent) {
        this(attribute, sourceId, operation, value, expiresAt, persistent, null);
    }

    public Modifier {
        if (attribute == null) {
            throw new IllegalArgumentException("attribute が null です");
        }
        if (operation == null) {
            // ここで弾かないと Calculator の switch まで生き延びて読み取り時に NPE になる(遅延爆発)
            throw new IllegalArgumentException("operation が null です");
        }
        if (sourceId == null || sourceId.isEmpty()) {
            throw new IllegalArgumentException("sourceId が空です");
        }
        if (sourceId.indexOf('|') >= 0) {
            throw new IllegalArgumentException("sourceId に '|' は使えません: " + sourceId);
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value が有限値ではありません: " + value);
        }
        if (expiresAt < 0 && expiresAt != PERMANENT) {
            throw new IllegalArgumentException("expiresAt が不正です: " + expiresAt);
        }
    }

    /** 指定時刻(gameTime)で期限切れか。 */
    public boolean isExpired(long gameTime) {
        return expiresAt != PERMANENT && gameTime >= expiresAt;
    }
}
