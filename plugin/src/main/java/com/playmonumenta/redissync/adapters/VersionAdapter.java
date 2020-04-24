package com.playmonumenta.redissync.adapters;

import java.io.IOException;

import org.bukkit.entity.Player;

import com.playmonumenta.redissync.DataEventListener.ReturnParams;

public interface VersionAdapter {
	class SaveData {
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

	SaveData extractSaveData(Player player, Object nbtObj, ReturnParams returnParams) throws IOException;

	void savePlayer(Player player) throws Exception;
}
