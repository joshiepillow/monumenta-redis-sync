package com.playmonumenta.redissync.api;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.bukkit.entity.Player;

import com.playmonumenta.redissync.DataEventListener;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;

public class RedisAPI {
	public static final class StringByteCodec implements RedisCodec<String, byte[]> {
		public static final StringByteCodec INSTANCE = new StringByteCodec();
		private static final byte[] EMPTY = new byte[0];
		private final Charset charset = Charset.forName("UTF-8");

		@Override
		public String decodeKey(final ByteBuffer bytes) {
			return charset.decode(bytes).toString();
		}

		@Override
		public byte[] decodeValue(final ByteBuffer bytes) {
			return getBytes(bytes);
		}

		@Override
		public ByteBuffer encodeKey(final String key) {
			return charset.encode(key);
		}

		@Override
		public ByteBuffer encodeValue(final byte[] value) {
			if (value == null) {
				return ByteBuffer.wrap(EMPTY);
			}

			return ByteBuffer.wrap(value);
		}

		private static byte[] getBytes(final ByteBuffer buffer) {
			final byte[] b = new byte[buffer.remaining()];
			buffer.get(b);
			return b;
		}

	}
	private static RedisClient mRedisClient = null;
	private static StatefulRedisConnection<String, String> mConnection = null;
	private static StatefulRedisConnection<String, byte[]> mStringByteConnection = null;

	public RedisAPI(String hostname, int port) {
		mRedisClient = RedisClient.create(RedisURI.Builder.redis(hostname, port).build());
		mConnection = mRedisClient.connect();
		mStringByteConnection = mRedisClient.connect(StringByteCodec.INSTANCE);
	}

	/*
	 * Do not call this outside Plugin.java onDisable()
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

	public static RedisCommands<String, byte[]> syncStringBytes() {
		return mStringByteConnection.sync();
	}

	public static RedisAsyncCommands<String, byte[]> asyncStringBytes() {
		return mStringByteConnection.async();
	}

	public static boolean isReady() {
		return mConnection.isOpen();
	}

	public static void disableDataSavingUntilNextLogin(Player player) {
		DataEventListener.disableDataSavingUntilNextLogin(player);
	}
}
