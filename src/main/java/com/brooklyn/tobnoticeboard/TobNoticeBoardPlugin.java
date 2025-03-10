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
import com.google.inject.Provides;
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
	name = "ToB Notice Board",
	description = "Highlight friends and clan members on the Theatre of Blood Notice Board",
	tags = "theatre, theater, pvm, combat, party, friend, clan, cc, fc, friendschat, clanchat, raids, hub, brooklyn"
)
public class TobNoticeBoardPlugin extends Plugin
{
	private static final int DEFAULT_RGB = 0xff981f;
	public static final int NOTICE_BOARD_COMPONENT_ID = 364;
	public static final int LOBBY_COMPONENT_ID = 50;
	public static final String CONFIG_KEY_HIGHLIGHT_LOBBY = "highlightInLobby";
	public static final String CONFIG_KEY_FRIEND_NOTES = "friendNotes";
	private boolean friendNotesEnabled = false;

	@Inject
	private Client client;

	@Inject
	private TobNoticeBoardConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private FriendNoteManager friendNotes;

	@Override
	public void startUp()
	{
		setNoticeBoard();
		eventBus.register(friendNotes);
		friendNotes.startUp();
	}

	@Override
	public void shutDown()
	{
		unsetNoticeBoard();
		eventBus.register(friendNotes);
		friendNotes.shutDown();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("tobnoticeboard"))
		{
			// Lobby highlighting has been disabled, reset the colors
			if (event.getKey().equals(CONFIG_KEY_HIGHLIGHT_LOBBY) && !config.highlightInLobby())
			{
				setLobbyColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
				return;
			}

			setNoticeBoard();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		clientThread.invokeLater(() ->
		{
			if (widgetLoaded.getGroupId() == NOTICE_BOARD_COMPONENT_ID || widgetLoaded.getGroupId() == LOBBY_COMPONENT_ID)
			{
				setNoticeBoard();
			}
		});
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_UPDATE || event.getScriptId() == ScriptID.IGNORE_UPDATE)
		{
			setNoticeBoard();
		}
	}

	private void setNoticeBoardColors(int friendColor, int clanColor, int ignoreColor)
	{
		for (int childID = 17; childID < 62; ++childID)
		{
			Widget noticeBoard = client.getWidget(NOTICE_BOARD_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					if (noticeBoardChild.getIndex() == 3)
					{
						updatePlayerName(Party.NOTICE_BOARD, noticeBoardChild, noticeBoard.getName(), friendColor, clanColor, ignoreColor);
					}
				}
			}
		}
	}

	private void setLobbyColors(int friendColor, int clanColor, int ignoreColor)
	{
		int[] children = {27, 42}; // 0 - lobby, 1 - lobby applicants

		for (int childID : children)
		{
			Widget noticeBoard = client.getWidget(LOBBY_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					// each row is 11 widgets long, the second (idx: 1) widget is the player name
					if (noticeBoardChild.getIndex() % 11 == 1)
					{
						updatePlayerName(Party.LOBBY, noticeBoardChild, noticeBoardChild.getText(), friendColor, clanColor, ignoreColor);
					}
				}
			}
		}
	}

	private void updatePlayerName(Party party, Widget noticeBoardChild, String nameText, int friendColor, int clanColor, int ignoreColor)
	{
		NameableContainer<Ignore> ignoreContainer = client.getIgnoreContainer();
		NameableContainer<Friend> friendContainer = client.getFriendContainer();
		String playerName = Text.removeTags(nameText).trim();
		String note = null;
		Integer noteIcon = null;

		// Don't highlight the local player
		if (playerName.equals(client.getLocalPlayer().getName()))
		{
			return;
		}

		// Fetch friend notes
		if (friendNotesEnabled)
		{
			note = friendNotes.getNote(playerName);
			noteIcon = friendNotes.getNoteIcon();

			log.debug("Player: {}, Note: {} ({})", playerName, note, noteIcon);
		}

		// Highlight friend/clan/ignored players
		if (ignoreContainer.findByName(playerName) != null)
		{
			noticeBoardChild.setTextColor(config.highlightIgnored() ? ignoreColor : DEFAULT_RGB);
		}
		else if (friendContainer.findByName(playerName) != null)
		{
			noticeBoardChild.setTextColor(config.highlightFriends() ? friendColor : DEFAULT_RGB);
		}
		else if (client.getFriendsChatManager() != null)
		{
			for (FriendsChatMember member : client.getFriendsChatManager().getMembers())
			{
				if (Text.toJagexName(member.getName()).equals(playerName))
				{
					noticeBoardChild.setTextColor(config.highlightClan() ? clanColor : DEFAULT_RGB);
				}
			}
		}

		// Add the note icon after the username (only shown on inside lobby widget)
		if (party.equals(Party.LOBBY) && !playerName.equals("-") && note != null && noteIcon != null)
		{
			noticeBoardChild.setText(playerName + " <img=" + noteIcon + ">");
		}
	}

	private void setNoticeBoard()
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

	private void unsetNoticeBoard()
	{
		setNoticeBoardColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
		setLobbyColors(DEFAULT_RGB, DEFAULT_RGB, DEFAULT_RGB);
	}

	@Provides
	TobNoticeBoardConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TobNoticeBoardConfig.class);
	}
}
