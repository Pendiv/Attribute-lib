package net.logiench.shardLib.api.register.mob;

import net.logiench.shardLib.api.register.attribute.AttributeDefinitionRegister;
import org.jetbrains.annotations.NotNull;

public interface MobRegister {
	/**
	 * モブの根幹、全てに適応されるステータスを登録するクラスを取得します。
	 */
	@NotNull
	AttributeDefinitionRegister coreAttributes();

	/**
	 * モブが持つステータスを登録するクラスを取得します。
	 */
	@NotNull
	MobAttributeRegister attributes();
}
