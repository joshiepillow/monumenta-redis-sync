package com.playmonumenta.redissync.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.redissync.Conf;
import com.playmonumenta.redissync.MonumentaRedisSync;
import com.playmonumenta.redissync.api.PlayerServerTransferEvent;

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
												  sendPlayer(plugin, player, (String)args[1]);
											  }
		                                  }
		);
	}

	private static void sendPlayer(Plugin plugin, Player player, String target) throws CommandSyntaxException {
		if (target.equalsIgnoreCase(Conf.getShard())) {
			error(player, "Can not transfer to the same server you are already on");
			return;
		}

		PlayerServerTransferEvent event = new PlayerServerTransferEvent(player, target);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		player.sendMessage(ChatColor.GOLD + "Transferring you to " + target);

		/* TODO: Lock data */

		try {
			MonumentaRedisSync.getVersionAdapter().savePlayer(player);
		} catch (Exception ex) {
			String message = "Failed to save player data for player '" + player.getName() + "'";
			plugin.getLogger().severe(message);
			ex.printStackTrace();
			CommandAPI.fail(message);
		}

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(target);

		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

		/* TODO: Timeout if it fails and unlock */
	}

	protected static void error(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.RED + msg);
	}
}


