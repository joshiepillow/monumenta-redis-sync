package com.playmonumenta.redissync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.event.player.PlayerAdvancementDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerAdvancementDataSaveEvent;
import com.destroystokyo.paper.event.player.PlayerDataLoadEvent;
import com.destroystokyo.paper.event.player.PlayerDataSaveEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.adapters.VersionAdapter;
import com.playmonumenta.redissync.adapters.VersionAdapter.SaveData;
import com.playmonumenta.redissync.utils.ScoreboardUtils;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;

public class DataEventListener implements Listener {
	private static final int TIMEOUT_SECONDS = 10;
	private static DataEventListener INSTANCE = null;

	private final Gson mGson = new Gson();
	private final Logger mLogger;
	private final VersionAdapter mAdapter;
	private final Set<UUID> mTransferringPlayers = new HashSet<UUID>();

	private final HashMap<UUID, List<RedisFuture<?>>> mPendingSaves = new HashMap<>();

	protected DataEventListener(Logger logger, VersionAdapter adapter) {
		mLogger = logger;
		mAdapter = adapter;
		INSTANCE = this;
	}

	/********************* Protected API *********************/

	protected static void setPlayerAsTransferring(Player player) {
		INSTANCE.mTransferringPlayers.add(player.getUniqueId());
	}

	protected static void setPlayerAsNotTransferring(Player player) {
		INSTANCE.mTransferringPlayers.remove(player.getUniqueId());
	}

	protected static boolean isPlayerTransferring(Player player) {
		return INSTANCE.mTransferringPlayers.contains(player.getUniqueId());
	}

	protected static void waitForPlayerToSave(Player player, Runnable callback) {
		INSTANCE.waitForPlayerToSaveInternal(player, callback);
	}

