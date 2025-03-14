package com.brooklyn.tobnoticeboard.raids;

import com.brooklyn.tobnoticeboard.NameUpdater;
import com.brooklyn.tobnoticeboard.Party;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

public class ToaNoticeBoard extends NoticeBoard
{
	public static final int TOA_NOTICE_BOARD_COMPONENT_ID = 366;
	public static final int TOA_LOBBY_COMPONENT_ID = 52;

	public ToaNoticeBoard(Client client)
	{
		super(client);
	}

	@Override
	public void setNoticeBoardColors(int friendColor, int clanColor, int ignoreColor, NameUpdater nameUpdater)
	{
		for (int childID = 17; childID < 62; ++childID)
		{
			Widget noticeBoard = client.getWidget(TOA_NOTICE_BOARD_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					if (noticeBoardChild.getIndex() == 3)
					{
						nameUpdater.run(Party.TOA_NOTICE_BOARD, noticeBoardChild, noticeBoard.getName(), friendColor, clanColor, ignoreColor);
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
			Widget noticeBoard = client.getWidget(TOA_LOBBY_COMPONENT_ID, childID);

			if (noticeBoard != null && noticeBoard.getName() != null && noticeBoard.getChildren() != null)
			{
				for (Widget noticeBoardChild : noticeBoard.getChildren())
				{
					// each row is 11 widgets long, the second (idx: 1) widget is the player name
					if (noticeBoardChild.getIndex() % 11 == 1)
					{
						nameUpdater.run(Party.TOA_LOBBY, noticeBoardChild, noticeBoardChild.getText(), friendColor, clanColor, ignoreColor);
					}
				}
			}
		}
	}
}
