package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.entity.Player;

public class PlayerLoadFromPlayer {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerFrom = new EntitySelectorArgument.OnePlayer("player");
		IntegerArgument profileTo = new IntegerArgument("profileTo", 0);
		IntegerArgument profileFrom = new IntegerArgument("profileFrom", 0);
		IntegerArgument historyIndex = new IntegerArgument("historyIndex", 0);

		new CommandAPICommand("playerloadfromplayer")
			.withPermission(CommandPermission.fromString("monumenta.command.playerloadfromplayer"))
			.withArguments(playerFrom)
			.withArguments(profileTo)
			.withArguments(profileFrom)
			.withArguments(historyIndex)
			.executesPlayer((sender, args) -> {
				try {
					MonumentaRedisSyncAPI.playerLoadFromPlayer(sender, args.getByArgument(playerFrom),
						args.getByArgument(profileTo), args.getByArgument(profileFrom), args.getByArgument(historyIndex));
				} catch (Exception ex) {
					throw CommandAPI.failWithString(ex.getMessage());
				}
			}
		).register();
	}
}
