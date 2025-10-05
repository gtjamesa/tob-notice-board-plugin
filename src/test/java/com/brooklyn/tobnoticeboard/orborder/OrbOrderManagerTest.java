package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.Constant;
import com.brooklyn.tobnoticeboard.TobNoticeBoardConfig;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayManager;
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

	@Mock
	@Bind
	private TobNoticeBoardConfig config;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Inject
	OrbOrderManager orbOrderManager;

	@Before
	public void before()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
	}

	private void setupPlayers(String[] players)
	{
		Widget widget = mock(Widget.class);
		when(widget.getText()).thenReturn(String.join("<br>", players));
		when(client.getWidget(Constant.TOB_HUD_COMPONENT_ID, Constant.TOB_HUD_CHILD_COMPONENT_ID)).thenReturn(widget);
	}

	private void setupTrio()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay"});
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

		// same players, orb order incorrect
		setupPlayers(new String[]{"Lynx Titan", "ShawnBay", "Hey Jase"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.INCORRECT, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeOkIfNewPlayerReplacesRole()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe"});

		orbOrderManager.startRaid();

		// new rdps, other roles are correct
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "Karma", "senZe"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeOkIfMultiplePlayersReplaceRole()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe"});

		orbOrderManager.startRaid();

		// new sfrz/rdps, other roles are correct
		setupPlayers(new String[]{"Zezima", "Hey Jase", "Karma", "senZe"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeIncorrectIfMultiplePlayersReplaceRoleAndOrderChanges()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe"});

		orbOrderManager.startRaid();

		// new sfrz/rdps, mfrz/mdps are in incorrect order
		setupPlayers(new String[]{"Zezima", "senZe", "Karma", "Hey Jase"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.INCORRECT, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeNewPlayersWithAdditionalPlayers()
	{
		setupTrio();

		orbOrderManager.startRaid();

		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe", "Karma"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.NEW_PLAYERS, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeOkWhenPlayersLeave()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe", "Karma"});

		orbOrderManager.startRaid();

		// orbs have swapped here, but players have left so we'll recruit or fix the order when player count matches
		setupPlayers(new String[]{"Lynx Titan", "ShawnBay", "Hey Jase"});

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldPrunePlayerMap()
	{
		setupTrio();

		orbOrderManager.startRaid();

		// new mdps has replaced shawnbay
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "senZe"});

		orbOrderManager.startRaid();

		assertNotNull(orbOrderManager.getPlayer("Lynx Titan"));
		assertNull(orbOrderManager.getPlayer("ShawnBay"));
		assertEquals(TobRole.MDPS, orbOrderManager.getPlayer("senZe").getRole());
	}

	@Test
	public void shouldFormatPartyString()
	{
		setupPlayers(new String[]{"Lynx Titan", "Hey Jase", "ShawnBay", "senZe", "Karma"});

		orbOrderManager.startRaid();

		assertEquals("Lynx Titan (SFRZ), Hey Jase (MFRZ), ShawnBay (RDPS), senZe (MDPS), Karma (MDPS)", orbOrderManager.getFormattedParty());
	}
}
