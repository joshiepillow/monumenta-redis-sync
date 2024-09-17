package com.playmonumenta.redissync;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.redissync.adapters.VersionAdapter.SaveData;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import com.playmonumenta.redissync.utils.Trie;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.wrappers.Rotation;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.util.Vector;

public class MonumentaRedisSyncAPI {
	public static class RedisPlayerData {
		private final UUID mUUID;
		private Object mNbtTagCompoundData;
		private String mAdvancements;
		private String mScores;
		private String mPluginData;
		private String mHistory;

		public RedisPlayerData(UUID uuid, Object nbtTagCompoundData, String advancements,
		                       String scores, String pluginData, String history) {
			mUUID = uuid;
			mNbtTagCompoundData = nbtTagCompoundData;
			mAdvancements = advancements;
			mScores = scores;
			mPluginData = pluginData;
			mHistory = history;
		}

		public UUID getUniqueId() {
			return mUUID;
		}

		public Object getNbtTagCompoundData() {
			return mNbtTagCompoundData;
		}

		public String getAdvancements() {
			return mAdvancements;
		}

		public String getScores() {
			return mScores;
		}

		public String getPluginData() {
			return mPluginData;
		}

		public String getHistory() {
			return mHistory;
		}

		public UUID getmUUID() {
			return mUUID;
		}

		public void setNbtTagCompoundData(Object nbtTagCompoundData) {
			this.mNbtTagCompoundData = nbtTagCompoundData;
		}

		public void setAdvancements(String advancements) {
			this.mAdvancements = advancements;
		}

		public void setScores(String scores) {
			this.mScores = scores;
		}

		public void setPluginData(String pluginData) {
			this.mPluginData = pluginData;
		}

		public void setHistory(String history) {
			this.mHistory = history;
		}
	}

	public static final int TIMEOUT_SECONDS = 10;
	public static final ArgumentSuggestions<CommandSender> SUGGESTIONS_ALL_CACHED_PLAYER_NAMES = ArgumentSuggestions.strings((info) ->
		MonumentaRedisSyncAPI.getAllCachedPlayerNames().toArray(String[]::new));

	private static final Trie<UUID> mNameToUuidTrie = new Trie<>();
	private static final Map<String, UUID> mNameToUuid = new ConcurrentHashMap<>();
	private static final Map<UUID, String> mUuidToName = new ConcurrentHashMap<>();

	protected static void updateUuidToName(UUID uuid, String name) {
		mUuidToName.put(uuid, name);
	}

	protected static void updateNameToUuid(String name, UUID uuid) {
		mNameToUuid.put(name, uuid);
		mNameToUuidTrie.put(name, uuid);
	}

	public static CompletableFuture<String> uuidToName(UUID uuid) {
		return RedisAPI.getInstance().async().hget("uuid2name", uuid.toString()).toCompletableFuture();
	}

	public static CompletableFuture<UUID> nameToUUID(String name) {
		return RedisAPI.getInstance().async().hget("name2uuid", name).thenApply((uuid) -> (uuid == null || uuid.isEmpty()) ? null : UUID.fromString(uuid)).toCompletableFuture();
	}

	public static CompletableFuture<Set<String>> getAllPlayerNames() {
		RedisFuture<Map<String, String>> future = RedisAPI.getInstance().async().hgetall("name2uuid");
		return future.thenApply(Map::keySet).toCompletableFuture();
	}

	public static CompletableFuture<Set<UUID>> getAllPlayerUUIDs() {
		RedisFuture<Map<String, String>> future = RedisAPI.getInstance().async().hgetall("uuid2name");
		return future.thenApply((data) -> data.keySet().stream().map(UUID::fromString).collect(Collectors.toSet())).toCompletableFuture();
	}

	public static @Nullable String cachedUuidToName(UUID uuid) {
		return mUuidToName.get(uuid);
	}

	public static @Nullable UUID cachedNameToUuid(String name) {
		return mNameToUuid.get(name);
	}

	public static Set<String> getAllCachedPlayerNames() {
		return new ConcurrentSkipListSet<>(mNameToUuid.keySet());
	}

	public static Set<UUID> getAllCachedPlayerUuids() {
		return new ConcurrentSkipListSet<>(mUuidToName.keySet());
	}

	public static @Nullable String getCachedCurrentName(String oldName) {
		UUID uuid = cachedNameToUuid(oldName);
		if (uuid == null) {
			return null;
		}
		return cachedUuidToName(uuid);
	}

	public static String getClosestPlayerName(String longestPossibleName) {
		@Nullable String result = mNameToUuidTrie.closestKey(longestPossibleName);
		if (result == null) {
			return "";
		}
		return result;
	}

	public static List<String> getSuggestedPlayerNames(String currentInput, int maxSuggestions) {
		return mNameToUuidTrie.suggestions(currentInput, maxSuggestions);
	}

	public static boolean isPlayerTransferring(Player player) {
		return DataEventListener.isPlayerTransferring(player);
	}

