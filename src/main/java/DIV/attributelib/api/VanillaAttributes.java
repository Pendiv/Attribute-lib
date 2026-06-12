package DIV.attributelib.api;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

/**
 * バニラ属性(MAX_HEALTH / ATTACK_DAMAGE / BLOCK_BREAK_SPEED 等)への薄いファサード。
 * カスタム属性と違い計算・保存はバニラが行うため、ここでやるのは
 * 「キー固定・冪等な付け外し」の定型文を1行に畳むことだけ。
 *
 * <p>同じ key で再度 set すると必ず上書きになる(remove → add)。
 * 「getItemMeta して足したか分からないまま add して重複」という Bukkit 定番バグを構造的に消す。</p>
 */
public final class VanillaAttributes {

    private VanillaAttributes() {
    }

    /**
     * モディファイアを冪等に設定する(同キーの既存分は除去してから追加)。
     * エンティティの NBT に保存され、再起動を跨いで残る。
     *
     * @return 属性をこのエンティティが持たない場合 false(例: クリーパーに ATTACK_DAMAGE)
     */
    public static boolean set(LivingEntity entity, Attribute attribute, NamespacedKey key,
                              AttributeModifier.Operation operation, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        removeByKey(instance, key);
        instance.addModifier(new AttributeModifier(key, amount, operation));
        return true;
    }

    /**
     * 一時モディファイアを冪等に設定する(保存されず、アンロードで消える)。
     * 装備や状態から毎回再計算して付け直す用途はこちら(ゴースト強化防止)。
     */
    public static boolean setTransient(LivingEntity entity, Attribute attribute, NamespacedKey key,
                                       AttributeModifier.Operation operation, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        removeByKey(instance, key);
        instance.addTransientModifier(new AttributeModifier(key, amount, operation));
        return true;
    }

    /** 指定キーのモディファイアを取り除く。 */
    public static boolean remove(LivingEntity entity, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        return removeByKey(instance, key);
    }

    /** 最終値。属性を持たないエンティティなら def を返す。 */
    public static double value(LivingEntity entity, Attribute attribute, double def) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null ? instance.getValue() : def;
    }

    /** 基礎値。属性を持たないエンティティなら def を返す。 */
    public static double baseValue(LivingEntity entity, Attribute attribute, double def) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null ? instance.getBaseValue() : def;
    }

    /** 基礎値を設定する。 @return 属性を持たないエンティティなら false */
    public static boolean setBaseValue(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        instance.setBaseValue(value);
        return true;
    }

    private static boolean removeByKey(AttributeInstance instance, NamespacedKey key) {
        boolean removed = false;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                instance.removeModifier(modifier);
                removed = true;
            }
        }
        return removed;
    }
}
