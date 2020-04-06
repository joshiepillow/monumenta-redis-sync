package com.playmonumenta.redissync.adapters;

import java.io.IOException;

import org.bukkit.entity.Player;

public interface VersionAdapter {
	public static class SaveData {
		private final byte[] mData;
		private final String mShardData;

		protected SaveData(byte[] data, String shardData) {
			mData = data;
			mShardData = shardData;
		}

		public byte[] getData() {
			return mData;
		}

		public String getShardData() {
			return mShardData;
		}
	}

	Object retrieveSaveData(Player player, byte[] data, String shardData) throws IOException;
	SaveData extractSaveData(Player player, Object nbtObj) throws IOException;
}
