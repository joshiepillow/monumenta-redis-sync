package com.playmonumenta.redissync;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.projectiles.ProjectileSource;

import com.destroystokyo.paper.event.player.PlayerAdvancementDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementDataSaveEvent;
import com.destroystokyo.paper.event.player.PlayerDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerDataSaveEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.adapters.VersionAdapter.SaveData;
import com.playmonumenta.redissync.api.RedisAPI;
import com.playmonumenta.redissync.utils.ScoreboardUtils;

public class DataEventListener implements Listener {
	private Gson mGson = new Gson();
	private static DataEventListener INSTANCE = null;

	private final Logger mLogger;
	private final Set<UUID> mTransferringPlayers = new HashSet<UUID>();

	public DataEventListener(Logger logger) {
		mLogger = logger;
		INSTANCE = this;
	}

	/********************* Public API *********************/

	public static void setPlayerAsTransferring(Player player) {
		INSTANCE.mTransferringPlayers.add(player.getUniqueId());
	}

	public static void setPlayerAsNotTransferring(Player player) {
		INSTANCE.mTransferringPlayers.remove(player.getUniqueId());
	}

	public static boolean isPlayerTransferring(Player player) {
		return INSTANCE.mTransferringPlayers.contains(player.getUniqueId());
	}

	/********************* Data Save/Load Event Handlers *********************/

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataLoadEvent(PlayerAdvancementDataLoadEvent event) {
		Player player = event.getPlayer();

		/* Advancements */
		/* TODO: Decrease verbosity */
		mLogger.info("Loading advancements data for player=" + player.getName());
		String jsonData = RedisAPI.sync().lindex(getRedisAdvancementsPath(player), 0);
		mLogger.finer("Data:" + jsonData);
		if (jsonData != null) {
			event.setJsonData(jsonData);
		} else {
			mLogger.warning("No advancements data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
		}

		/* Scoreboards */
		/* TODO: Decrease verbosity */
		mLogger.info("Loading scoreboard data for player=" + player.getName());
		jsonData = RedisAPI.sync().lindex(getRedisScoresPath(player), 0);
		mLogger.finer("Data:" + jsonData);
		if (jsonData != null) {
			JsonObject obj = mGson.fromJson(jsonData, JsonObject.class);
			if (obj != null) {
				ScoreboardUtils.loadFromJsonObject(player, obj);
			} else {
				mLogger.severe("Failed to parse player '" + player.getName() + "' scoreboard data as JSON. This results in data loss!");
			}
		} else {
			mLogger.warning("No scoreboard data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		Player player = event.getPlayer();
		if (isPlayerTransferring(player)) {
			mLogger.fine("Ignoring PlayerAdvancementDataSaveEvent for player:" + player.getName());
			event.setCancelled(true);
			return;
		}

		/* Advancements */
		/* TODO: Decrease verbosity */
		mLogger.info("Saving advancements data for player=" + player.getName());
		mLogger.finer("Data:" + event.getJsonData());
		RedisAPI.sync().lpush(getRedisAdvancementsPath(player), event.getJsonData());

		/* Scoreboards */
		/* TODO: Decrease verbosity */
		mLogger.info("Saving scoreboard data for player=" + player.getName());
		String data = ScoreboardUtils.getAsJsonObject(player).toString();
		mLogger.finer("Data:" + data);
		RedisAPI.sync().lpush(getRedisScoresPath(player), data);

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
		mLogger.finer("data: " + b64encode(data));

		/* Load the per-shard data */
		String shardData = RedisAPI.sync().hget(getRedisPerShardDataPath(player), Conf.getShard());
		if (shardData == null) {
			/* This is not an error - this will happen whenever a player first visits a new shard */
			mLogger.info("Player '" + player.getName() + "' has never been to this shard before");
		} else {
			mLogger.finer("sharddata: " + shardData);
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
		if (isPlayerTransferring(player)) {
			mLogger.fine("Ignoring PlayerDataSaveEvent for player:" + player.getName());
			event.setCancelled(true);
			return;
		}

		/* TODO: Decrease verbosity */
		mLogger.info("Saving data for player=" + player.getName());

		try {
			SaveData data = MonumentaRedisSync.getVersionAdapter().extractSaveData(player, event.getData());

			mLogger.finer("data: " + b64encode(data.getData()));
			mLogger.finer("sharddata: " + data.getShardData());
			RedisAPI.syncStringBytes().lpush(getRedisDataPath(player), data.getData());
			RedisAPI.sync().hset(getRedisPerShardDataPath(player), Conf.getShard(), data.getShardData());

			event.setCancelled(true);
		} catch (IOException ex) {
			mLogger.severe("Failed to save player data: " + ex.toString());
			ex.printStackTrace();
		}
	}

	/********************* Transferring Restriction Event Handlers *********************/

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinEvent(PlayerJoinEvent event) {
		setPlayerAsNotTransferring(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerInteractEvent(PlayerInteractEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerFishEvent(PlayerFishEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerBedEnterEvent(PlayerBedEnterEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void blockBreakEvent(BlockBreakEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		cancelEventIfTransferring(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryClickEvent(InventoryClickEvent event) {
		cancelEventIfTransferring(event.getWhoClicked(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryDragEvent(InventoryDragEvent event) {
		cancelEventIfTransferring(event.getWhoClicked(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		cancelEventIfTransferring(event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		cancelEventIfTransferring(event.getWhoClicked(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		cancelEventIfTransferring(event.getEntity(), event);
		cancelEventIfTransferring(event.getCombuster(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		cancelEventIfTransferring(event.getEntity(), event);
		cancelEventIfTransferring(event.getDamager(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void entityDamageEvent(EntityDamageEvent event) {
		cancelEventIfTransferring(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void hangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		cancelEventIfTransferring(event.getRemover(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		ProjectileSource shooter = event.getEntity().getShooter();
		if (shooter != null && shooter instanceof Player) {
			cancelEventIfTransferring((Player)shooter, event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void potionSplashEvent(PotionSplashEvent event) {
		event.getAffectedEntities().removeIf(entity -> (entity instanceof Player && isPlayerTransferring((Player)entity)));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		event.getAffectedEntities().removeIf(entity -> (entity instanceof Player && isPlayerTransferring((Player)entity)));
	}

	/********************* Private Utility Methods *********************/

	private void cancelEventIfTransferring(Entity entity, Cancellable event) {
		if (entity != null && entity instanceof Player && isPlayerTransferring((Player)entity)) {
			event.setCancelled(true);
		}
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

	private String getRedisScoresPath(Player player) {
		return String.format("%s:playerdata:%s:scores", Conf.getDomain(), player.getUniqueId().toString());
	}

	private static String b64encode(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}
}
