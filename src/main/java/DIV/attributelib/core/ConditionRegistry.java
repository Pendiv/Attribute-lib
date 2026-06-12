package DIV.attributelib.core;

import DIV.attributelib.api.Condition;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * 適用条件のレジストリ(内部中枢)。利用側は
 * {@link DIV.attributelib.api.Conditions} ファサードを使うこと。
 */
public final class ConditionRegistry {

    private final Logger logger;
    private final Map<NamespacedKey, Condition> conditions = new LinkedHashMap<>();
    private final Set<NamespacedKey> warnedUnknown = new HashSet<>();

    public ConditionRegistry(Logger logger) {
        this.logger = logger;
    }

    public Condition register(Plugin owner, String id, Component displayName, Predicate<LivingEntity> test) {
        AttributeEngine.checkMainThread();
        NamespacedKey key = new NamespacedKey(owner, id);
        Condition condition = new Condition(key, displayName, test);
        if (conditions.putIfAbsent(key, condition) != null) {
            throw new IllegalStateException("条件が二重登録されました: " + key);
        }
        return condition;
    }

    public Condition byKey(NamespacedKey key) {
        return conditions.get(key);
    }

    public Collection<Condition> registered() {
        return Collections.unmodifiableCollection(conditions.values());
    }

    /**
     * 条件キーを評価する。null(無条件)は常に成立。
     * 未登録キーは不成立(効果が消える側に倒す)+初回のみ警告。
     * 述語が例外を投げた場合も不成立(1つの壊れた条件が計算全体を落とさない)。
     */
    public boolean isActive(NamespacedKey conditionKey, LivingEntity entity) {
        if (conditionKey == null) {
            return true;
        }
        Condition condition = conditions.get(conditionKey);
        if (condition == null) {
            if (warnedUnknown.add(conditionKey)) {
                logger.warning("未登録の条件キーです(提供元プラグインの欠落?)。不成立として扱います: " + conditionKey);
            }
            return false;
        }
        try {
            return condition.test().test(entity);
        } catch (Exception e) {
            if (warnedUnknown.add(conditionKey)) {
                logger.warning("条件 " + conditionKey + " の評価で例外。不成立として扱います: " + e);
            }
            return false;
        }
    }
}
