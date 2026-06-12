package net.logiench.shardCore.core.item.base.def;

public enum ItemGroup {
	ITEM,
	GEM,
	ARMOR,
	WEAPON,
	ACCESSORY,
}

/*

main スケーリングしない + 完成度で変化
sub スケーリングしない + 完全ランダムで+-変化 : スケーリングする可能性もあり : 外部から決める(10レべ武器をサブだけ30レべ帯リロールなど)

装備固有 スケーリング
main, subは物によっては上位レベルに勝てる




sub ->

int level -> double

double +-

@todo
AttributeEnum側でスケーリング倍率を指定
スケーリングするかどうかはboolでEnumに持たせる
それ以外にもWeaponとかArmorならスケーリングしないとかあるからlevelとか与えたらMapで返してくれるメソッドをabstractで実装みたいでもいいかも

 */
