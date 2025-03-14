/*
 * Copyright (c) 2021, Brooklyn <https://github.com/Broooklyn>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.brooklyn.tobnoticeboard;

import com.brooklyn.tobnoticeboard.friendnotes.FriendNoteManager;
import com.brooklyn.tobnoticeboard.raids.NoticeBoard;
import com.brooklyn.tobnoticeboard.raids.ToaNoticeBoard;
import com.brooklyn.tobnoticeboard.raids.TobNoticeBoard;
import com.google.inject.Provides;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.Ignore;
import net.runelite.api.NameableContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Raid Notice Board",
	description = "Highlight friends and clan members on the ToB/ToA Notice Board",
	tags = "tob, theatre, theater, pvm, combat, party, friend, clan, cc, fc, friendschat, clanchat, raids, hub, brooklyn, gtjamesa, toa"
)
public class RaidNoticeBoardPlugin extends Plugin
{
	private static final int DEFAULT_RGB = 0xff981f;
	public static final String CONFIG_GROUP = "tobnoticeboard";
	public static final String CONFIG_KEY_HIGHLIGHT_LOBBY = "highlightInLobby";
	public static final String CONFIG_KEY_FRIEND_NOTES = "friendNotes";
	private boolean friendNotesEnabled = false;

	public static final List<Integer> LOBBY_COMPONENTS = List.of(
		TobNoticeBoard.TOB_NOTICE_BOARD_COMPONENT_ID, TobNoticeBoard.TOB_LOBBY_COMPONENT_ID,
		ToaNoticeBoard.TOA_NOTICE_BOARD_COMPONENT_ID, ToaNoticeBoard.TOA_LOBBY_COMPONENT_ID
	);

	@Inject
	private Client client;

	@Inject
	private RaidNoticeBoardConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private FriendNoteManager friendNotes;

	@Inject
	private TobNoticeBoard tobNoticeBoard;

	@Inject
	private ToaNoticeBoard toaNoticeBoard;

	private NoticeBoard noticeBoard;
	private int lastWidgetId = -1;

	@Override
	public void startUp()
	{
		addHighlighting();
		eventBus.register(friendNotes);
		friendNotes.startUp();
	}

	@Override
	public void shutDown()
	{
		removeHighlighting();
		eventBus.register(friendNotes);
		friendNotes.shutDown();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			// Lobby highlighting has been disabled, reset the colors
			if (event.getKey().equals(CONFIG_KEY_HIGHLIGHT_LOBBY) && !config.highlightInLobby())
			{
				setLobbyColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
				return;
			}

			addHighlighting();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		clientThread.invokeLater(() ->
		{
			final int groupId = widgetLoaded.getGroupId();

			if (LOBBY_COMPONENTS.contains(groupId))
			{
				lastWidgetId = groupId;
				noticeBoard = getNoticeBoard(groupId);
				addHighlighting();
			}
		});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_UPDATE || event.getScriptId() == ScriptID.IGNORE_UPDATE)
		{
			addHighlighting();
		}
	}

	/**
	 * Initialise the correct notice board based on the group ID
	 */
	private NoticeBoard getNoticeBoard(int groupId)
	{
		switch (groupId)
		{
			case TobNoticeBoard.TOB_NOTICE_BOARD_COMPONENT_ID:
			case TobNoticeBoard.TOB_LOBBY_COMPONENT_ID:
				return tobNoticeBoard;
			case ToaNoticeBoard.TOA_NOTICE_BOARD_COMPONENT_ID:
			case ToaNoticeBoard.TOA_LOBBY_COMPONENT_ID:
				return toaNoticeBoard;
			default:
				return null;
		}
	}

	/**
	 * Set colors on the main notice board (party listing)
	 */
	private void setNoticeBoardColors(int friendColor, int clanColor, int ignoreColor)
	{
		if (noticeBoard != null)
		{
			noticeBoard.setNoticeBoardColors(friendColor, clanColor, ignoreColor, this::updatePlayerName);
		}
	}

	/**
	 * Set colors in the lobby (inside a party)
	 */
	private void setLobbyColors(int friendColor, int clanColor, int ignoreColor)
	{
		if (noticeBoard != null)
		{
			noticeBoard.setLobbyColors(friendColor, clanColor, ignoreColor, this::updatePlayerName);
		}
	}

	private void updatePlayerName(Party party, Widget widget, String nameText, int friendColor, int clanColor, int ignoreColor)
	{
		NameableContainer<Ignore> ignoreContainer = client.getIgnoreContainer();
		NameableContainer<Friend> friendContainer = client.getFriendContainer();
		String playerName = Text.removeTags(nameText).trim();
		final boolean isLobby = party.equals(Party.TOB_LOBBY) || party.equals(Party.TOA_LOBBY);

		// Don't highlight the local player
		if (playerName.equals(client.getLocalPlayer().getName()))
		{
			return;
		}

		// Highlight friend/clan/ignored players
		if (ignoreContainer.findByName(playerName) != null)
		{
			widget.setTextColor(config.highlightIgnored() ? ignoreColor : DEFAULT_RGB);
		}
		else if (friendContainer.findByName(playerName) != null)
		{
			widget.setTextColor(config.highlightFriends() ? friendColor : DEFAULT_RGB);
		}
		else if (client.getFriendsChatManager() != null)
		{
			for (FriendsChatMember member : client.getFriendsChatManager().getMembers())
			{
				if (Text.toJagexName(member.getName()).equals(playerName))
				{
					widget.setTextColor(config.highlightClan() ? clanColor : DEFAULT_RGB);
				}
			}
		}

		// Add the note icon after the username (only shown on inside lobby widget)
		if (friendNotesEnabled && isLobby && !playerName.equals("-"))
		{
			final String note = friendNotes.getNote(playerName);

			if (note != null)
			{
				log.debug("Player: {}, Note: {}", playerName, note);
				friendNotes.updateWidget(widget, playerName);
			}
		}
	}

	private void addHighlighting()
	{
		int friendColor = config.friendColor().getRGB();
		int clanColor = config.clanColor().getRGB();
		int ignoreColor = config.ignoredColor().getRGB();
		friendNotesEnabled = friendNotes.isEnabled();

		setNoticeBoardColors(friendColor, clanColor, ignoreColor);

		if (config.highlightInLobby())
		{
			setLobbyColors(friendColor, clanColor, ignoreColor);
		}
	}

	private void removeHighlighting()
	{
		setNoticeBoardColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
		setLobbyColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
	}

	@Provides
	RaidNoticeBoardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RaidNoticeBoardConfig.class);
	}
}
