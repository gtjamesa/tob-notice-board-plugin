package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.Constant;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OrbOrderManagerTest
{
	@Mock
	@Bind
	private Client client;

	@Inject
	OrbOrderManager orbOrderManager;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	private void setupTrio()
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>Hey Jase<br>ShawnBay<br>-<br>-");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);
	}

	@Test
	public void shouldFetchPlayerNames()
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>Hey Jase<br>ShawnBay<br>-<br>-");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);

		List<String> playerNames = orbOrderManager.fetchPlayerNames();
		assertEquals(3, playerNames.size());
		assertEquals("Lynx Titan", playerNames.get(0));
		assertEquals("Hey Jase", playerNames.get(1));
		assertEquals("ShawnBay", playerNames.get(2));
	}

	@Test
	public void shouldSavePlayersWithRoles()
	{
		setupTrio();

		orbOrderManager.startRaid();

		assertEquals(3, orbOrderManager.getLastPlayerCount());
		assertEquals("Hey Jase", orbOrderManager.getLastParty()[1]);

		TobPlayer player = orbOrderManager.getPlayer("Hey Jase");
		assertEquals("Hey Jase", player.getName());
		assertEquals(TobRole.RDPS, player.getRole());
	}

	@Test
	public void shouldBeOkWhenOrderMatches()
	{
		setupTrio();

		orbOrderManager.startRaid();
		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeIncorrectWhenOrderChanges()
	{
		setupTrio();

		orbOrderManager.startRaid();

		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>ShawnBay<br>Hey Jase<br>-<br>-");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.INCORRECT, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeNewPlayersWithAdditionalPlayers()
	{
		setupTrio();

		orbOrderManager.startRaid();

		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>Hey Jase<br>ShawnBay<br>senZe<br>Karma");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.NEW_PLAYERS, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeOkWhenPlayersLeave()
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>Hey Jase<br>ShawnBay<br>senZe<br>Karma");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);

		orbOrderManager.startRaid();

		// orbs have swapped here, but players have left so we'll recruit or fix the order when player count matches
		Widget widget2 = mock(Widget.class);
		when(widget2.getText()).thenReturn("Lynx Titan<br>ShawnBay<br>Hey Jase<br>-<br>-");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget2);

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldPrunePlayerMap()
	{
		setupTrio();

		orbOrderManager.startRaid();

		// new mdps has replaced shawnbay
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn("Lynx Titan<br>Hey Jase<br>senZe<br>-<br>-");
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);

		orbOrderManager.startRaid();

		assertNotNull(orbOrderManager.getPlayer("Lynx Titan"));
		assertNull(orbOrderManager.getPlayer("ShawnBay"));
		assertEquals(TobRole.MDPS, orbOrderManager.getPlayer("senZe").getRole());
	}
}
