package com.playmonumenta.redissync;

public class Conf {
	private static Conf INSTANCE = null;

	private final String mHost;
	private final int mPort;
	private final String mDomain;
	private final String mShard;
	private final int mHistory;
	private final boolean mSavingDisabled;

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

	protected static int getHistory() {
		return INSTANCE.mHistory;
	}

	protected static boolean getSavingDisabled() {
		return INSTANCE.mSavingDisabled;
	}

	protected Conf(String host, int port, String shard) {
		mHost = host;
		mPort = port;
		mDomain = null;
		mShard = shard;
		mHistory = -1;
		mSavingDisabled = false;
		INSTANCE = this;
	}

	protected Conf(String host, int port, String domain, String shard, int history, boolean savingDisabled) {
		mHost = host;
		mPort = port;
		mDomain = domain;
		mShard = shard;
		mHistory = history;
		mSavingDisabled = savingDisabled;
		INSTANCE = this;
	}
}
