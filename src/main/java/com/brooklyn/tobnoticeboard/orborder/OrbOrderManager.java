package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.Constant;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

@Slf4j
public class OrbOrderManager
{
	public static final String MESSAGE_RAID_ENTERED = "You enter the Theatre of Blood";

	/**
	 * VarClientStrs IDs for the player names on the orb order interface.
	 *
	 * @see <a href="https://github.com/Trevor159/runelite-external-plugins/blob/b9d58dd864ce33a23b34eac91865bdb1521a379a/src/main/java/trevor/tobhealthbars/TobHealthBarsPlugin.java#L63-L67">tobhealthbars plugin</a>
	 */
	private final int[] playerNameVarc = {330, 331, 332, 333, 334};

	private HashMap<String, TobPlayer> playerMap = new HashMap<>();
	private String[] currentParty;
	private int playerCount = 0;

	@Getter
	private String[] lastParty;

	@Getter
	private int lastPlayerCount = 0;

	@Getter
	private OrbStatus orbStatus = OrbStatus.OK;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage());

		// Raid started, save current orb order
		if (message.startsWith(MESSAGE_RAID_ENTERED))
		{
			clientThread.invokeLater(this::startRaid);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == Constant.SCRIPT_ID_TOB_HUD_DRAW)
		{
			clientThread.invokeLater(this::createPlayers);
		}
	}

	protected void startRaid()
	{
		createPlayers();
		updateRoles();
		save();
	}

	/**
	 * Get the current players in the orb order.
	 */
	protected void createPlayers()
	{
		currentParty = new String[5];
		playerCount = 0;
		String[] playerNames = fetchPlayerNames();

		for (int i = 0; i < playerNames.length; i++)
		{
			String playerName = playerNames[i];
			currentParty[i] = playerName;
			playerMap.put(playerName, new TobPlayer(playerName, i));
			playerCount++;
		}

		orbStatus = checkOrbOrder();
	}

	/**
	 * Save the current players as the last known raid players.
	 * <br>
	 * This is called when a raid starts
	 */
	protected void save()
	{
		if (currentParty == null || playerCount == 0)
		{
			return;
		}

		lastParty = Arrays.copyOf(currentParty, currentParty.length);
		lastPlayerCount = playerCount;

		// Prune `playerMap` to ensure only contains players in the current party
		playerMap = Arrays.stream(currentParty)
			.map(name -> playerMap.get(name))
			.filter(Objects::nonNull)
			.collect(
				HashMap::new,
				(m, p) -> m.put(p.getName(), p),
				HashMap::putAll
			);
	}

	protected void reset()
	{
		currentParty = null;
		playerCount = 0;
		lastParty = null;
		lastPlayerCount = 0;
		orbStatus = OrbStatus.OK;
	}

	private OrbStatus checkOrbOrder()
	{
		if (playerCount > lastPlayerCount) // players have joined
		{
			return OrbStatus.NEW_PLAYERS;
		}
		else if (lastPlayerCount > playerCount) // players have left
		{
			return OrbStatus.OK;
		}

		for (int i = 0; i < currentParty.length; i++)
		{
			if (currentParty[i] == null || lastParty == null || lastParty[i] == null)
			{
				continue;
			}

			if (!currentParty[i].equals(lastParty[i]))
			{
				return OrbStatus.INCORRECT;
			}
		}

		return OrbStatus.OK;
	}

	@VisibleForTesting
	TobPlayer getPlayer(String name)
	{
		return playerMap.get(name);
	}

	/**
	 * Fetch player names from the client varc strings or the party panel
	 *
	 * @return array of player names
	 */
	@VisibleForTesting
	String[] fetchPlayerNames()
	{
		return Arrays.stream(playerNameVarc)
			.mapToObj(client::getVarcStrValue)
			.filter(name -> name != null && !name.isEmpty())
			.toArray(String[]::new);
	}

	private int fetchPlayerCount()
	{
		return fetchPlayerNames().length;
	}

	/**
	 * Assign player roles based on their position in the orb order
	 * All scales start with a mage
	 */
	private void updateRoles()
	{
		if (currentParty == null || playerCount == 0)
		{
			return;
		}

		if (playerCount == 1)
		{
			playerMap.get(currentParty[0]).setRole(TobRole.SOLO);
			return;
		}

		playerMap.get(currentParty[0]).setRole(TobRole.SFRZ);

		// Assign roles based on player count
		switch (playerCount)
		{
			case 2:
				playerMap.get(currentParty[1]).setRole(TobRole.RDPS);
				break;
			case 3:
				playerMap.get(currentParty[1]).setRole(TobRole.RDPS);
				playerMap.get(currentParty[2]).setRole(TobRole.MDPS);
				break;
			case 4:
				playerMap.get(currentParty[1]).setRole(TobRole.MFRZ);
				playerMap.get(currentParty[2]).setRole(TobRole.RDPS);
				playerMap.get(currentParty[3]).setRole(TobRole.MDPS);
				break;
			case 5:
				playerMap.get(currentParty[1]).setRole(TobRole.MFRZ);
				playerMap.get(currentParty[2]).setRole(TobRole.RDPS);
				playerMap.get(currentParty[3]).setRole(TobRole.MDPS);
				playerMap.get(currentParty[4]).setRole(TobRole.MDPS);
				break;
			default:
				break;
		}
	}
}