	public static void sendPlayer(Player player, String target) throws Exception {
		sendPlayer(player, target, null);
	}

	public static void sendPlayer(Player player, String target, @Nullable Location returnLoc) throws Exception {
		sendPlayer(player, target, returnLoc, null, null);
	}

	public static void sendPlayer(Player player, String target, @Nullable Location returnLoc, @Nullable Rotation rotation) throws Exception {
		sendPlayer(player, target, returnLoc, rotation == null ? null : rotation.getNormalizedYaw(), rotation == null ? null : rotation.getNormalizedPitch());
	}

	public static void sendPlayer(Player player, String target, @Nullable Location returnLoc, @Nullable Float returnYaw, @Nullable Float returnPitch) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		/* Don't allow transferring while transferring */
		if (DataEventListener.isPlayerTransferring(player)) {
			return;
		}

		long startTime = System.currentTimeMillis();

		if (target.equalsIgnoreCase(ConfigAPI.getShardName())) {
			player.sendMessage(ChatColor.RED + "Can not transfer to the same server you are already on");
			return;
		}

		/* If any return params were specified, mark them on the player */
		if (returnLoc != null || returnYaw != null || returnPitch != null) {
			DataEventListener.setPlayerReturnParams(player, returnLoc, returnYaw, returnPitch);
		}

		PlayerServerTransferEvent event = new PlayerServerTransferEvent(player, target);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		player.sendMessage(ChatColor.GOLD + "Transferring you to " + target);

		savePlayer(player);

		/* Lock player during transfer and prevent data saving when they log out */
		DataEventListener.setPlayerAsTransferring(player);

		DataEventListener.waitForPlayerToSaveThenSync(player, () -> {
			/*
			 * Use plugin messages to tell bungee to transfer the player.
			 * This is nice because in the event of multiple bungeecord's,
			 * it'll use the one the player is connected to.
			 */
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF(target);

			player.sendPluginMessage(mrs, "BungeeCord", out.toByteArray());
		});

