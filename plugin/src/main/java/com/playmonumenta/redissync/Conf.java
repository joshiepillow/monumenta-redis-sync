package com.playmonumenta.redissync;

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

	public Conf(String host, int port, String domain, String shard) {
		mHost = host;
		mPort = port;
		mDomain = domain;
		mShard = shard;
		INSTANCE = this;
	}
}
