package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.entity.Player;

public class PlayerRollback {
	public static void register() {
		new CommandAPICommand("playerrollback")
			.withPermission(CommandPermission.fromString("monumenta.command.playerrollback"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.withArguments(new IntegerArgument("index", 0))
			.executes((sender, args) -> {
				if (!(sender instanceof Player)) {
					throw CommandAPI.failWithString("This command can only be run by players");
				}
				try {
					MonumentaRedisSyncAPI.playerRollback((Player)sender, (Player)args[0], (Integer)args[1]);
				} catch (Exception ex) {
					throw CommandAPI.failWithString(ex.getMessage());
				}
			}
		).register();
	}
}
