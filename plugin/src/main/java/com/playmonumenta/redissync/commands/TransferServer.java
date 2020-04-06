package com.playmonumenta.redissync.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.redissync.Conf;
import com.playmonumenta.redissync.api.PlayerServerTransferEvent;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class TransferServer {
	@SuppressWarnings("unchecked")
	public static void register() {
		String command = "transferserver";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.transferserver");

		/* No-argument variant to get server list */
		LinkedHashMap<String, Argument> arguments;

		/* Transfer with data by default */
		arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("server", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      sendPlayer((Collection<Player>)args[0], (String)args[1]);
		                                  }
		);
	}

	private static void sendPlayer(Collection<Player> players, String target) {
		for (Player player : players) {
			if (target.equalsIgnoreCase(Conf.getShard())) {
				error(player, "Can not transfer to the same server you are already on");
				continue;
			}

			PlayerServerTransferEvent event = new PlayerServerTransferEvent(player, target);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			player.sendMessage(ChatColor.GOLD + "Transferring you to " + target);

			player.saveData();

			/* ???? */
		}
	}

	protected static void error(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.RED + msg);
	}
}


