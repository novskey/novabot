package com.github.novskey.novabot.data;


import com.github.novskey.novabot.raids.RaidLobbyMember;
import java.util.HashSet;

/**
 * Created by Paris on 18/01/2018.
 */
public class DbLobby {
    private final String gymId;
    public String channelId;
    public String roleId;
    public int nextTimeLeftUpdate;
    public String inviteCode;
    public String lobbyChatId;
    public String[] lobbyChatIds;
    public HashSet<RaidLobbyMember> members = new HashSet<RaidLobbyMember>();

    public DbLobby(String gymId, String channelId, String roleId, int nextTimeLeftUpdate, String inviteCode, HashSet<RaidLobbyMember> members, String[] lobbyChatIds) {
        this.gymId = gymId;
        this.channelId = channelId;
        this.roleId = roleId;
        this.nextTimeLeftUpdate = nextTimeLeftUpdate;
        this.inviteCode = inviteCode;
        this.members = members;
        this.lobbyChatIds = lobbyChatIds;
    }
}
