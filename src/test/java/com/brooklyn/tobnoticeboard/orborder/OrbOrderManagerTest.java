package com.brooklyn.tobnoticeboard.orborder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.Client;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
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
		when(client.getVarcStrValue(330)).thenReturn("Lynx Titan");
		when(client.getVarcStrValue(331)).thenReturn("Hey Jase");
		when(client.getVarcStrValue(332)).thenReturn("ShawnBay");
	}

	@Test
	public void shouldFetchPlayerNames()
	{
		when(client.getVarcStrValue(330)).thenReturn("Lynx Titan");
		when(client.getVarcStrValue(331)).thenReturn("Hey Jase");
		when(client.getVarcStrValue(332)).thenReturn("ShawnBay");

		String[] playerNames = orbOrderManager.fetchPlayerNames();
		assertEquals(3, playerNames.length);
		assertEquals("Lynx Titan", playerNames[0]);
		assertEquals("Hey Jase", playerNames[1]);
		assertEquals("ShawnBay", playerNames[2]);
	}

	@Test
	public void shouldSavePlayersWithRoles()
	{
		setupTrio();

		orbOrderManager.startRaid();

		assertEquals(3, orbOrderManager.getLastPlayerCount());
		assertEquals("Hey Jase", orbOrderManager.getLastRaidPlayers()[1].getName());
		assertEquals(TobRole.RDPS, orbOrderManager.getLastRaidPlayers()[1].getRole());
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

		when(client.getVarcStrValue(331)).thenReturn("ShawnBay");
		when(client.getVarcStrValue(332)).thenReturn("Hey Jase");

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.INCORRECT, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeNewPlayersWithAdditionalPlayers()
	{
		setupTrio();

		orbOrderManager.startRaid();

		when(client.getVarcStrValue(330)).thenReturn("Lynx Titan");
		when(client.getVarcStrValue(331)).thenReturn("Hey Jase");
		when(client.getVarcStrValue(332)).thenReturn("ShawnBay");
		when(client.getVarcStrValue(333)).thenReturn("senZe");
		when(client.getVarcStrValue(334)).thenReturn("Karma");

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.NEW_PLAYERS, orbOrderManager.getOrbStatus());
	}

	@Test
	public void shouldBeOkWhenPlayersLeave()
	{
		when(client.getVarcStrValue(330)).thenReturn("Lynx Titan");
		when(client.getVarcStrValue(331)).thenReturn("Hey Jase");
		when(client.getVarcStrValue(332)).thenReturn("ShawnBay");
		when(client.getVarcStrValue(333)).thenReturn("senZe");
		when(client.getVarcStrValue(334)).thenReturn("Karma");

		orbOrderManager.startRaid();

		// orbs have swapped here, but players have left so we'll recruit or fix the order when player count matches
		setupTrio();
		lenient().when(client.getVarcStrValue(333)).thenReturn(null);
		lenient().when(client.getVarcStrValue(334)).thenReturn(null);

		orbOrderManager.createPlayers();

		assertEquals(OrbStatus.OK, orbOrderManager.getOrbStatus());
	}
}
