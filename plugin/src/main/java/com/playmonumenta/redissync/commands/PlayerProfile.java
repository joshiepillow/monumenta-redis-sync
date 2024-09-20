package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class PlayerProfile {
	public static void register() {
		new CommandAPICommand("playerprofile")
			.withPermission(CommandPermission.fromString("monumenta.command.playerprofile"))
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
