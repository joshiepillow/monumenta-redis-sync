package com.playmonumenta.redissync;

public class Conf {
	private static Conf INSTANCE = null;

	private final String mHost;
	private final int mPort;
	private final String mDomain;
	private final String mShard;
	private final int mHistory;
	private final boolean mSavingDisabled;
	private final boolean mScoreboardCleanupEnabled;

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

	protected static boolean getScoreboardCleanupEnabled() {
		return INSTANCE.mScoreboardCleanupEnabled;
	}

	protected Conf(String host, int port, String shard) {
		mHost = host;
		mPort = port;
		mDomain = null;
		mShard = shard;
		mHistory = -1;
		mSavingDisabled = false;
		mScoreboardCleanupEnabled = true;
		INSTANCE = this;
	}

	protected Conf(String host, int port, String domain, String shard, int history, boolean savingDisabled, boolean scoreboardCleanupEnabled) {
		mHost = host;
		mPort = port;
		mDomain = domain;
		mShard = shard;
		mHistory = history;
		mSavingDisabled = savingDisabled;
		mScoreboardCleanupEnabled = scoreboardCleanupEnabled;
		INSTANCE = this;
	}
}
