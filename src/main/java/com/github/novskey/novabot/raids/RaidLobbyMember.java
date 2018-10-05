package com.github.novskey.novabot.raids;

public class RaidLobbyMember {
	
	public String memberId;
	public int count;
	public String time;
	
	public RaidLobbyMember(String memberId, int count, String time) {
		this.memberId = memberId;
		this.count = count;
		this.time = time;
	}

	public boolean equals(Object o) {
		if (o instanceof RaidLobbyMember) {
			RaidLobbyMember member = (RaidLobbyMember) o;
			return member.memberId == memberId;
		}
		return false;
	}
	
	public int hashCode() {
		return memberId.hashCode();
	}
	
}