	private void waitForPlayerToSaveInternal(Player player, Runnable callback) {
		Plugin plugin = MonumentaRedisSync.getInstance();

		List<RedisFuture<?>> futures = mPendingSaves.remove(player.getUniqueId());

		if (futures == null || futures.isEmpty()) {
			mLogger.warning("Got request to wait for save commit but no pending save operations found. This might be a bug with the plugin that uses MonumentaRedisSync");
			callback.run();
			return;
		}

		long startTime = System.currentTimeMillis();

		new BukkitRunnable() {
			public void run() {
				if (!LettuceFutures.awaitAll(TIMEOUT_SECONDS, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]))) {
					mLogger.severe("Got timeout waiting to commit transactions for player '" + player.getName() + "'. This is very bad!");
				}

				/* Run the callback on the main thread */
				new BukkitRunnable() {
					public void run() {
						/* TODO: Verbosity */
						mLogger.info("Committing save took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds");
						callback.run();
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

	/********************* Data Save/Load Event Handlers *********************/

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataLoadEvent(PlayerAdvancementDataLoadEvent event) {
		Player player = event.getPlayer();

		RedisFuture<String> advanceFuture = RedisAPI.getInstance().async().lindex(getRedisAdvancementsPath(player), 0);
		RedisFuture<String> scoreFuture = RedisAPI.getInstance().async().lindex(getRedisScoresPath(player), 0);

		try {
			/* Advancements */
			/* TODO: Decrease verbosity */
			mLogger.info("Loading advancements data for player=" + player.getName());
			String jsonData = advanceFuture.get();
			mLogger.finer("Data:" + jsonData);
			if (jsonData != null) {
				event.setJsonData(jsonData);
			} else {
				mLogger.warning("No advancements data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
			}

			/* Scoreboards */
			/* TODO: Decrease verbosity */
			mLogger.info("Loading scoreboard data for player=" + player.getName());
			jsonData = scoreFuture.get();
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
		} catch (InterruptedException | ExecutionException ex) {
			mLogger.severe("Failed to get advancements/scores data for player '" + player.getName() + "'. This is very bad!");
			ex.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerAdvancementDataSaveEvent(PlayerAdvancementDataSaveEvent event) {
		/* Always cancel saving the player file to disk with this plugin present */
		event.setCancelled(true);

		Player player = event.getPlayer();
		if (isPlayerTransferring(player)) {
			mLogger.fine("Ignoring PlayerAdvancementDataSaveEvent for player:" + player.getName());
			return;
		}

		List<RedisFuture<?>> futures = mPendingSaves.remove(player.getUniqueId());
		if (futures == null) {
			futures = new ArrayList<>();
		} else {
			futures.removeIf(future -> future.isDone());
		}

		/* Advancements */
		/* TODO: Decrease verbosity */
		mLogger.info("Saving advancements data for player=" + player.getName());
		mLogger.finer("Data:" + event.getJsonData());
		String advPath = getRedisAdvancementsPath(player);
		futures.add(RedisAPI.getInstance().async().lpush(advPath, event.getJsonData()));
		futures.add(RedisAPI.getInstance().async().ltrim(advPath, 0, 20)); // TODO Config

		/* Scoreboards */
		/* TODO: Decrease verbosity */
		mLogger.info("Saving scoreboard data for player=" + player.getName());
		long startTime = System.currentTimeMillis();
		String data = ScoreboardUtils.getAsJsonObject(player).toString();
		/* TODO: Decrease verbosity */
		mLogger.info("Scoreboard saving took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds on main thread");
		mLogger.finer("Data:" + data);
		String scorePath = getRedisScoresPath(player);
		futures.add(RedisAPI.getInstance().async().lpush(scorePath, data));
		futures.add(RedisAPI.getInstance().async().ltrim(scorePath, 0, 20)); // TODO Config

		/* Don't block - store the pending futures for completion later */
		mPendingSaves.put(player.getUniqueId(), futures);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDataLoadEvent(PlayerDataLoadEvent event) {
		Player player = event.getPlayer();
		/* TODO: Decrease verbosity */
		mLogger.info("Loading data for player=" + player.getName());

		/* Load the primary shared NBT data */
		byte[] data = RedisAPI.getInstance().syncStringBytes().lindex(getRedisDataPath(player), 0);
		if (data == null) {
			mLogger.warning("No data for player '" + player.getName() + "' - if they are not new, this is a serious error!");
			return;
		}
		mLogger.finer("data: " + b64encode(data));

		/* Load the per-shard data */
		String shardData = RedisAPI.getInstance().sync().hget(getRedisPerShardDataPath(player), Conf.getShard());
		if (shardData == null) {
			/* This is not an error - this will happen whenever a player first visits a new shard */
			mLogger.info("Player '" + player.getName() + "' has never been to this shard before");
		} else {
			mLogger.finer("sharddata: " + shardData);
		}

		try {
			Object nbtTagCompound = mAdapter.retrieveSaveData(player, data, shardData);
			event.setData(nbtTagCompound);
		} catch (IOException ex) {
			mLogger.severe("Failed to load player data: " + ex.toString());
			ex.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDataSaveEvent(PlayerDataSaveEvent event) {
		event.setCancelled(true);

		Player player = event.getPlayer();
		if (isPlayerTransferring(player)) {
			mLogger.fine("Ignoring PlayerDataSaveEvent for player:" + player.getName());
			return;
		}

		/* TODO: Decrease verbosity */
		mLogger.info("Saving data for player=" + player.getName());

		List<RedisFuture<?>> futures = mPendingSaves.remove(player.getUniqueId());
		if (futures == null) {
			futures = new ArrayList<>();
		} else {
			futures.removeIf(future -> future.isDone());
		}

		try {
			SaveData data = mAdapter.extractSaveData(player, event.getData());

			mLogger.finer("data: " + b64encode(data.getData()));
			mLogger.finer("sharddata: " + data.getShardData());
			String dataPath = getRedisDataPath(player);
			futures.add(RedisAPI.getInstance().asyncStringBytes().lpush(dataPath, data.getData()));
			futures.add(RedisAPI.getInstance().asyncStringBytes().ltrim(dataPath, 0, 20)); // TODO Config
			futures.add(RedisAPI.getInstance().async().hset(getRedisPerShardDataPath(player), Conf.getShard(), data.getShardData()));
			String histPath = getRedisHistoryPath(player);
			futures.add(RedisAPI.getInstance().async().lpush(histPath, Conf.getShard() + "|" + Long.toString(System.currentTimeMillis())));
			futures.add(RedisAPI.getInstance().async().ltrim(histPath, 0, 20)); // TODO Config
		} catch (IOException ex) {
			mLogger.severe("Failed to save player data: " + ex.toString());
			ex.printStackTrace();
		}

		/* Don't block - store the pending futures for completion later */
		mPendingSaves.put(player.getUniqueId(), futures);
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

	private String getRedisHistoryPath(Player player) {
		return String.format("%s:playerdata:%s:history", Conf.getDomain(), player.getUniqueId().toString());
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
