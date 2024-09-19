package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class PlayerChangeProfile {
	public static void register() {
		IntegerArgument profileTo = new IntegerArgument("profileTo", 0);

		new CommandAPICommand("playerchangeprofile")
			.withPermission(CommandPermission.fromString("monumenta.command.playerchangeprofile"))
			.withArguments(profileTo)
			.executesPlayer((sender, args) -> {
					try {
						MonumentaRedisSyncAPI.playerChangeProfile(sender, args.getByArgument(profileTo));
					} catch (Exception ex) {
						throw CommandAPI.failWithString(ex.getMessage());
					}
				}
			).register();
	}
}
