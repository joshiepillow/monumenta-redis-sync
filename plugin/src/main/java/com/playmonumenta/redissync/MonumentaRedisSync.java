package com.playmonumenta.redissync;

import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.redissync.adapters.RedisDedicatedPlayerList1132;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MonumentaRedisSync extends JavaPlugin {
	public static JedisPool pool;
	private static MonumentaRedisSync INSTANCE = null;

	public static MonumentaRedisSync getInstance() {
		return INSTANCE;
	}

	@Override
	public void onLoad() {
		/* Replace the DedicatedServerList as early as possible during load */
		try {
			RedisDedicatedPlayerList1132.inject(getLogger());
			INSTANCE = this;
		} catch (Exception ex) {
			getLogger().severe("Failed to inject player list: " + ex.getMessage());
			ex.printStackTrace();
		}

		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */
	}

	@Override
	public void onEnable() {
		pool = new JedisPool(new JedisPoolConfig(), "redis", 6379);
	}

	@Override
	public void onDisable() {
		pool.close();
		INSTANCE = null;
		getServer().getScheduler().cancelTasks(this);
	}
}
