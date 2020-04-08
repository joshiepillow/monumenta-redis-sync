package com.playmonumenta.redissync.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class TransferServer {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		String command = "transferserver";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.transferserver");
		LinkedHashMap<String, Argument> arguments;

		arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("server", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  for (Player player : (Collection<Player>)args[0]) {
												  try {
													  MonumentaRedisSyncAPI.sendPlayer(plugin, player, (String)args[1]);
												  } catch (Exception ex) {
													  CommandAPI.fail(ex.getMessage());
												  }
											  }
		                                  }
		);
	}

}


