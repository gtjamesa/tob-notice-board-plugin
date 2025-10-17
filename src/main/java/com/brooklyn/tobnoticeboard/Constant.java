package com.brooklyn.tobnoticeboard;

import net.runelite.api.gameval.InterfaceID;

public class Constant
{
	public static final String CONFIG_GROUP = "tobnoticeboard";
	public static final String CONFIG_KEY_HIGHLIGHT_LOBBY = "highlightInLobby";
	public static final String CONFIG_KEY_FRIEND_NOTES = "friendNotes";
	public static final String CONFIG_KEY_ORB_ORDER_ENABLED = "orbOrderEnabled";
	public static final String CONFIG_KEY_ORB_DEBUG_ENABLED = "orbOrderDebug";
	public static final String CONFIG_KEY_ORB_SHOW_ROLE_NAME = "orbOrderShowRoleName";

	public static final int NOTICE_BOARD_COMPONENT_ID = InterfaceID.TOB_PARTYLIST;
	public static final int LOBBY_COMPONENT_ID = InterfaceID.TOB_PARTYDETAILS;
	public static final int TOB_HUD_COMPONENT_ID = InterfaceID.TOB_HUD;
	public static final int TOB_HUD_CHILD_COMPONENT_ID = 12;
	public static final int TOB_MAX_PARTY_SIZE = 5;

	public static final int SCRIPT_ID_TOB_HUD_DRAW = 2297;
}
