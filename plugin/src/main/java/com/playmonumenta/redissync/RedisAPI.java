package com.playmonumenta.redissync;

import org.bukkit.entity.Player;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisAPI {
	private static RedisClient mRedisClient = null;
	private static StatefulRedisConnection<String, String> mConnection = null;

	public RedisAPI(String hostname, int port) {
		mRedisClient = RedisClient.create(RedisURI.Builder.redis(hostname, port).build());
		mConnection = mRedisClient.connect();
	}

	/*
	 * Do not call this outside Plugin.java onDisable()
	 */
	protected void shutdown() {
		mConnection.close();
		mConnection = null;
		mRedisClient.shutdown();
		mRedisClient = null;
	}

	public static RedisCommands<String, String> sync() {
		return mConnection.sync();
	}

	public static RedisAsyncCommands<String, String> async() {
		return mConnection.async();
	}

	public static boolean isReady() {
		return mConnection.isOpen();
	}

	public static void disableDataSavingUntilNextLogin(Player player) {
		DataEventListener.disableDataSavingUntilNextLogin(player);
	}
}
