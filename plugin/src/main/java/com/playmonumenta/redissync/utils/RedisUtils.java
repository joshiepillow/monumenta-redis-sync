package com.playmonumenta.redissync.utils;

import com.playmonumenta.redissync.MonumentaRedisSync;

import redis.clients.jedis.Jedis;

public class RedisUtils {
	public static void set(String key, String value) {
		Jedis j = null;
		try {
			j = MonumentaRedisSync.pool.getResource();
			j.set(key, value);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (j != null) {
			j.close();
		}
	}

	public static String get(String key) {
		String retVal = "";
		Jedis j = null;
		try {
			j = MonumentaRedisSync.pool.getResource();
			retVal = j.get(key);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}
}
