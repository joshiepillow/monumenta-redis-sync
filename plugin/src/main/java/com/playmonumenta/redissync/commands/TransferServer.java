package com.playmonumenta.redissync.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.FloatArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
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

		arguments.put("location", new LocationArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
		                                          try {
		                                              MonumentaRedisSyncAPI.sendPlayer(plugin, player, (String)args[1], (Location)args[2]);
		                                          } catch (Exception ex) {
		                                              CommandAPI.fail(ex.getMessage());
		                                          }
		                                      }
		                                  }
		);

		arguments.put("yaw", new FloatArgument());
		arguments.put("pitch", new FloatArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
		                                          try {
		                                              MonumentaRedisSyncAPI.sendPlayer(plugin, player, (String)args[1], (Location)args[2], (Float)args[3], (Float)args[4]);
		                                          } catch (Exception ex) {
		                                              CommandAPI.fail(ex.getMessage());
		                                          }
		                                      }
		                                  }
		);

		/* Single player alias */
		arguments = new LinkedHashMap<>();
		arguments.put("server", new StringArgument());
		CommandAPI.getInstance().register("s",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      if (sender instanceof Player) {
		                                          try {
		                                              MonumentaRedisSyncAPI.sendPlayer(plugin, (Player)sender, (String)args[0]);
		                                          } catch (Exception ex) {
		                                              CommandAPI.fail(ex.getMessage());
		                                          }
		                                      } else {
		                                          CommandAPI.fail("This command can only be run by players");
		                                      }
		                                  }
		);
	}

}


