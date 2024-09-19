package com.playmonumenta.redissync;

import com.playmonumenta.redissync.adapters.VersionAdapter;
import com.playmonumenta.redissync.commands.*;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MonumentaRedisSync extends JavaPlugin {
	private static @Nullable MonumentaRedisSync INSTANCE = null;
	private @Nullable RedisAPI mRedisAPI = null;
	private @Nullable VersionAdapter mVersionAdapter = null;
	private @Nullable CustomLogger mLogger = null;

	private void loadVersionAdapter() {
		/* From https://github.com/mbax/AbstractionExamplePlugin */

		String packageName = this.getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);

		try {
			final Class<?> clazz = Class.forName("com.playmonumenta.redissync.adapters.VersionAdapter_" + version);
			// Check if we have a valid adapter class at that location.
			if (VersionAdapter.class.isAssignableFrom(clazz)) {
				mVersionAdapter = (VersionAdapter) clazz.getConstructor(Logger.class).newInstance(this.getLogger());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			getLogger().severe("Server version " + version + " is not supported!");
			return;
		}
		getLogger().info("Loading support for " + version);
	}

	@Override
	public void onLoad() {
		loadVersionAdapter();

		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */
		TransferServer.register();
		Stash.register();
		PlayerHistory.register(this);
		PlayerRollback.register();
		PlayerLoadFromPlayer.register();
		UpgradeAllPlayers.register(this);
		ChangeLogLevel.register(this);
		RboardCommand.register(this);
		RemoteDataCommand.register(this);

		PlayerChangeProfile.register();
		ActivePlayerProfile.register();
	}

	@Override
	public void onEnable() {
		/* Refuse to enable without a version adapter */
		if (mVersionAdapter == null) {
			this.setEnabled(false);
			return;
		}

		/* Needed to tell Netty where it moved to */
		System.setProperty("com.playmonumenta.redissync.internal.io.netty", "com.playmonumenta.redissync.internal");

		INSTANCE = this;

		if (getServer().getPluginManager().isPluginEnabled("MonumentaNetworkRelay")) {
			try {
				getServer().getPluginManager().registerEvents(new NetworkRelayIntegration(this.getLogger()), this);
			} catch (Exception ex) {
				getLogger().severe("Failed to enable MonumentaNetworkRelay integration: " + ex.getMessage());
			}
		}

		loadConfig();
		mRedisAPI = new RedisAPI(ConfigAPI.getRedisHost(), ConfigAPI.getRedisPort());
		getServer().getPluginManager().registerEvents(new DataEventListener(this.getLogger(), mVersionAdapter), this);
		getServer().getPluginManager().registerEvents(new ScoreboardCleanupListener(this, this.getLogger(), mVersionAdapter), this);
		if (ConfigAPI.getTicksPerPlayerAutosave() > 0) {
			getServer().getPluginManager().registerEvents(new AutoSaveListener(this, mVersionAdapter), this);
		}

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
	}

	@Override
	public void onDisable() {
		INSTANCE = null;
		if (mRedisAPI != null) {
			mRedisAPI.shutdown();
		}
		mRedisAPI = null;
		getServer().getScheduler().cancelTasks(this);
	}

	@SuppressWarnings("NullAway") // Intentionally don't mark this as nullable - if this plugin is working, this will not be null
	protected static MonumentaRedisSync getInstance() {
		return INSTANCE;
	}

	@SuppressWarnings("NullAway") // Intentionally don't mark this as nullable - if this plugin is working, this will not be null
	public VersionAdapter getVersionAdapter() {
		return mVersionAdapter;
	}

	private void loadConfig() {
		File configFile = new File(this.getDataFolder(), "config.yml");
		/* TODO: Default file if not exist */
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		String redisHost = config.getString("redis_host", "redis");
		int redisPort = config.getInt("redis_port", 6379);
		String serverDomain = config.getString("server_domain", "default_domain");

		/* Get default shard name from network relay if enabled */
		String shardName = NetworkRelayIntegration.getShardName();
		if (shardName == null) {
			shardName = "default_shard";
		}
		shardName = config.getString("shard_name", shardName);

		int historyAmount = config.getInt("history_amount", 20);
		int ticksPerPlayerAutosave = config.getInt("ticks_per_player_autosave", 6060);
		boolean savingDisabled = config.getBoolean("saving_disabled", false);
		boolean scoreboardCleanupEnabled = config.getBoolean("scoreboard_cleanup_enabled", true);

		String level = config.getString("log_level", "INFO").toLowerCase();
		switch (level) {
			case "finest":
				setLogLevel(Level.FINEST);
				break;
			case "finer":
				setLogLevel(Level.FINER);
				break;
			case "fine":
				setLogLevel(Level.FINE);
				break;
			default:
				setLogLevel(Level.INFO);
		}

		new ConfigAPI(getLogger(), redisHost, redisPort, serverDomain, shardName, historyAmount, ticksPerPlayerAutosave, savingDisabled, scoreboardCleanupEnabled);
	}

	public void setLogLevel(Level level) {
		super.getLogger().info("Changing log level to: " + level.toString());
		getLogger().setLevel(level);
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}
}
