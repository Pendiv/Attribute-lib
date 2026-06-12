package net.logiench.shardCore.core._party;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PartyProvider {
	private final List<PartyData> parties = new ArrayList<>();
	/// パーティーにそのプレイヤーが存在するかを確認するためのデータ
	private final List<UUID> members = new ArrayList<>();

	public boolean createParty(UUID owner, boolean isPrivate) {
		if (members.contains(owner)) {
			return false;
		}
		parties.add(new PartyData(owner, new ArrayList<>(), isPrivate));
		return true;
	}

	private Optional<PartyData> getParty(UUID owner) {
		return parties.stream().filter(partyData -> partyData.owner.equals(owner)).findFirst();
	}

	public boolean partyExists(UUID owner) {
		return getParty(owner).isPresent();
	}

	public boolean addMember(UUID owner, UUID member) {
		if (members.contains(member)) {
			return false; // すでにパーティーに参加しているメンバーが指定された
		}
		Optional<PartyData> partyOptional = getParty(owner);
		if (partyOptional.isEmpty()) {
			return false;
		}
		partyOptional.get().member.add(member);
		return true;
	}

	public void removeMember(UUID owner, UUID member) {
		getParty(owner).ifPresent(partyData -> partyData.member.remove(member));
	}

	private record PartyData(UUID owner, List<UUID> member, boolean isPrivate) {
	}
}
