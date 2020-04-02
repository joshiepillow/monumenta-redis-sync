package com.playmonumenta.redissync;

import java.util.Map;
import java.util.logging.Logger;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

/*
 * IMPORTANT: Do not try to access the Jedis functions directly as doing so incorrectly can cause
 * server crashes. If you need to use a Jedis function that does not yet exist in this class,
 * add it here following the same format used in the other functions.
 */
public class RedisAPI {
	private static Logger mLogger = null;

	private static RedisClient mRedisClient = null;
	private static StatefulRedisConnection<String, String> mConnection = null;

	public RedisAPI(Logger logger, String uri) throws Exception {
		mLogger = logger;
		mRedisClient = RedisClient.create(RedisURI.Builder.redis("redis", 6379).build());
		mConnection = mRedisClient.connect();
	}

	/*
	 * Do not call this outside Pulgin.java onDisable()
	 */
	public void shutdown() {
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
}
