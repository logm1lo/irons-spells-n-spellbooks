package com.example.testmod.capabilities.magic;

import com.example.testmod.TestMod;
import com.example.testmod.network.ClientBoundSyncPlayerData;
import com.example.testmod.setup.Messages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SyncedSpellData {

    //TODO: may want to switch this to ServerPlayer.UUID
    private final int serverPlayerId;
    private Player player;

    /**
     * REMINDER: Need to update ClientBoundSyncPlayerData when adding fields to this class
     **/
    private boolean hasAngelWings;
    private boolean hasEvasion;
    private boolean hasHeartstop;
    private float hearstopDamage;

    //Use this on the client
    public SyncedSpellData(int serverPlayerId) {
        this.player = null;
        this.serverPlayerId = serverPlayerId;
        this.hasAngelWings = false;
        this.hasEvasion = false;
    }

    //Use this on the server
    public SyncedSpellData(Player player) {
        this(player == null ? -1 : player.getId());
        this.player = player;
    }

    public int getServerPlayerId() {
        return serverPlayerId;
    }

    private void doSync() {
        //this.player will only be null on the client side
        TestMod.LOGGER.debug("SyncedSpellData.doSync player:{}", player);

        if (this.player != null) {
            Messages.sendToPlayer(new ClientBoundSyncPlayerData(this), (ServerPlayer) player);
            Messages.sendToPlayersTrackingEntity(new ClientBoundSyncPlayerData(this), player);
        }
    }

    public void syncToPlayer(ServerPlayer serverPlayer) {
        Messages.sendToPlayer(new ClientBoundSyncPlayerData(this), serverPlayer);
    }

    public boolean getHasAngelWings() {
        return hasAngelWings;
    }

    public void setHasAngelWings(boolean hasAngelWings) {
        this.hasAngelWings = hasAngelWings;
        doSync();
    }

    public boolean getHasEvasion() {
        return hasEvasion;
    }

    public void setHasEvasion(boolean hasEvasion) {
        this.hasEvasion = hasEvasion;
        doSync();
    }

    public boolean getHasHeartstop() {
        return hasHeartstop;
    }

    public void setHasHeartstop(boolean hasHeartstop) {
        this.hasHeartstop = hasHeartstop;
        doSync();
    }

    public float getHeartstopAccumulatedDamage() {
        return hearstopDamage;
    }

    public void setHeartstopAccumulatedDamage(float damage) {
        this.hearstopDamage = damage;
        doSync();
    }

    public void addHeartstopDamage(float amount) {
        this.hearstopDamage += amount;
        doSync();
    }
}
