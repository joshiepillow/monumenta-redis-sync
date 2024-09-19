package com.playmonumenta.redissync;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

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