		mrs.getLogger().fine(() -> "Transferring players took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds on main thread");
	}

	public static void stashPut(Player player, @Nullable String name) throws Exception {
		savePlayer(player);

		DataEventListener.waitForPlayerToSaveThenAsync(player, () -> {
			List<RedisFuture<?>> futures = new ArrayList<>();

			RedisAPI api = RedisAPI.getInstance();

			String saveName = name;
			if (saveName == null) {
				saveName = player.getUniqueId().toString();
			} else {
				futures.add(api.async().sadd(getStashListPath(), saveName));
			}

			try {
				/* Read the most-recent player data save, and copy it to the stash */
				//TODO not sure what the correct use case for this function is... is saving the current profile sufficient?
				PlayerProfileManager ppm = new PlayerProfileManager(player);

				RedisFuture<byte[]> dataFuture = api.asyncStringBytes().lindex(ppm.getRedisDataPath(), 0);
				RedisFuture<String> advanceFuture = api.async().lindex(ppm.getRedisAdvancementsPath(), 0);
				RedisFuture<String> scoreFuture = api.async().lindex(ppm.getRedisScoresPath(), 0);
				RedisFuture<String> pluginFuture = api.async().lindex(ppm.getRedisPluginDataPath(), 0);
				RedisFuture<String> historyFuture = api.async().lindex(ppm.getRedisHistoryPath(), 0);

				futures.add(api.asyncStringBytes().hset(getStashPath(), saveName + "-data", dataFuture.get()));
				futures.add(api.async().hset(getStashPath(), saveName + "-scores", scoreFuture.get()));
				futures.add(api.async().hset(getStashPath(), saveName + "-advancements", advanceFuture.get()));
				futures.add(api.async().hset(getStashPath(), saveName + "-plugins", pluginFuture.get()));
				futures.add(api.async().hset(getStashPath(), saveName + "-history", historyFuture.get()));

				if (!LettuceFutures.awaitAll(TIMEOUT_SECONDS, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]))) {
					MonumentaRedisSync.getInstance().getLogger().severe("Got timeout waiting to commit stash data for player '" + player.getName() + "'");
					player.sendMessage(ChatColor.RED + "Got timeout trying to commit stash data");
					return;
				}
			} catch (InterruptedException | ExecutionException ex) {
				MonumentaRedisSync.getInstance().getLogger().severe("Got exception while committing stash data for player '" + player.getName() + "'");
				ex.printStackTrace();
				player.sendMessage(ChatColor.RED + "Failed to save stash data: " + ex.getMessage());
				return;
			}

			player.sendMessage(ChatColor.GOLD + "Data, scores, advancements saved to stash successfully");
		});
	}

	public static void stashGet(Player player, @Nullable String name) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		/*
		 * Save player in case this was a mistake so they can get back
		 * This also saves per-shard data like location
		 */
		savePlayer(player);

		/* Lock player during stash get */
		DataEventListener.setPlayerAsTransferring(player);

		/* Wait for save to complete */
		DataEventListener.waitForPlayerToSaveThenAsync(player, () -> {
			List<RedisFuture<?>> futures = new ArrayList<>();

			RedisAPI api = RedisAPI.getInstance();

			String saveName = name;
			if (saveName == null) {
				saveName = player.getUniqueId().toString();
			}

			try {
				/* Read from the stash, and push it to the player's data */

				RedisFuture<byte[]> dataFuture = api.asyncStringBytes().hget(getStashPath(), saveName + "-data");
				RedisFuture<String> advanceFuture = api.async().hget(getStashPath(), saveName + "-advancements");
				RedisFuture<String> scoreFuture = api.async().hget(getStashPath(), saveName + "-scores");
				RedisFuture<String> pluginFuture = api.async().hget(getStashPath(), saveName + "-plugins");
				RedisFuture<String> historyFuture = api.async().hget(getStashPath(), saveName + "-history");

				/* Make sure there's actually data */
				if (dataFuture.get() == null || advanceFuture.get() == null || scoreFuture.get() == null || pluginFuture.get() == null || historyFuture.get() == null) {
					if (name == null) {
						player.sendMessage(ChatColor.RED + "You don't have any stash data");
					} else {
						player.sendMessage(ChatColor.RED + "No stash data found for '" + name + "'");
					}
					return;
				}

				PlayerProfileManager ppm = new PlayerProfileManager(player);

				futures.add(api.asyncStringBytes().lpush(ppm.getRedisDataPath(), dataFuture.get()));
				futures.add(api.async().lpush(ppm.getRedisAdvancementsPath(), advanceFuture.get()));
				futures.add(api.async().lpush(ppm.getRedisScoresPath(), scoreFuture.get()));
				futures.add(api.async().lpush(ppm.getRedisPluginDataPath(), pluginFuture.get()));
				futures.add(api.async().lpush(ppm.getRedisHistoryPath(), "stash@" + historyFuture.get()));

				if (!LettuceFutures.awaitAll(TIMEOUT_SECONDS, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]))) {
					MonumentaRedisSync.getInstance().getLogger().severe("Got timeout loading stash data for player '" + player.getName() + "'");
					player.sendMessage(ChatColor.RED + "Got timeout loading stash data");
					return;
				}
			} catch (InterruptedException | ExecutionException ex) {
				MonumentaRedisSync.getInstance().getLogger().severe("Got exception while loading stash data for player '" + player.getName() + "'");
				ex.printStackTrace();
				player.sendMessage(ChatColor.RED + "Failed to load stash data: " + ex.getMessage());
				return;
			}

			/* Kick the player on the main thread to force rejoin */
			Bukkit.getServer().getScheduler().runTask(mrs, () -> player.kick(Component.text("Stash data loaded successfully")));
		});
	}

	public static void stashInfo(Player player, @Nullable String name) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		RedisAPI api = RedisAPI.getInstance();

		String saveName;
		if (name != null) {
			saveName = name;
		} else {
			saveName = player.getUniqueId().toString();
		}

		Bukkit.getScheduler().runTaskAsynchronously(mrs, () -> {
			String history = api.sync().hget(getStashPath(), saveName + "-history");
			Bukkit.getScheduler().runTask(mrs, () -> {
				if (history == null) {
					if (name == null) {
						player.sendMessage(ChatColor.RED + "You don't have any stash data");
					} else {
						player.sendMessage(ChatColor.RED + "No stash data found for '" + name + "'");
					}
					return;
				}

				String[] split = history.split("\\|");
				if (split.length != 3) {
					player.sendMessage(ChatColor.RED + "Got corrupted history with " + split.length + " entries: " + history);
					return;
				}

				if (name == null) {
					player.sendMessage(ChatColor.GOLD + "Stash last saved on " + split[0] + " " + getTimeDifferenceSince(Long.parseLong(split[1])) + " ago");
				} else {
					player.sendMessage(ChatColor.GOLD + "Stash '" + name + "' last saved on " + split[0] + " by " + split[2] + " " + getTimeDifferenceSince(Long.parseLong(split[1])) + " ago");
				}
			});
		});
	}

	//TODO maybe this should go in PlayerProfileManager?
	//TODO dont use sync
	//TODO dont kick player
	public static void playerChangeProfile(Player player, int profileIndex) throws Exception {
		savePlayer(player); // not sure if this is necessary -> saves happen automatically when you are kicked right?
		RedisAPI.getInstance().sync().set(getRedisProfilePath(player.getUniqueId()), String.valueOf(profileIndex));
		DataEventListener.setPlayerAsTransferring(player);

		DataEventListener.waitForPlayerToSaveThenAsync(player, () -> {
			Bukkit.getServer().getScheduler().runTask(MonumentaRedisSync.getInstance(), () -> player.kick(Component.text("Your player data has been rolled back, and you can now re-join the server")));
		});
	}

	public static void playerRollback(Player moderator, Player player, int historyIndex) throws Exception {
		int currentProfile = new PlayerProfileManager(player).getProfileIndex();
		playerRollback(moderator, player, currentProfile, currentProfile, historyIndex);
	}

	public static void playerRollback(Player moderator, Player player, int profileTo, int profileFrom, int historyIndex) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		/*
		 * Save player in case this was a mistake so they can get back
		 * This also saves per-shard data like location
		 */
		savePlayer(player);

		/* Now that data has saved, the index we want to roll back to is +1 older */
		final int rollbackIndex = historyIndex + 1;

		/* Lock player during rollback */
		DataEventListener.setPlayerAsTransferring(player);

		/* Wait for save to complete */
		DataEventListener.waitForPlayerToSaveThenAsync(player, () -> {
			List<RedisFuture<?>> futures = new ArrayList<>();

			RedisAPI api = RedisAPI.getInstance();

			UUID uuid = player.getUniqueId();
			try {
				/* Read the history element and push it to the player's data */

				RedisFuture<byte[]> dataFuture = api.asyncStringBytes().lindex(getRedisDataPath(uuid, profileFrom), rollbackIndex);
				RedisFuture<String> advanceFuture = api.async().lindex(getRedisAdvancementsPath(uuid, profileFrom), rollbackIndex);
				RedisFuture<String> scoreFuture = api.async().lindex(getRedisScoresPath(uuid, profileFrom), rollbackIndex);
				RedisFuture<String> pluginFuture = api.async().lindex(getRedisPluginDataPath(uuid, profileFrom), rollbackIndex);
				RedisFuture<String> historyFuture = api.async().lindex(getRedisHistoryPath(uuid, profileFrom), rollbackIndex);

				/* Make sure there's actually data */
				if (dataFuture.get() == null || advanceFuture.get() == null || scoreFuture.get() == null || pluginFuture.get() == null || historyFuture.get() == null) {
					moderator.sendMessage(ChatColor.RED + "Failed to retrieve player's rollback data");
					return;
				}

				futures.add(api.asyncStringBytes().lpush(MonumentaRedisSyncAPI.getRedisDataPath(uuid, profileTo), dataFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisAdvancementsPath(uuid, profileTo), advanceFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisScoresPath(uuid, profileTo), scoreFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisPluginDataPath(uuid, profileTo), pluginFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisHistoryPath(uuid, profileTo), "rollback@" + historyFuture.get()));

				if (!LettuceFutures.awaitAll(TIMEOUT_SECONDS, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]))) {
					MonumentaRedisSync.getInstance().getLogger().severe("Got timeout loading rollback data for player '" + player.getName() + "'");
					moderator.sendMessage(ChatColor.RED + "Got timeout loading rollback data");
					return;
				}
			} catch (InterruptedException | ExecutionException ex) {
				MonumentaRedisSync.getInstance().getLogger().severe("Got exception while loading rollback data for player '" + player.getName() + "'");
				ex.printStackTrace();
				moderator.sendMessage(ChatColor.RED + "Failed to load rollback data: " + ex.getMessage());
				return;
			}

			moderator.sendMessage(ChatColor.GREEN + "Player " + player.getName() + " rolled back successfully");

			/* Kick the player on the main thread to force rejoin */
			Bukkit.getServer().getScheduler().runTask(mrs, () -> player.kick(Component.text("Your player data has been rolled back, and you can now re-join the server")));
		});
	}

	public static void playerLoadFromPlayer(Player loadto, Player loadfrom, int profileTo, int profileFrom, int index) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync is not loaded!");
		}

		/*
		 * Save player in case this was a mistake so they can get back
		 * This also saves per-shard data like location
		 */
		savePlayer(loadto);

		/* Lock player during load */
		DataEventListener.setPlayerAsTransferring(loadto);

		/* Wait for save to complete */
		DataEventListener.waitForPlayerToSaveThenAsync(loadto, () -> {
			List<RedisFuture<?>> futures = new ArrayList<>();

			RedisAPI api = RedisAPI.getInstance();
			UUID uuidFrom = loadfrom.getUniqueId();
			UUID uuidTo = loadto.getUniqueId();
			try {
				/* Read the history element and push it to the player's data */

				RedisFuture<byte[]> dataFuture = api.asyncStringBytes().lindex(getRedisDataPath(uuidFrom, profileFrom), index);
				RedisFuture<String> advanceFuture = api.async().lindex(getRedisAdvancementsPath(uuidFrom, profileFrom), index);
				RedisFuture<String> scoreFuture = api.async().lindex(getRedisScoresPath(uuidFrom, profileFrom), index);
				RedisFuture<String> pluginFuture = api.async().lindex(getRedisPluginDataPath(uuidFrom, profileFrom), index);
				RedisFuture<String> historyFuture = api.async().lindex(getRedisHistoryPath(uuidFrom, profileFrom), index);

				/* Make sure there's actually data */
				if (dataFuture.get() == null || advanceFuture.get() == null || scoreFuture.get() == null || pluginFuture.get() == null || historyFuture.get() == null) {
					loadto.sendMessage(ChatColor.RED + "Failed to retrieve player's data to load");
					return;
				}

				futures.add(api.asyncStringBytes().lpush(MonumentaRedisSyncAPI.getRedisDataPath(uuidTo, profileTo), dataFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisAdvancementsPath(uuidTo, profileTo), advanceFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisScoresPath(uuidTo, profileTo), scoreFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisPluginDataPath(uuidTo, profileTo), pluginFuture.get()));
				futures.add(api.async().lpush(MonumentaRedisSyncAPI.getRedisHistoryPath(uuidTo, profileTo), "loadfrom@" + loadfrom.getName() + "@" + historyFuture.get()));

				if (!LettuceFutures.awaitAll(TIMEOUT_SECONDS, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]))) {
					MonumentaRedisSync.getInstance().getLogger().severe("Got timeout loading data for player '" + loadfrom.getName() + "'");
					loadto.sendMessage(ChatColor.RED + "Got timeout loading data");
					return;
				}
			} catch (InterruptedException | ExecutionException ex) {
				MonumentaRedisSync.getInstance().getLogger().severe("Got exception while loading data for player '" + loadfrom.getName() + "'");
				ex.printStackTrace();
				loadto.sendMessage(ChatColor.RED + "Failed to load data: " + ex.getMessage());
				return;
			}

			/* Kick the player on the main thread to force rejoin */
			Bukkit.getServer().getScheduler().runTask(mrs, () -> loadto.kick(Component.text("Data loaded from player " + loadfrom.getName() + " at index " + index + " and you can now re-join the server")));
		});
	}

	public static String getRedisProfilePath(UUID uuid) {
		return String.format("%s:playerdata:%s:profile", ConfigAPI.getServerDomain(), uuid.toString());
	}

	public static String getRedisDataPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:data", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisHistoryPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:history", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisPerShardDataPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:sharddata", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisPluginDataPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:plugins", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisAdvancementsPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:advancements", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisScoresPath(UUID uuid, int profileIndex) {
		return String.format("%s:playerdata:%s:profile_%s:scores", ConfigAPI.getServerDomain(), uuid.toString(), profileIndex);
	}

	public static String getRedisPerShardDataWorldKey(World world) {
		return getRedisPerShardDataWorldKey(world.getUID(), world.getName());
	}

	public static String getRedisPerShardDataWorldKey(UUID worldUUID, String worldName) {
		return worldUUID.toString() + ":" + worldName;
	}

	public static String getStashPath() {
		return String.format("%s:stash", ConfigAPI.getServerDomain());
	}

	public static String getStashListPath() {
		return String.format("%s:stashlist", ConfigAPI.getServerDomain());
	}

	public static String getTimeDifferenceSince(long compareTime) {
		final long diff = System.currentTimeMillis() - compareTime;
		final long diffSeconds = diff / 1000 % 60;
		final long diffMinutes = diff / (60 * 1000) % 60;
		final long diffHours = diff / (60 * 60 * 1000) % 24;
		final long diffDays = diff / (24 * 60 * 60 * 1000);

		String timeStr = "";
		if (diffDays > 0) {
			timeStr += diffDays + " day";
			if (diffDays > 1) {
				timeStr += "s";
			}
		}

		if (diffDays > 0 && (diffHours > 0 || diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffHours > 0) {
			timeStr += diffHours + " hour";
			if (diffHours > 1) {
				timeStr += "s";
			}
		}

		if (diffHours > 0 && (diffMinutes > 0 || diffSeconds > 0)) {
			timeStr += " ";
		}

		if (diffMinutes > 0) {
			timeStr += diffMinutes + " minute";
			if (diffMinutes > 1) {
				timeStr += "s";
			}
		}

		if (diffMinutes > 0 && diffSeconds > 0 && (diffDays == 0 && diffHours == 0)) {
			timeStr += " ";
		}

		if (diffSeconds > 0 && (diffDays == 0 && diffHours == 0)) {
			timeStr += diffSeconds + " second";
			if (diffSeconds > 1) {
				timeStr += "s";
			}
		}

		return timeStr;
	}

	/**
	 * Saves all of player's data, including advancements, scores, plugin data, inventory, world location, etc.
	 *
	 * Also creates a rollback point like all full saves.
	 *
	 * Takes several milliseconds so care should be taken not to call this too frequently
	 */
	public static void savePlayer(Player player) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync invoked but is not loaded");
		}

		try {
			mrs.getVersionAdapter().savePlayer(player);
		} catch (Exception ex) {
			String message = "Failed to save player data for player '" + player.getName() + "'";
			mrs.getLogger().severe(message);
			throw new Exception(message, ex);
		}
	}

	/**
	 * Gets player plugin data from the cache.
	 *
	 * Only valid if the player is currently on this shard.
	 *
	 * @param uuid              Player's UUID to get data for
	 * @param pluginIdentifier  A unique string key identifying which plugin data to get for this player
	 *
	 * @return plugin data for this identifier (or null if it doesn't exist or player isn't online)
	 */
	public static @Nullable JsonObject getPlayerPluginData(UUID uuid, String pluginIdentifier) {
		JsonObject pluginData = DataEventListener.getPlayerPluginData(uuid);
		if (pluginData == null || !pluginData.has(pluginIdentifier)) {
			return null;
		}

		JsonElement pluginDataElement = pluginData.get(pluginIdentifier);
		if (!pluginDataElement.isJsonObject()) {
			return null;
		}

		return pluginDataElement.getAsJsonObject();
	}

	public static class PlayerWorldData {
		// Other sharddata fields that are not returned here: {"SpawnDimension":"minecraft:overworld","Dimension":0,"Paper.Origin":[-1450.0,241.0,-1498.0]}"}
		// Note: This list might be out of date

		private final Location mSpawnLoc; // {"SpawnX":-1450,"SpawnY":241,"SpawnZ":-1498,"SpawnAngle":0.0}
		private final Location mPlayerLoc; // {"Pos":[-1280.5,95.0,5369.7001953125],"Rotation":[-358.9,2.1]}
		private final Vector mMotion; // {"Motion":[0.0,-0.0784000015258789,0.0]}
		private final boolean mSpawnForced; // {"SpawnForced":true}
		private final boolean mFlying; // {"flying":false}
		private final boolean mFallFlying; // {"FallFlying":false}
		private final float mFallDistance; // {"FallDistance":0.0}
		private final boolean mOnGround; // {"OnGround":true}

		private PlayerWorldData(Location spawnLoc, Location playerLoc, Vector motion, boolean spawnForced, boolean flying, boolean fallFlying, float fallDistance, boolean onGround) {
			mSpawnLoc = spawnLoc;
			mPlayerLoc = playerLoc;
			mMotion = motion;
			mSpawnForced = spawnForced;
			mFlying = flying;
			mFallFlying = fallFlying;
			mFallDistance = fallDistance;
			mOnGround = onGround;
		}

		public Location getSpawnLoc() {
			return mSpawnLoc;
		}

		public Location getPlayerLoc() {
			return mPlayerLoc;
		}

		public Vector getMotion() {
			return mMotion;
		}

		public boolean getFallFlying() {
			return mFallFlying;
		}

		public double getFallDistance() {
			return mFallDistance;
		}

		public boolean getOnGround() {
			return mOnGround;
		}

		public void applyToPlayer(Player player) {
			player.teleport(mPlayerLoc);
			player.setVelocity(mMotion);
			player.setFlying(mFlying && player.getAllowFlight());
			player.setGliding(mFallFlying);
			player.setFallDistance(mFallDistance);
			player.setBedSpawnLocation(mSpawnLoc, mSpawnForced);
		}

		private static PlayerWorldData fromJson(@Nullable String jsonStr, World world) {
			// Defaults to world spawn
			Location spawnLoc = world.getSpawnLocation();
			Location playerLoc = spawnLoc.clone();
			Vector motion = new Vector(0, 0, 0);
			boolean spawnForced = true;
			boolean flying = false;
			boolean fallFlying = false;
			float fallDistance = 0;
			boolean onGround = true;

			if (jsonStr != null && !jsonStr.isEmpty()) {
				try {
					JsonObject obj = new Gson().fromJson(jsonStr, JsonObject.class);
					if (obj.has("SpawnX")) {
						spawnLoc.setX(obj.get("SpawnX").getAsDouble());
					}
					if (obj.has("SpawnY")) {
						spawnLoc.setY(obj.get("SpawnY").getAsDouble());
					}
					if (obj.has("SpawnZ")) {
						spawnLoc.setZ(obj.get("SpawnZ").getAsDouble());
					}
					if (obj.has("Pos")) {
						JsonArray arr = obj.get("Pos").getAsJsonArray();
						playerLoc.setX(arr.get(0).getAsDouble());
						playerLoc.setY(arr.get(1).getAsDouble());
						playerLoc.setZ(arr.get(2).getAsDouble());
					}
					if (obj.has("Rotation")) {
						JsonArray arr = obj.get("Rotation").getAsJsonArray();
						playerLoc.setYaw(arr.get(0).getAsFloat());
						playerLoc.setPitch(arr.get(1).getAsFloat());
					}
					if (obj.has("Motion")) {
						JsonArray arr = obj.get("Motion").getAsJsonArray();
						motion = new Vector(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble());
					}
					if (obj.has("SpawnForced")) {
						spawnForced = obj.get("SpawnForced").getAsBoolean();
					}
					if (obj.has("flying")) {
						flying = obj.get("flying").getAsBoolean();
					}
					if (obj.has("FallFlying")) {
						fallFlying = obj.get("FallFlying").getAsBoolean();
					}
					if (obj.has("FallDistance")) {
						fallDistance = obj.get("FallDistance").getAsFloat();
					}
					if (obj.has("OnGround")) {
						onGround = obj.get("OnGround").getAsBoolean();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			return new PlayerWorldData(spawnLoc, playerLoc, motion, spawnForced, flying, fallFlying, fallDistance, onGround);
		}
	}

	/**
	 * Gets player location data for a world
	 * <p>
	 * Only valid if the player is currently on this shard.
	 *
	 * @param player  Player's to get data for
	 * @param world   World to get data for
	 *
	 * @return plugin data for this identifier (or null if it doesn't exist or player isn't online)
	 */
	public static PlayerWorldData getPlayerWorldData(Player player, World world) {
		Map<String, String> shardData = DataEventListener.getPlayerShardData(player.getUniqueId());
		if (shardData == null || shardData.isEmpty()) {
			return PlayerWorldData.fromJson(null, world);
		}

		String worldShardData = shardData.get(getRedisPerShardDataWorldKey(world));
		if (worldShardData == null || worldShardData.isEmpty()) {
			return PlayerWorldData.fromJson(null, world);
		}

		return PlayerWorldData.fromJson(worldShardData, world);
	}

	/** Future returns non-null if successfully loaded data, null on error */
	@Nullable
	private static RedisPlayerData transformPlayerData(MonumentaRedisSync mrs, UUID uuid, TransactionResult result) {
		if (result.isEmpty() || result.size() == 0 || result.get(0) == null) {
			mrs.getLogger().warning("Failed to retrieve player data; likely player didn't make it past the tutorial");
			return null;
		}

		if (result.size() != 5) {
			mrs.getLogger().severe("Failed to retrieve player data; only " + result.size() + " / 5 expected data elements retrieved");
			return null;
		}

		try {
			String advancements;
			String scores;
			String pluginData;
			String history;

			byte[] data = result.get(0);

			if (result.get(1) == null) {
				mrs.getLogger().severe("Player advancements data was missing or corrupted and has been reset");
				advancements = "{}";
			} else {
				advancements = new String(result.get(1), StandardCharsets.UTF_8);
			}

			if (result.get(2) == null) {
				mrs.getLogger().severe("Player scores data was missing or corrupted and has been reset");
				scores = "{}";
			} else {
				scores = new String(result.get(2), StandardCharsets.UTF_8);
			}

			if (result.get(3) == null) {
				mrs.getLogger().warning("Player pluginData was missing or corrupted and has been reset");
				pluginData = "{}";
			} else {
				pluginData = new String(result.get(3), StandardCharsets.UTF_8);
			}

			if (result.get(4) == null) {
				mrs.getLogger().warning("Player history data was missing or corrupted and has been reset");
				history = "UpdateAllPlayers|" + System.currentTimeMillis() + "|unknown";
			} else {
				history = new String(result.get(4), StandardCharsets.UTF_8);
			}

			return new RedisPlayerData(uuid, mrs.getVersionAdapter().retrieveSaveData(data, new JsonObject()), advancements, scores, pluginData, history);
		} catch (Exception e) {
			mrs.getLogger().severe("Failed to parse player data: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static CompletableFuture<RedisPlayerData> getOfflinePlayerData(UUID uuid) throws Exception {
		return getOfflinePlayerData(uuid, new PlayerProfileManager(uuid).getProfileIndex());
	}

	public static CompletableFuture<RedisPlayerData> getOfflinePlayerData(UUID uuid, int profileIndex) throws Exception {
		if (Bukkit.getPlayer(uuid) != null) {
			throw new Exception("Player " + uuid + " is online");
		}

		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync invoked but is not loaded");
		}

		PlayerProfileManager profileManager = new PlayerProfileManager(uuid);

		RedisAsyncCommands<String, byte[]> commands = RedisAPI.getInstance().asyncStringBytes();
		commands.multi();

		commands.lindex(MonumentaRedisSyncAPI.getRedisDataPath(uuid, profileIndex), 0);
		commands.lindex(MonumentaRedisSyncAPI.getRedisAdvancementsPath(uuid, profileIndex), 0);
		commands.lindex(MonumentaRedisSyncAPI.getRedisScoresPath(uuid, profileIndex), 0);
		commands.lindex(MonumentaRedisSyncAPI.getRedisPluginDataPath(uuid, profileIndex), 0);
		commands.lindex(MonumentaRedisSyncAPI.getRedisHistoryPath(uuid, profileIndex), 0);

		return commands.exec().thenApply((TransactionResult result) -> transformPlayerData(mrs, uuid, result)).toCompletableFuture();
	}

	/**
	 * Gets a map of all player scoreboard values.
	 * <p>
	 * If player is online, will pull them from the current scoreboard. This work will be done on the main thread (will take several milliseconds).
	 * If player is offline, will pull them from the most recent redis save on an async thread, then compose them into a map (basically no main thread time)
	 * <p>
	 * The return future will always complete on the main thread with either results or an exception.
	 * Suggest chaining on .whenComplete((data, ex) -> your code) to do something with this data when complete
	 */
	public static CompletableFuture<Map<String, Integer>> getPlayerScores(UUID uuid) {
		CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();

		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			future.completeExceptionally(new Exception("MonumentaRedisSync invoked but is not loaded"));
			return future;
		}

		Player player = Bukkit.getPlayer(uuid);
		if (player != null) {
			Map<String, Integer> scores = new HashMap<>();
			for (Objective objective : Bukkit.getScoreboardManager().getMainScoreboard().getObjectives()) {
				Score score = objective.getScore(player.getName());
				scores.put(objective.getName(), score.getScore());
			}
			future.complete(scores);
			return future;
		}

		RedisAsyncCommands<String, String> commands = RedisAPI.getInstance().async();

		commands.lindex(MonumentaRedisSyncAPI.getRedisScoresPath(uuid, new PlayerProfileManager(uuid).getProfileIndex()), 0)
			.thenApply(
				(scoreData) -> new Gson().fromJson(scoreData, JsonObject.class).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (entry) -> entry.getValue().getAsInt())))
			.whenComplete((scoreMap, ex) -> {
				Bukkit.getScheduler().runTask(mrs, () -> {
					if (ex != null) {
						future.completeExceptionally(ex);
					} else {
						future.complete(scoreMap);
					}
				});
			});

		return future;
	}

	private static Boolean transformPlayerSaveResult(MonumentaRedisSync mrs, TransactionResult result) {
		if (result.isEmpty() || result.size() != 5 || result.get(0) == null
		    || result.get(1) == null || result.get(2) == null || result.get(3) == null || result.get(4) == null) {
			mrs.getLogger().severe("Failed to commit player data");
			return false;
		}

		return true;
	}

	public static CompletableFuture<Boolean> saveOfflinePlayerData(RedisPlayerData data) throws Exception {
		return saveOfflinePlayerData(data, new PlayerProfileManager(data.getUniqueId()).getProfileIndex());
	}

	/** Future returns true if successfully committed, false if not */
	public static CompletableFuture<Boolean> saveOfflinePlayerData(RedisPlayerData data, int profileIndex) throws Exception {
		MonumentaRedisSync mrs = MonumentaRedisSync.getInstance();
		if (mrs == null) {
			throw new Exception("MonumentaRedisSync invoked but is not loaded");
		}

		RedisAsyncCommands<String, byte[]> commands = RedisAPI.getInstance().asyncStringBytes();
		commands.multi();

		SaveData splitData = mrs.getVersionAdapter().extractSaveData(data.getNbtTagCompoundData(), null);
		commands.lpush(MonumentaRedisSyncAPI.getRedisDataPath(data.getUniqueId(), profileIndex), splitData.getData());
		commands.lpush(MonumentaRedisSyncAPI.getRedisAdvancementsPath(data.getUniqueId(), profileIndex), data.getAdvancements().getBytes(StandardCharsets.UTF_8));
		commands.lpush(MonumentaRedisSyncAPI.getRedisScoresPath(data.getUniqueId(), profileIndex), data.getScores().getBytes(StandardCharsets.UTF_8));
		commands.lpush(MonumentaRedisSyncAPI.getRedisPluginDataPath(data.getUniqueId(), profileIndex), data.getPluginData().getBytes(StandardCharsets.UTF_8));
		commands.lpush(MonumentaRedisSyncAPI.getRedisHistoryPath(data.getUniqueId(), profileIndex), data.getHistory().getBytes(StandardCharsets.UTF_8));

		return commands.exec().thenApply((TransactionResult result) -> transformPlayerSaveResult(mrs, result)).toCompletableFuture();
	}

	/*
	 * rboard API
	 *********************************************************************************/

	/**
	 * If MonumentaNetworkRelay is installed, returns a list of all other shard names
	 * that are currently up and valid transfer targets from this server.
	 * <p>
	 * If MonumentaNetworkRelay is not installed, returns an empty array.
	 */
	public static String[] getOnlineTransferTargets() {
		return NetworkRelayIntegration.getOnlineTransferTargets();
	}

	/**
	 * Runs the result of an asynchronous transaction on the main thread after it is completed
	 * <p>
	 * Will always call the callback function eventually, even if the resulting transaction fails or is lost.
	 * <p>
	 * When the function is called, either data will be non-null and exception null,
	 * or data will be null and the exception will be non-null
	 */
	public static <T> void runOnMainThreadWhenComplete(Plugin plugin, CompletableFuture<T> future, BiConsumer<T, Throwable> func) {
		future.whenComplete((T result, Throwable ex) -> {
			Bukkit.getScheduler().runTask(plugin, () -> {
				func.accept(result, ex);
			});
		});
	}
}
