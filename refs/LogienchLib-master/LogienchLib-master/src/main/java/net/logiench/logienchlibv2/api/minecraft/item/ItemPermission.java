package net.logiench.logienchlibv2.api.minecraft.item;

public enum ItemPermission {
	/// 指定なし
	NONE(0),
	/// 無敵、もしくはOP持ちのみ
	INVULNERABLE_OR_OP(1),
	/// OP持ち
	OP(2),
	/// 無敵、かつOP持ち
	INVULNERABLE_AND_OP(3),
	;

	private final byte level;

	ItemPermission(int level) {
		this.level = (byte) level;
	}

	public byte getLevel() {
		return level;
	}
}

