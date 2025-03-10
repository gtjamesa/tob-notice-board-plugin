package com.brooklyn.tobnoticeboard.friendnotes;

import com.brooklyn.tobnoticeboard.TobNoticeBoardConfig;
import com.brooklyn.tobnoticeboard.TobNoticeBoardPlugin;
import com.google.common.base.Strings;
import java.awt.image.BufferedImage;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

/**
 * Adapter to interact with the default RuneLite "Friend Notes" plugin.
 *
 * @see net.runelite.client.plugins.friendnotes.FriendNotesPlugin
 */
@Slf4j
public class FriendNoteManager
{
	private static final String FRIEND_NOTES_CONFIG_GROUP = "friendNotes";
	private static final String FRIEND_NOTES_KEY_PREFIX = "note_";
	private static final int ICON_WIDTH = 14;
	private static final int ICON_HEIGHT = 12;
	private int iconId = -1;
	private int chatIconIndex = -1;

	@Inject
	private TobNoticeBoardConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatIconManager chatIconManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FriendNoteOverlay overlay;

	@Inject
	private PluginManager pluginManager;

	@Getter
	private HoveredFriend hoveredFriend = null;

	public void startUp()
	{
		log.debug("FriendNoteManager::startUp");
		overlayManager.add(overlay);
		loadIcon();
	}

	public void shutDown()
	{
		overlayManager.remove(overlay);
	}

	/**
	 * Get the friend note of a display name, or null if no friend note exists for it.
	 */
	@Nullable
	public String getNote(String displayName)
	{
		final String sanitizedName = Text.toJagexName(Text.removeTags(displayName));
		return configManager.getConfiguration(FRIEND_NOTES_CONFIG_GROUP, FRIEND_NOTES_KEY_PREFIX + sanitizedName);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final int groupId = WidgetUtil.componentToInterface(event.getActionParam1());
//		log.debug("onMenuEntryAdded: {} / {}", event.getActionParam1(), groupId);

		// Look for "Message" on friends list
		if (groupId == TobNoticeBoardPlugin.LOBBY_COMPONENT_ID)
		{
			setHoveredFriend(event.getTarget());
		}
	}

	/**
	 * Set the currently hovered display name, if a friend note exists for it.
	 */
	private void setHoveredFriend(String displayName)
	{
		hoveredFriend = null;

		if (!Strings.isNullOrEmpty(displayName))
		{
			final String note = getNote(displayName);
			if (note != null)
			{
				hoveredFriend = new HoveredFriend(displayName, note);
			}
		}
	}

	public Integer getNoteIcon()
	{
		if (chatIconIndex != -1)
		{
			return chatIconIndex;
		}

		if (iconId != -1)
		{
			chatIconIndex = chatIconManager.chatIconIndex(iconId);
			return chatIconIndex;
		}

		return null;
	}

	public void loadIcon()
	{
		if (iconId != -1)
		{
			return;
		}

		final BufferedImage iconImg = ImageUtil.loadImageResource(TobNoticeBoardPlugin.class, "/note_icon.png");
		if (iconImg == null)
		{
			throw new RuntimeException("unable to load icon");
		}

		final BufferedImage resized = ImageUtil.resizeImage(iconImg, ICON_WIDTH, ICON_HEIGHT);
		iconId = chatIconManager.registerChatIcon(resized);
	}

	public boolean isEnabled()
	{
		if (!config.friendNotes()) {
			return false;
		}

		final Optional<Plugin> friendNotePlugin = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Friend Notes")).findFirst();
		return friendNotePlugin.isPresent() && pluginManager.isPluginEnabled(friendNotePlugin.get());
	}
}
