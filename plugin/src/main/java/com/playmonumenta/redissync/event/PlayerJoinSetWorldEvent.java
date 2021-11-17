package com.playmonumenta.redissync.event;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * This event fires when a player joins and their data is loaded but before they are loaded into a world.
 *
 * This is useful to change the player's world seamlessly without them ever seeing a flash of a different world on join.
 *
 * Note that at the time this event is called, the player's scores are available but all player data is blank, including inventory and tags.
 */
public class PlayerJoinSetWorldEvent extends PlayerEvent {

	private static final HandlerList handlers = new HandlerList();

	private World mWorld;

	public PlayerJoinSetWorldEvent(Player player, World world) {
		super(player);
		mWorld = world;
	}

	/*
	 * Get the world the player is currently set to join on login
	 */
	public World getWorld() {
		return mWorld;
	}

	/* Causes a player to be logged into this world instead
	 * This takes a world object that should be currently loaded
	 */
	public void setWorld(World world) {
		mWorld = world;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
