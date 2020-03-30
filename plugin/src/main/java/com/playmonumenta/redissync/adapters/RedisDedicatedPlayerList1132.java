package com.playmonumenta.redissync.adapters;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.CraftServer;
import org.bukkit.event.Listener;

import net.minecraft.server.v1_13_R2.DedicatedPlayerList;
import net.minecraft.server.v1_13_R2.DedicatedServer;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.NBTTagCompound;

public class RedisDedicatedPlayerList1132 extends DedicatedPlayerList implements Listener {
	private static RedisDedicatedPlayerList1132 INSTANCE = null;
	private final Logger mLog;

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
    }

	@Override
	public void savePlayerFile(EntityPlayer player) {
		super.savePlayerFile(player);
		mLog.info("Saving player data: " + player.getUniqueID().toString());
	}

	@Nullable
	@Override
    public NBTTagCompound a(EntityPlayer player) {
		mLog.info("Loading player data: " + player.getUniqueID().toString());
		return super.a(player);
    }
}
