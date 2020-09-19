package com.playmonumenta.redissync.adapters;

import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface VersionAdapter {
	public static class ReturnParams {
		public final Location mReturnLoc;
		public final Float mReturnYaw;
		public final Float mReturnPitch;

		public ReturnParams(Location returnLoc, Float returnYaw, Float returnPitch) {
			mReturnLoc = returnLoc;
			mReturnYaw = returnYaw;
			mReturnPitch = returnPitch;
		}
	}

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

	Object retrieveSaveData(byte[] data, String shardData) throws IOException;

	SaveData extractSaveData(Object nbtObj, ReturnParams returnParams) throws IOException;

	void savePlayer(Player player) throws Exception;

	Object upgradePlayerData(Object nbtTagCompound);

	String upgradePlayerAdvancements(String advancementsStr) throws Exception;
}
