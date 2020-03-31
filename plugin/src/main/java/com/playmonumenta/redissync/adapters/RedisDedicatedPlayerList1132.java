package com.playmonumenta.redissync.adapters;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.event.Listener;

import net.minecraft.server.v1_13_R2.AdvancementDataPlayer;
import net.minecraft.server.v1_13_R2.DedicatedPlayerList;
import net.minecraft.server.v1_13_R2.DedicatedServer;
import net.minecraft.server.v1_13_R2.DimensionManager;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.MinecraftServer;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.ServerStatisticManager;

public class RedisDedicatedPlayerList1132 extends DedicatedPlayerList implements Listener {
	private static RedisDedicatedPlayerList1132 INSTANCE = null;
	private final Logger mLog;
	private final MinecraftServer mServer;

	public static RedisDedicatedPlayerList1132 getInstance() {
		return INSTANCE;
	}

    public static void inject(Logger logger) throws Exception {
		CraftServer craftServer = (CraftServer)Bukkit.getServer();

		/* Get a handle to the root NMS server object */
        DedicatedServer server = (DedicatedServer) craftServer.getServer();

		/* Existing dedicated player list to override */
		DedicatedPlayerList list = craftServer.getHandle();

		/* Create a new instance of this player list adapter */
		INSTANCE = new RedisDedicatedPlayerList1132(list, logger);

		/* Inject the player list adapter into CraftBukkit */
		Field field = CraftServer.class.getDeclaredField("playerList");
		field.setAccessible(true);
		field.set(craftServer, INSTANCE);

		/* Inject our player list adapter over top of the vanilla one */
		server.a(INSTANCE);
    }

	private RedisDedicatedPlayerList1132(DedicatedPlayerList list, Logger logger) {
		super(list);
		mLog = logger;
		mServer = list.getServer();
    }

	@Override
	public void savePlayerFile(EntityPlayer entityplayer) {
		mLog.info("Saving player data: " + entityplayer.getUniqueID().toString());

        entityplayer.lastSave = MinecraftServer.currentTick; // Paper
        this.playerFileData.save(entityplayer);
        ServerStatisticManager serverstatisticmanager = (ServerStatisticManager) entityplayer.getStatisticManager(); // CraftBukkit

        if (serverstatisticmanager != null) {
            serverstatisticmanager.a();
        }

        AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) entityplayer.getAdvancementData(); // CraftBukkit

        if (advancementdataplayer != null) {
            advancementdataplayer.c();
        }
	}

	@Nullable
	@Override
    public NBTTagCompound a(EntityPlayer player) {
		mLog.info("Loading player data: " + player.getUniqueID().toString());
		return super.a(player);
    }

    public AdvancementDataPlayer h(EntityPlayer entityplayer) {
        AdvancementDataPlayer advancementdataplayer = entityplayer.getAdvancementData();

        if (advancementdataplayer == null) {
			UUID uuid = entityplayer.getUniqueID();
            File file = new File(this.mServer.getWorldServer(DimensionManager.OVERWORLD).getDataManager().getDirectory(), "advancements");
            File file1 = new File(file, uuid + ".json");

            advancementdataplayer = new AdvancementDataPlayer(this.server, file1, entityplayer);
        }

        return advancementdataplayer;
    }
}
