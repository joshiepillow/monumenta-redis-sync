package com.playmonumenta.redissync;

import com.playmonumenta.redissync.adapters.VersionAdapter;
import com.playmonumenta.redissync.adapters.VersionAdapter113;
import com.playmonumenta.redissync.api.RedisAPI;

import net.md_5.bungee.api.plugin.Plugin;

public class MonumentaRedisSyncBungee extends Plugin {
	private static MonumentaRedisSyncBungee INSTANCE = null;
	private RedisAPI mRedisAPI = null;
	private VersionAdapter mVersionAdapter = null;

	public static MonumentaRedisSyncBungee getInstance() {
		return INSTANCE;
	}

	@Override
	public void onEnable() {
		INSTANCE = this;
		new Conf(this.getDataFolder(), true);
		mRedisAPI = new RedisAPI(Conf.getHost(), Conf.getPort());
		mVersionAdapter = new VersionAdapter113();
	}

	@Override
	public void onDisable() {
		INSTANCE = null;
		mRedisAPI.shutdown();
		mRedisAPI = null;
	}

	public static VersionAdapter getVersionAdapter() {
		return INSTANCE.mVersionAdapter;
	}
}
