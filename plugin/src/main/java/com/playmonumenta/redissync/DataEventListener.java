package com.playmonumenta.redissync;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.destroystokyo.paper.event.player.PlayerAdvancementDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementDataSaveEvent;
import com.destroystokyo.paper.event.player.PlayerDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerDataSaveEvent;
import com.playmonumenta.redissync.adapters.VersionAdapter.SaveData;

public class DataEventListener implements Listener {
	private static DataEventListener INSTANCE = null;

	private final Logger mLogger;
	private final Set<UUID> mSaveDisabledPlayers = new HashSet<UUID>();

	public DataEventListener(Logger logger) {
		mLogger = logger;
		INSTANCE = this;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerJoinEvent(PlayerJoinEvent event) {
		mSaveDisabledPlayers.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataLoadEvent(PlayerAdvancementDataLoadEvent event) {
		Player player = event.getPlayer();
		/* TODO: Decrease verbosity */
		mLogger.info("Loading advancements data for player=" + player.getName());

		String jsonData = RedisAPI.sync().lindex(getRedisAdvancementsPath(player), 0);
		mLogger.finer("Data:" + jsonData);
		if (jsonData != null) {
			event.setJsonData(jsonData);
		} else {
			mLogger.warning("No advancements data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		Player player = event.getPlayer();
		if (mSaveDisabledPlayers.contains(player.getUniqueId())) {
			mLogger.fine("Ignoring PlayerAdvancementDataSaveEvent for player:" + player.getName());
			return;
		}

		/* TODO: Decrease verbosity */
		mLogger.info("Saving advancements data for player=" + player.getName());
		mLogger.finer("Data:" + event.getJsonData());
		RedisAPI.sync().lpush(getRedisAdvancementsPath(player), event.getJsonData());
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDataLoadEvent(PlayerDataLoadEvent event) {
		Player player = event.getPlayer();
		/* TODO: Decrease verbosity */
		mLogger.info("Loading data for player=" + player.getName());

		/* Load the primary shared NBT data */
		byte[] data = RedisAPI.syncStringBytes().lindex(getRedisDataPath(player), 0);
		if (data == null) {
			mLogger.warning("No data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
			return;
		}
		/* TODO: Decrease verbosity */
		mLogger.info("data: " + b64encode(data));

		/* Load the per-shard data */
		String shardData = RedisAPI.sync().hget(getRedisPerShardDataPath(player), Conf.getShard());
		if (shardData == null) {
			/* This is not an error - this will happen whenever a player first visits a new shard */
			/* TODO: Decrease Verbosity */
			mLogger.info("Player '" + player.getName() + "' has never been to this shard before");
		} else {
			mLogger.info("sharddata: " + shardData);
		}

		try {
			Object nbtTagCompound = MonumentaRedisSync.getVersionAdapter().retrieveSaveData(player, data, shardData);
			event.setData(nbtTagCompound);
		} catch (IOException ex) {
			mLogger.severe("Failed to load player data: " + ex.toString());
			ex.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDataSaveEvent(PlayerDataSaveEvent event) {
		Player player = event.getPlayer();
		if (mSaveDisabledPlayers.contains(player.getUniqueId())) {
			mLogger.fine("Ignoring PlayerDataSaveEvent for player:" + player.getName());
			return;
		}

		/* TODO: Decrease verbosity */
		mLogger.info("Saving data for player=" + player.getName());

		try {
			SaveData data = MonumentaRedisSync.getVersionAdapter().extractSaveData(player, event.getData());

			/* TODO: Decrease verbosity */
			mLogger.info("data: " + b64encode(data.getData()));
			mLogger.info("sharddata: " + data.getShardData());
			RedisAPI.syncStringBytes().lpush(getRedisDataPath(player), data.getData());
			RedisAPI.sync().hset(getRedisPerShardDataPath(player), Conf.getShard(), data.getShardData());

			event.setCancelled(true);
		} catch (IOException ex) {
			mLogger.severe("Failed to save player data: " + ex.toString());
			ex.printStackTrace();
		}
	}

	public static void disableDataSavingUntilNextLogin(Player player) {
		INSTANCE.mSaveDisabledPlayers.add(player.getUniqueId());
	}

	private String getRedisDataPath(Player player) {
		return String.format("%s:playerdata:%s:data", Conf.getDomain(), player.getUniqueId().toString());
	}

	private String getRedisPerShardDataPath(Player player) {
		return String.format("%s:playerdata:%s:sharddata", Conf.getDomain(), player.getUniqueId().toString());
	}

	private String getRedisAdvancementsPath(Player player) {
		return String.format("%s:playerdata:%s:advancements", Conf.getDomain(), player.getUniqueId().toString());
	}

	private static String b64encode(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}
}
