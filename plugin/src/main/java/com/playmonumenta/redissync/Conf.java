package com.playmonumenta.redissync;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Conf {
	private static Conf INSTANCE = null;

	private final String mHost;
	private final int mPort;
	private final String mDomain;
	private final String mShard;

	public static String getHost() {
		return INSTANCE.mHost;
	}

	public static int getPort() {
		return INSTANCE.mPort;
	}

	public static String getDomain() {
		return INSTANCE.mDomain;
	}

	public static String getShard() {
		return INSTANCE.mShard;
	}

	public Conf(File dataFolder, boolean isBungee) {
		File configFile = new File(dataFolder, "config.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		mHost = config.getString("redis_host", "redis");
		mPort = config.getInt("redis_port", 6379);
		mDomain = config.getString("server_domain", "default_domain");
		mShard = isBungee ? "bungee" : config.getString("shard_name", "default_shard");

		INSTANCE = this;
	}
}
