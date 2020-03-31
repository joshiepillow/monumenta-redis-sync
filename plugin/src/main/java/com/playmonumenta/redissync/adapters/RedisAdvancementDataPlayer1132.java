package com.playmonumenta.redissync.adapters;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.v1_13_R2.Advancement;
import net.minecraft.server.v1_13_R2.AdvancementDataPlayer;
import net.minecraft.server.v1_13_R2.AdvancementProgress;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.MinecraftServer;

public class RedisAdvancementDataPlayer1132 extends AdvancementDataPlayer {
    private static final Gson gson = (new GsonBuilder()).registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.a()).registerTypeAdapter(MinecraftKey.class, new MinecraftKey.a()).setPrettyPrinting().create();
	private final EntityPlayer entityplayer;

    public RedisAdvancementDataPlayer1132(MinecraftServer minecraftserver, File file, EntityPlayer entityplayer) {
		super(minecraftserver, file, entityplayer);
		this.entityplayer = entityplayer;
    }

	@Override
    public void c() {
        Map<MinecraftKey, AdvancementProgress> map = Maps.newHashMap();
        Iterator<Map.Entry<Advancement, AdvancementProgress>> iterator = this.data.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Advancement, AdvancementProgress> entry = (Entry<Advancement, AdvancementProgress>) iterator.next();
            AdvancementProgress advancementprogress = (AdvancementProgress) entry.getValue();

            if (advancementprogress.b()) {
                map.put(((Advancement) entry.getKey()).getName(), advancementprogress);
            }
        }

		gson.toJson(map).toString();
		/* TODO: JEDIS */
    }

	@Override
    private void g() {

	}
}

