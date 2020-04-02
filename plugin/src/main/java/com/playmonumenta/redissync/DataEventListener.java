package com.playmonumenta.redissync;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementDataSaveEvent;

public class DataEventListener implements Listener {
	private final Logger mLogger;

	public DataEventListener(Logger logger) {
		mLogger = logger;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataLoadEvent(PlayerAdvancementDataLoadEvent event) {
		mLogger.info("Loading advancements data for player=" + event.getPlayer().getUniqueId().toString());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		mLogger.info("Saving advancements data for player=" + event.getPlayer().getUniqueId().toString() + " : " + event.getJsonData());
	}
}
