package com.brooklyn.tobnoticeboard.raids;

import com.brooklyn.tobnoticeboard.NameUpdater;
import com.brooklyn.tobnoticeboard.Party;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

public class TobNoticeBoard extends NoticeBoard
{
	public static final int TOB_NOTICE_BOARD_COMPONENT_ID = 364;
	public static final int TOB_LOBBY_COMPONENT_ID = 50;

	public TobNoticeBoard(Client client)
	{
		super(client);
	}

	@Override
	public void setNoticeBoardColors(int friendColor, int clanColor, int ignoreColor, NameUpdater nameUpdater)
	{
		for (int childID = 17; childID < 62; ++childID)
		{
			Widget noticeBoard = client.getWidget(TOB_NOTICE_BOARD_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					if (noticeBoardChild.getIndex() == 3)
					{
						nameUpdater.run(Party.TOB_NOTICE_BOARD, noticeBoardChild, noticeBoard.getName(), friendColor, clanColor, ignoreColor);
					}
				}
			}
		}
	}

	@Override
	public void setLobbyColors(int friendColor, int clanColor, int ignoreColor, NameUpdater nameUpdater)
	{
		int[] children = {27, 42}; // 0 - lobby, 1 - lobby applicants

		for (int childID : children)
		{
			Widget noticeBoard = client.getWidget(TOB_LOBBY_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					// each row is 11 widgets long, the second (idx: 1) widget is the player name
					if (noticeBoardChild.getIndex() % 11 == 1)
					{
						nameUpdater.run(Party.TOB_LOBBY, noticeBoardChild, noticeBoardChild.getText(), friendColor, clanColor, ignoreColor);
					}
				}
			}
		}
	}
}
