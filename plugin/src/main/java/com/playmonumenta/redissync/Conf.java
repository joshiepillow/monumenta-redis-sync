package com.playmonumenta.redissync;

public class Conf {
	private static Conf INSTANCE = null;

	private final String mHost;
	private final int mPort;
	private final String mDomain;
	private final String mShard;

	protected static String getHost() {
		return INSTANCE.mHost;
	}

	protected static int getPort() {
		return INSTANCE.mPort;
	}

	protected static String getDomain() {
		return INSTANCE.mDomain;
	}

	protected static String getShard() {
		return INSTANCE.mShard;
	}

	protected Conf(String host, int port, String domain, String shard) {
		mHost = host;
		mPort = port;
		mDomain = domain;
		mShard = shard;
		// TODO: History to keep
		INSTANCE = this;
	}
}
