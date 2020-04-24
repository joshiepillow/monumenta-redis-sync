package com.playmonumenta.redissync;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;

public class RedisAPI {
	private static final class StringByteCodec implements RedisCodec<String, byte[]> {
		private static final StringByteCodec INSTANCE = new StringByteCodec();
		private static final byte[] EMPTY = new byte[0];
		private final Charset mCharset = Charset.forName("UTF-8");

		@Override
		public String decodeKey(final ByteBuffer bytes) {
			return mCharset.decode(bytes).toString();
		}

		@Override
		public byte[] decodeValue(final ByteBuffer bytes) {
			return getBytes(bytes);
		}

		@Override
		public ByteBuffer encodeKey(final String key) {
			return mCharset.encode(key);
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

	private static RedisAPI INSTANCE = null;

	private RedisClient mRedisClient = null;
	private StatefulRedisConnection<String, String> mConnection = null;
	private StatefulRedisConnection<String, byte[]> mStringByteConnection = null;

	protected RedisAPI(String hostname, int port) {
		mRedisClient = RedisClient.create(RedisURI.Builder.redis(hostname, port).build());
		mConnection = mRedisClient.connect();
		mStringByteConnection = mRedisClient.connect(StringByteCodec.INSTANCE);
		INSTANCE = this;
	}

	protected void shutdown() {
		mConnection.close();
		mConnection = null;
		mRedisClient.shutdown();
		mRedisClient = null;
	}

	public static RedisAPI getInstance() {
		return INSTANCE;
	}

	public RedisCommands<String, String> sync() {
		return mConnection.sync();
	}

	public RedisAsyncCommands<String, String> async() {
		return mConnection.async();
	}

	public RedisCommands<String, byte[]> syncStringBytes() {
		return mStringByteConnection.sync();
	}

	public RedisAsyncCommands<String, byte[]> asyncStringBytes() {
		return mStringByteConnection.async();
	}

	public boolean isReady() {
		return mConnection.isOpen();
	}
}
