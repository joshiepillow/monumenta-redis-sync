package com.playmonumenta.redissync.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ScoreboardUtils {
	/* TODO: Move to config file */
	public static final String[] NOT_TRANSFERRED_OBJECTIVES_VALS =
	    new String[] {"Apartment", "AptIdle", "VotesWeekly", "VotesTotal", "VotesSinceWin", "VoteRewards", "VoteRaffle", "VoteCache", "KaulSpleefWins", "SnowmanKills"};
	public static final Set<String> NOT_TRANSFERRED_OBJECTIVES =
	    new HashSet<>(Arrays.asList(NOT_TRANSFERRED_OBJECTIVES_VALS));

	public static JsonObject getAsJsonObject(Player player) {
		JsonObject data = new JsonObject();

		for (Objective objective : Bukkit.getScoreboardManager().getMainScoreboard().getObjectives()) {
			Score score = objective.getScore(player.getName());
			if (score != null) {
				data.addProperty(objective.getName(), score.getScore());
			}
		}

		return data;
	}

	public static void loadFromJsonObject(Player player, JsonObject data) {
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
			String name = entry.getKey();
			if (!NOT_TRANSFERRED_OBJECTIVES.contains(name)) {
				int scoreVal = entry.getValue().getAsInt();

				Objective objective = scoreboard.getObjective(name);
				if (objective == null) {
					objective = scoreboard.registerNewObjective(name, "dummy", name);
				}

				Score score = objective.getScore(player.getName());
				score.setScore(scoreVal);
			}
		}
	}
}
