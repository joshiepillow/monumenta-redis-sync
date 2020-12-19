package com.playmonumenta.redissync.commands;

import java.util.LinkedHashMap;
import java.util.logging.Level;

import com.playmonumenta.redissync.MonumentaRedisSync;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;

public class ChangeLogLevel {
	public static void register(MonumentaRedisSync plugin) {
		String command = "mrsChangeLogLevel";
		CommandPermission perms = CommandPermission.fromString("mrs.command.changeloglevel");
		LinkedHashMap<String, Argument> arguments;

		arguments = new LinkedHashMap<>();
		arguments.put("INFO", new LiteralArgument("INFO"));
		new CommandAPICommand(command)
			.withArguments(arguments)
			.withPermission(perms)
			.executes((sender, args) -> {
				plugin.setLogLevel(Level.INFO);
			}
		).register();

		arguments.clear();
		arguments.put("FINE", new LiteralArgument("FINE"));
		new CommandAPICommand(command)
			.withArguments(arguments)
			.withPermission(perms)
			.executes((sender, args) -> {
				plugin.setLogLevel(Level.FINE);
			}
		).register();

		arguments.clear();
		arguments.put("FINER", new LiteralArgument("FINER"));
		new CommandAPICommand(command)
			.withArguments(arguments)
			.withPermission(perms)
			.executes((sender, args) -> {
				plugin.setLogLevel(Level.FINER);
			}
		).register();

		arguments.clear();
		arguments.put("FINEST", new LiteralArgument("FINEST"));
		new CommandAPICommand(command)
			.withArguments(arguments)
			.withPermission(perms)
			.executes((sender, args) -> {
				plugin.setLogLevel(Level.FINEST);
			}
		).register();
	}
}
