package com.playmonumenta.redissync;

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
		INSTANCE = this;
		new Conf(this.getDataFolder(), false);
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
}
