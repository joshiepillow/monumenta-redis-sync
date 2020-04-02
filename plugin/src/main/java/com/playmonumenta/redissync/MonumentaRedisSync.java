package com.playmonumenta.redissync;

import org.bukkit.plugin.java.JavaPlugin;

public class MonumentaRedisSync extends JavaPlugin {
	private static MonumentaRedisSync INSTANCE = null;
	private RedisAPI mRedisAPI = null;

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
	}

	@Override
	public void onEnable() {
		INSTANCE = this;
		new Conf(this);
		mRedisAPI = new RedisAPI(Conf.getHost(), Conf.getPort());
		getServer().getPluginManager().registerEvents(new DataEventListener(this.getLogger()), this);
	}

	@Override
	public void onDisable() {
		INSTANCE = null;
		mRedisAPI.shutdown();
		mRedisAPI = null;
		getServer().getScheduler().cancelTasks(this);
	}
}
