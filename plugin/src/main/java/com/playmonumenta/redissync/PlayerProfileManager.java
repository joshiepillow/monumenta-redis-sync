package com.playmonumenta.redissync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO in order of difficulty
// add some way of knowing what profiles exist -> should be easy to just attach as a hashmap to getRedisProfilePath
// edit a lot of commands to deal with profiles
// global data like market bans guh
public final class PlayerProfileManager {
	// prevent construction
	private PlayerProfileManager() {}

	private static final HashMap<UUID, Integer> muuidToProfile = new HashMap<>();

	/**
	 * Return active profile index if cached, otherwise call load profile index
	 * @param uuid uuid of player
	 * @return active profile index
	 */
	static int getProfileIndex(UUID uuid) {
		return muuidToProfile.containsKey(uuid) ?
			muuidToProfile.get(uuid) : loadProfileIndex(uuid);
	}

	/**
	 * Reload active profile index from redis data
	 * @param uuid uuid of player
	 * @return active profile index
	 */
	//TODO get rid of sync
	static int loadProfileIndex(UUID uuid) {
		String out = RedisAPI.getInstance().sync().get(MonumentaRedisSyncAPI.getRedisProfilePath(uuid));
		muuidToProfile.put(uuid, (out == null) ? 0 : Integer.parseInt(out));
		return muuidToProfile.get(uuid);
	}

	/**
	 * Change the active profile instance and save that change to redis
	 * @param uuid uuid of player
	 * @param index new active profile
	 */
	static void changeProfileIndex(UUID uuid, int index) {
		muuidToProfile.put(uuid, index);
		RedisAPI.getInstance().sync().set(MonumentaRedisSyncAPI.getRedisProfilePath(uuid), String.valueOf(index));
	}

	/**
	 * Remove a player's profile from cache
	 * @param uuid uuid of player
	 * @return player's current profile at time of removal
	 */
	static int removePlayer(UUID uuid) {
		return muuidToProfile.remove(uuid);
	}

	private static Stream<Map.Entry<String, Integer>> toStream(String mapAsString) {
		return new Gson().fromJson(mapAsString, JsonObject.class).entrySet().stream()
			.map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getAsInt()));
	}

	private static Map<String, Integer> filterGlobal(String mapAsString) {
		return toStream(mapAsString)
			.filter(entry -> ConfigAPI.getGlobalScoreNames().contains(entry.getKey()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Get profile scores from redis and merge them with global scores
	 * @param globalScores global scores from redis as string
	 * @param profileScores profile scores from redis as string
	 * @return all scores
	 */
	static Map<String, Integer> getScores(String globalScores, String profileScores) {
		Map<String, Integer> globalScoresMap = globalScores == null ? new HashMap<>() : filterGlobal(globalScores);
		Map<String, Integer> profileScoresMap = profileScores == null ? new HashMap<>() : toStream(profileScores)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		profileScoresMap.putAll(globalScoresMap);
		return profileScoresMap;
	}

	/**
	 * Store all scores
	 * @param uuid uuid of player
	 * @param profileIndex profile to save to
	 * @param allScores all scores attached to player
	 * @param commands multi command to add to
	 */
	static void saveScores(UUID uuid, int profileIndex, String allScores, RedisAsyncCommands<String, byte[]> commands) {
		commands.lpush(MonumentaRedisSyncAPI.getRedisScoresPath(uuid, profileIndex), allScores.getBytes(StandardCharsets.UTF_8));
		String globalScores = new Gson().toJson(filterGlobal(allScores));
		commands.lpush(MonumentaRedisSyncAPI.getRedisGlobalScoresPath(uuid), globalScores.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Why does DataEventListener do the exact same thing as MonumentaRedisSyncAPI but use String instead of byte[]
	 * Cant even overload this because of "erasure"
	 */
	static void saveScores2(UUID uuid, int profileIndex, String allScores, RedisAsyncCommands<String, String> commands) {
		commands.lpush(MonumentaRedisSyncAPI.getRedisScoresPath(uuid, profileIndex), allScores);
		String globalScores = new Gson().toJson(filterGlobal(allScores));
		commands.lpush(MonumentaRedisSyncAPI.getRedisGlobalScoresPath(uuid), globalScores);
	}

	static String getRedisDataPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisDataPath(uuid, getProfileIndex(uuid));
	}

	static String getRedisHistoryPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisHistoryPath(uuid, getProfileIndex(uuid));
	}

	static String getRedisPerShardDataPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisPerShardDataPath(uuid, getProfileIndex(uuid));
	}

	static String getRedisPluginDataPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisPluginDataPath(uuid, getProfileIndex(uuid));
	}

	static String getRedisAdvancementsPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisAdvancementsPath(uuid, getProfileIndex(uuid));
	}

	static String getRedisScoresPath(UUID uuid) {
		return MonumentaRedisSyncAPI.getRedisScoresPath(uuid, getProfileIndex(uuid));
	}
}
