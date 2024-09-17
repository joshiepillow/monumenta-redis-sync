package com.playmonumenta.redissync;

import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerProfileManager {
	private final UUID mUUID;
	private Integer mProfileIndex;
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

	/**
	 * Change the active profile instance and save that change to redis
	 * @param index new active profile
	 */
	//TODO batch all saves at savePlayer, get rid of sync
	public void changeProfileIndex(int index) {
		mProfileIndex = index;
		RedisAPI.getInstance().sync().set(MonumentaRedisSyncAPI.getRedisProfilePath(mUUID), String.valueOf(index));
	}

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
