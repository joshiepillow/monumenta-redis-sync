package com.playmonumenta.redissync;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.redissync.adapters.VersionAdapter;
import com.playmonumenta.redissync.adapters.VersionAdapter113;
import com.playmonumenta.redissync.api.RedisAPI;
import com.playmonumenta.redissync.commands.TransferServer;

public class MonumentaRedisSync extends JavaPlugin {
	private static MonumentaRedisSync INSTANCE = null;
	private RedisAPI mRedisAPI = null;
	private VersionAdapter mVersionAdapter = null;

	public static MonumentaRedisSync getInstance() {
		return INSTANCE;
	}

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */
		TransferServer.register(this);
	}

	@Override
	public void onEnable() {
		/* Needed to tell Netty where it moved to */
		System.setProperty("com.playmonumenta.redissync.internal.io.netty", "com.playmonumenta.redissync.internal");

		INSTANCE = this;
		loadConfig();
		mRedisAPI = new RedisAPI(Conf.getHost(), Conf.getPort());
		getServer().getPluginManager().registerEvents(new DataEventListener(this.getLogger()), this);
		mVersionAdapter = new VersionAdapter113();

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
	}

	@Override
	public void onDisable() {
		INSTANCE = null;
		mRedisAPI.shutdown();
		mRedisAPI = null;
		getServer().getScheduler().cancelTasks(this);
	}

	public static VersionAdapter getVersionAdapter() {
		return INSTANCE.mVersionAdapter;
	}

	private void loadConfig() {
		File configFile = new File(this.getDataFolder(), "config.yml");
		/* TODO: Default file if not exist */
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		String host = config.getString("redis_host", "redis");
		int port = config.getInt("redis_port", 6379);
		String domain = config.getString("server_domain", "default_domain");
		String shard = config.getString("shard_name", "default_shard");
		new Conf(host, port, domain, shard);
	}
}
