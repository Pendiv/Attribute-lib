package DIV.attributelib.api;

/**
 * 付与したモディファイア1件を指すチケット。{@link #remove()} でそのモディファイアだけを
 * 取り除ける(時限式でも手動で早期解除できる)。
 *
 * <p>remove は冪等: 2回呼んでも、期限切れで既に消えていても、何も起きない。</p>
 */
public interface ModifierHandle {

    /** このモディファイアを取り除く。既に消えていれば何もしない。 */
    void remove();

    /** このモディファイアがまだ有効か(期限切れ・remove 済みなら false)。 */
    boolean isActive();
}
