package com.playmonumenta.redissync;

import java.io.File;
import java.io.IOException;

import com.playmonumenta.redissync.adapters.VersionAdapter;
import com.playmonumenta.redissync.adapters.VersionAdapter113;
import com.playmonumenta.redissync.api.RedisAPI;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

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
		loadConfig();
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

	private void loadConfig() {
		File configFile = new File(this.getDataFolder(), "config.yml");
		/* TODO: Default file if not exist */
		String host = "redis";
		int port = 6379;
		String domain = null;
		String shard = "bungee";
		try {
			Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
			host = config.getString("redis_host", host);
			port = config.getInt("redis_port", port);
		} catch (IOException ex) {
			this.getLogger().warning("Failed to load config file " + configFile.getPath() + " : " + ex.getMessage());
			return;
		}
		new Conf(host, port, domain, shard);
	}
}
