package com.brooklyn.tobnoticeboard;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RaidNoticeBoardPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RaidNoticeBoardPlugin.class);
		RuneLite.main(args);
	}
}