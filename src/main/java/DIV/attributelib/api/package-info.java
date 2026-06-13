/**
 * attributelib の公開 API。
 *
 * <p>このパッケージは {@link org.jspecify.annotations.NullMarked} 宣言下にあり、
 * 型はすべて非 null がデフォルト。null を受理/返却する箇所だけ
 * {@link org.jspecify.annotations.Nullable} で明示する。利用側プラグインの IDE は
 * これを読み取り、null を渡したコードを記述時に警告できる(Paper API と同じ JSpecify)。</p>
 */
@NullMarked
package DIV.attributelib.api;

import org.jspecify.annotations.NullMarked;
