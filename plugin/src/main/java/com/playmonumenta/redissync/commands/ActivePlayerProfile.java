package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class ActivePlayerProfile {
	public static void register() {
		new CommandAPICommand("activeplayerprofile")
			.withPermission(CommandPermission.fromString("monumenta.command.activeplayerprofile"))
			.executesPlayer((sender, args) -> {
					try {
						sender.sendMessage("Profile: " + MonumentaRedisSyncAPI.getPlayerProfile(sender));
					} catch (Exception ex) {
						throw CommandAPI.failWithString(ex.getMessage());
					}
				}
			).register();
	}
}
