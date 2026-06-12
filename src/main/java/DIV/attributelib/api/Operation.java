package DIV.attributelib.api;

/**
 * モディファイアの演算種別。適用順序は宣言順で固定:
 * <pre>final = clamp( (base + ΣADD) × ΠMULTIPLY )、SET があれば最後の SET が全てに勝つ</pre>
 */
public enum Operation {
    /** 加算。value をそのまま足す。 */
    ADD,
    /** 乗算。value を係数として掛ける(1.2 = +20%)。複数は積で合成される。 */
    MULTIPLY,
    /** 上書き。計算結果を無視して value にする。複数あれば最後に追加されたものが勝つ。 */
    SET
}
