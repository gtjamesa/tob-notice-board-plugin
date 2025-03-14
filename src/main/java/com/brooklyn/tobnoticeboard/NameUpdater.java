package com.brooklyn.tobnoticeboard;

import net.runelite.api.widgets.Widget;

@FunctionalInterface
public interface NameUpdater
{
	void run(Party party, Widget widget, String nameText, int friendColor, int clanColor, int ignoreColor);
}