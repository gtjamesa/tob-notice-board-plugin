package com.brooklyn.tobnoticeboard.raids;

import com.brooklyn.tobnoticeboard.NameUpdater;
import net.runelite.api.Client;

public abstract class NoticeBoard
{
	protected final Client client;

	public NoticeBoard(Client client)
	{
		this.client = client;
	}

	/**
	 * Set colors on the main notice board (party listing)
	 */
	abstract public void setNoticeBoardColors(int friendColor, int clanColor, int ignoreColor, NameUpdater nameUpdater);

	/**
	 * Set colors in the lobby (inside a party)
	 */
	abstract public void setLobbyColors(int friendColor, int clanColor, int ignoreColor, NameUpdater nameUpdater);
}
