package com.playmonumenta.redissync.commands;

import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;

public class PlayerLoadFromPlayer {
	public static void register() {
		String command = "playerloadfromplayer";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.playerloadfromplayer");
		LinkedHashMap<String, Argument> arguments;

		arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("index", new IntegerArgument(0));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.playerLoadFromPlayer((Player)sender, (Player)args[0], (Integer)args[1]);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);
	}
}
