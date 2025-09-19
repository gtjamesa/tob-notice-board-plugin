package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.Constant;
import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
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

	private TobPlayer[] players;
	private int playerCount = 0;

	@Getter
	private int lastPlayerCount = 0;

	@Getter
	private TobPlayer[] lastRaidPlayers;

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
		switch (event.getScriptId())
		{
			case Constant.SCRIPT_ID_TOB_HUD_DRAW:
			case Constant.SCRIPT_ID_TOB_PARTY_DETAILS_SORT:
			case Constant.SCRIPT_ID_TOB_PARTY_LIST_SET_SORT:
				log.debug("ScriptPostFired: {}", event.getScriptId());
				clientThread.invokeLater(this::createPlayers);
				break;
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
		players = new TobPlayer[5];
		playerCount = 0;
		String[] playerNames = fetchPlayerNames();

		for (int i = 0; i < playerNames.length; i++)
		{
			players[i] = new TobPlayer(playerNames[i], i);
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
		if (players != null && playerCount > 0)
		{
			lastRaidPlayers = Arrays.copyOf(players, players.length);
			lastPlayerCount = playerCount;
		}
	}

	protected void reset()
	{
		players = null;
		playerCount = 0;
		lastRaidPlayers = null;
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

		for (int i = 0; i < players.length; i++)
		{
			if (players[i] == null || lastRaidPlayers == null || lastRaidPlayers[i] == null)
			{
				continue;
			}

			if (!players[i].getName().equals(lastRaidPlayers[i].getName()))
			{
				return OrbStatus.INCORRECT;
			}
		}

		return OrbStatus.OK;
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
		if (players == null || playerCount == 0)
		{
			return;
		}

		if (playerCount == 1)
		{
			players[0].setRole(TobRole.SOLO);
			return;
		}

		players[0].setRole(TobRole.SFRZ);

		// Assign roles based on player count
		switch (playerCount)
		{
			case 2:
				players[1].setRole(TobRole.RDPS);
				break;
			case 3:
				players[1].setRole(TobRole.RDPS);
				players[2].setRole(TobRole.MDPS);
				break;
			case 4:
				players[1].setRole(TobRole.MFRZ);
				players[2].setRole(TobRole.RDPS);
				players[3].setRole(TobRole.MDPS);
				break;
			case 5:
				players[1].setRole(TobRole.MFRZ);
				players[2].setRole(TobRole.RDPS);
				players[3].setRole(TobRole.MDPS);
				players[4].setRole(TobRole.MDPS2);
				break;
			default:
				break;
		}
	}
}
