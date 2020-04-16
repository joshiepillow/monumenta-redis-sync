package com.playmonumenta.redissync.commands;

import java.util.LinkedHashMap;

import org.bukkit.entity.Player;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class Stash {
	public static void register() {
		String command = "stash";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.stash");
		LinkedHashMap<String, Argument> arguments;

		/********************* stash put *********************/

		arguments = new LinkedHashMap<>();
		arguments.put("put", new LiteralArgument("put"));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashPut((Player)sender, null);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);

		/* Optional argument version */
		arguments.put("name", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashPut((Player)sender, (String)args[0]);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);

		/********************* stash get *********************/

		arguments = new LinkedHashMap<>();
		arguments.put("get", new LiteralArgument("get"));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashGet((Player)sender, null);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);

		/* Optional argument version */
		arguments.put("name", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashGet((Player)sender, (String)args[0]);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);

		/********************* stash info *********************/

		arguments = new LinkedHashMap<>();
		arguments.put("info", new LiteralArgument("info"));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashInfo((Player)sender, null);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);

		/* Optional argument version */
		arguments.put("name", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  if (!(sender instanceof Player)) {
												  CommandAPI.fail("This command can only be run by players");
											  }
											  try {
												  MonumentaRedisSyncAPI.stashInfo((Player)sender, (String)args[0]);
											  } catch (Exception ex) {
												  CommandAPI.fail(ex.getMessage());
											  }
		                                  }
		);


	}

}
