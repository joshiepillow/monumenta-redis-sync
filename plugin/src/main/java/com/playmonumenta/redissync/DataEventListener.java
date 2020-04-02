package com.playmonumenta.redissync;

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
		/* TODO: Decrease verbosity */
		mLogger.info("Loading advancements data for player=" + event.getPlayer().getName());

		String jsonData = RedisAPI.sync().get(getRedisAdvancementPath(event.getPlayer()));
		if (jsonData != null) {
			event.setJsonData(jsonData);
		} else {
			mLogger.warning("No advancements data for player '" + event.getPlayer().getName() + "' - if they are not new, this is a serious error!");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		if (mSaveDisabledPlayers.contains(event.getPlayer().getUniqueId())) {
			mLogger.fine("Ignoring PlayerAdvancementDataSaveEvent for player:" + event.getPlayer().getName());
			return;
		}

		/* TODO: Decrease verbosity */
		mLogger.info("Saving advancements data for player=" + event.getPlayer().getName());
		mLogger.finer("Data:" + event.getJsonData());

		RedisAPI.sync().set(getRedisAdvancementPath(event.getPlayer()), event.getJsonData());
	}

	private String getRedisAdvancementPath(Player player) {
		return String.format("playerdata:%s:%s:advancements:current", Conf.getDomain(), player.getUniqueId().toString());
	}

	public static void disableDataSavingUntilNextLogin(Player player) {
		INSTANCE.mSaveDisabledPlayers.add(player.getUniqueId());
	}
}
