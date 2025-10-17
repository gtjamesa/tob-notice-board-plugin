package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.Constant;
import com.brooklyn.tobnoticeboard.RaidStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
public class OrbOrderManager
{
	public static final String MESSAGE_RAID_ENTERED = "You enter the Theatre of Blood";

	private HashMap<String, TobPlayer> playerMap = new HashMap<>();
	private String[] currentParty;
	private int playerCount = 0;

	@Getter
	private String[] lastParty;

	@Getter
	private int lastPlayerCount = 0;

	@Getter
	private OrbStatus orbStatus = OrbStatus.OK;

	@Getter
	private boolean inTob;

	@Getter
	private RaidStatus raidStatus = RaidStatus.NOT_IN_PARTY;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BoardHighlightOverlay boardHighlightOverlay;

	public void startUp()
	{
		overlayManager.add(boardHighlightOverlay);
	}

	public void shutDown()
	{
		reset();
		overlayManager.remove(boardHighlightOverlay);
	}

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
		if (event.getScriptId() == Constant.SCRIPT_ID_TOB_HUD_DRAW && raidStatus.equals(RaidStatus.IN_PARTY))
		{
			clientThread.invokeLater(this::createPlayers);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.TOB_CLIENT_PARTYSTATUS)
		{
			return;
		}

		int val = client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS);
		raidStatus = RaidStatus.fromInt(val);
		inTob = val > 1;
	}

	protected void startRaid()
	{
		createPlayers();
		updateRoles();
		save();

		log.debug("Raid started: {}", getFormattedParty());
	}

	/**
	 * Get the current players in the orb order.
	 */
	protected void createPlayers()
	{
		currentParty = new String[Constant.TOB_MAX_PARTY_SIZE];
		playerCount = 0;
		val playerNames = fetchPlayerNames();

		for (int i = 0; i < playerNames.size(); i++)
		{
			val playerName = playerNames.get(i);
			currentParty[i] = playerName;

			// update player's orb position in the mapping
			// the position will later be saved for checks before the next raid
			val tobPlayer = playerMap.getOrDefault(playerName, new TobPlayer(playerName));
			tobPlayer.setOrb(i);
			tobPlayer.setRole(TobRole.MFRZ);
			playerMap.put(playerName, tobPlayer);

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
		// We will also save their orb position for the next raid
		playerMap = Arrays.stream(currentParty)
			.map(name -> playerMap.get(name))
			.filter(Objects::nonNull)
			.collect(
				HashMap::new,
				(m, p) -> {
					p.setSavedOrb(p.getOrb());
					m.put(p.getName(), p);
				},
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
		int curHash = Arrays.hashCode(currentParty);
		int lastHash = Arrays.hashCode(lastParty);

		if (curHash == lastHash)
		{
			return OrbStatus.OK;
		}

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
			if (currentParty[i] == null || lastParty[i] == null)
			{
				continue;
			}

			// order is different
			if (!currentParty[i].equals(lastParty[i]))
			{
				// get the player who was in this position for the last raid
				val lastPlayer = playerMap.get(lastParty[i]);

				// if the previous player's "current" position matches saved, then they have left the raid
				// this is true because we already know the order is different
				if (lastPlayer != null && lastPlayer.getSavedOrb() == i && lastPlayer.getOrb() == lastPlayer.getSavedOrb())
				{
					continue;
				}

				return OrbStatus.INCORRECT;
			}
		}

		return OrbStatus.OK;
	}

	public TobPlayer getPlayer(String name)
	{
		return playerMap.get(name);
	}

	/**
	 * Fetch player names from the top-left party panel
	 *
	 * @return list of player names
	 */
	@VisibleForTesting
	List<String> fetchPlayerNames()
	{
		List<String> names = new ArrayList<>();
		Widget hud = client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID);

		if (hud == null)
		{
			return names;
		}

		String text = hud.getText();
		if (Strings.isNullOrEmpty(text))
		{
			return names;
		}

		return Arrays.stream(text.split("<br>")) // user1<br>user2<br>-<br>-<br>-
			.filter(name -> !name.equals("-") && !Strings.isNullOrEmpty(name))
			.map(name -> Text.removeTags(name).trim())
			.collect(Collectors.toList());
	}

	private int fetchPlayerCount()
	{
		return fetchPlayerNames().size();
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

	String getFormattedParty()
	{
		if (lastParty == null || lastPlayerCount == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lastPlayerCount; i++)
		{
			TobPlayer player = playerMap.get(lastParty[i]);
			if (player != null)
			{
				sb.append(String.format("%s (%s), ", player.getName(), player.getRole().getName()));
			}
		}

		if (sb.length() > 2)
		{
			sb.setLength(sb.length() - 2); // Remove trailing comma and space
		}

		return sb.toString();
	}
}
