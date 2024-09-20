package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class PlayerProfileDelete {
	public static void register() {
		new CommandAPICommand("playerprofiledelete")
			.withPermission(CommandPermission.fromString("monumenta.command.playerprofiledelete"))
			.executesPlayer((sender, args) -> {
					try {
						MonumentaRedisSyncAPI.deletePlayerProfile(sender);
					} catch (Exception ex) {
						throw CommandAPI.failWithString(ex.getMessage());
					}
				}
			).register();
	}
}
