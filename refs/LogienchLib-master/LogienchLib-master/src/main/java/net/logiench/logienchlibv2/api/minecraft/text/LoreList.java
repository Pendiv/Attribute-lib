package net.logiench.logienchlibv2.api.minecraft.text;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link ComponentUtil}と連携した{@link ArrayList}の拡張です。
 * Stringを入力するとComponentUtilを介して適応します。
 * 内部的には<code>ArrayList&ltComponent&gt</code>に扱われます。
 */
public class LoreList extends ArrayList<Component> {
	public static LoreList create() {
		return new LoreList();
	}

	public static LoreList create(int initialCapacity) {
		return new LoreList(initialCapacity);
	}

	public static LoreList createString(@NotNull Collection<? extends String> c) {
		return new LoreList(c.stream().map(ComponentUtil::text).toList());
	}

	public static LoreList createComponent(@NotNull Collection<? extends Component> c) {
		return new LoreList(c);
	}

	private LoreList(@NotNull Collection<? extends Component> c) {
		super(c);
	}

	private LoreList(int initialCapacity) {
		super(initialCapacity);
	}

	private LoreList() {
		super();
	}

	public String getString(int index) {
		return ComponentUtil.string(get(index));
	}

	public String getFirstString() {
		return ComponentUtil.string(getFirst());
	}

	public String getLastString() {
		return ComponentUtil.string(getLast());
	}

	public String set(int index, String element) {
		return ComponentUtil.string(
			set(index, ComponentUtil.text(element))
		);
	}

	public boolean add(String element) {
		return add(ComponentUtil.text(element));
	}

	public void add(int index, String element) {
		add(index, ComponentUtil.text(element));
	}

	public void addFirst(String element) {
		addFirst(ComponentUtil.text(element));
	}

	public void addLast(String element) {
		addLast(ComponentUtil.text(element));
	}

	public boolean addAllString(Collection<? extends String> c) {
		return addAll(c.stream().map(ComponentUtil::text).toList());
	}

	public boolean addAllString(int index, Collection<? extends String> c) {
		return addAll(index, c.stream().map(ComponentUtil::text).toList());
	}

	/**
	 * 全ての内容をStringに変換します。
	 *
	 * @return 編集不可なList
	 */
	@Unmodifiable
	public List<String> toStringAll() {
		return ComponentUtil.string(this);
	}
}
