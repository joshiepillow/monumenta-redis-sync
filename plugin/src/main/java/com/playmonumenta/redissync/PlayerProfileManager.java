package com.playmonumenta.redissync;

import org.bukkit.entity.Player;

import java.util.UUID;

//TODO in order of difficulty
// add some way of knowing what profiles exist -> should be easy to just attach as a hashmap to getRedisProfilePath
// edit a lot of commands to deal with profiles
// global data like market bans guh
public class PlayerProfileManager {
	private final UUID mUUID;
	private Integer mProfileIndex;

	//TODO make this a singleton with a hashmap from Player to ID or something so that the redis fetch doesn't happen everytime
	// just need to be careful to release data when player logs out to not have memory leaks
	// (though tbf this memory leak would have barely any impact since it would just be UUID int pairs)
	public PlayerProfileManager(Player player) {
		mUUID = player.getUniqueId();
	}
	public PlayerProfileManager(UUID uuid) {
		mUUID = uuid;
	}

	/**
	 * Load active profile index from redis data
	 * @return active profile index
	 */
	//TODO get rid of sync
	public int loadProfileIndex() {
		String out = RedisAPI.getInstance().sync().get(MonumentaRedisSyncAPI.getRedisProfilePath(mUUID));
		if (out == null) {
			mProfileIndex = 0;
		} else {
			mProfileIndex = Integer.parseInt(out);
		}
		return mProfileIndex;
	}

	/**
	 * Return locally stored profile index if loaded already, otherwise load from redis
	 * @return active profile index
	 */
	public int getProfileIndex() {
		return mProfileIndex == null ? loadProfileIndex() : mProfileIndex;
	}

	// Moved to MonumentaRedisSyncAPI -- not sure where it should be

//	/**
//	 * Change the active profile instance and save that change to redis
//	 * @param index new active profile
//	 */
//	//TODO batch all saves at savePlayer, get rid of sync
//	//TODO do something more advanced than kicking the player LMAO
//	 public void changeProfileIndex(int index) {
//		mProfileIndex = index;
//		RedisAPI.getInstance().sync().set(MonumentaRedisSyncAPI.getRedisProfilePath(mUUID), String.valueOf(index));
//	}

	public String getRedisDataPath() {
		return MonumentaRedisSyncAPI.getRedisDataPath(mUUID, getProfileIndex());
	}

	public String getRedisHistoryPath() {
		return MonumentaRedisSyncAPI.getRedisHistoryPath(mUUID, getProfileIndex());
	}

	public String getRedisPerShardDataPath() {
		return MonumentaRedisSyncAPI.getRedisPerShardDataPath(mUUID, getProfileIndex());
	}

	public String getRedisPluginDataPath() {
		return MonumentaRedisSyncAPI.getRedisPluginDataPath(mUUID, getProfileIndex());
	}

	public String getRedisAdvancementsPath() {
		return MonumentaRedisSyncAPI.getRedisAdvancementsPath(mUUID, getProfileIndex());
	}

	public String getRedisScoresPath() {
		return MonumentaRedisSyncAPI.getRedisScoresPath(mUUID, getProfileIndex());
	}
}
