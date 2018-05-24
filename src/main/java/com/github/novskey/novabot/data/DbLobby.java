package com.github.novskey.novabot.data;

/**
 * Created by Paris on 18/01/2018.
 */
public class DbLobby {
    private final String gymId;
    public int memberCount;
    public String channelId;
    public String roleId;
    public int nextTimeLeftUpdate;
    public String inviteCode;

    public DbLobby(String gymId, int memberCount, String channelId, String roleId, int nextTimeLeftUpdate, String inviteCode) {
        this.gymId = gymId;
        this.memberCount = memberCount;
        this.channelId = channelId;
        this.roleId = roleId;
        this.nextTimeLeftUpdate = nextTimeLeftUpdate;
        this.inviteCode = inviteCode;
    }
}
