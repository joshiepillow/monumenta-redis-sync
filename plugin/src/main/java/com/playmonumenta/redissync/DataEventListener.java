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
		/* TODO: Decrease verbosity */
		mLogger.info("Loading advancements data for player=" + event.getPlayer().getUniqueId().toString());

		String jsonData = RedisAPI.sync().get("user:" + event.getPlayer().getUniqueId().toString() + ":advancements");
		if (jsonData != null) {
			event.setJsonData(jsonData);
		} else {
			mLogger.warning("No advancements data for player '" + event.getPlayer().getUniqueId().toString() + "' - if they are not new, this is a serious error!");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		/* TODO: Decrease verbosity */
		mLogger.info("Saving advancements data for player=" + event.getPlayer().getUniqueId().toString() + " : " + event.getJsonData());

		RedisAPI.sync().set("user:" + event.getPlayer().getUniqueId().toString() + ":advancements", event.getJsonData());
	}
}
