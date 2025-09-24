package com.brooklyn.tobnoticeboard.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

@Singleton
public class NoticeBoard
{
	@Getter
	private final WorldPoint worldLocation = new WorldPoint(3662, 3219, 0);

	@Getter
	private GameObject gameObject;

	@Inject
	private Client client;

	public GameObject find()
	{
		if (gameObject != null)
		{
			return gameObject;
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldLocation);
		if (localPoint == null)
		{
			return null;
		}

		Tile[][][] tiles = worldView.getScene().getTiles();
		Tile tile = tiles[worldView.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];

		for (GameObject obj : tile.getGameObjects())
		{
			if (obj != null && obj.getId() == ObjectID.TOB_SURFACE_NOTICE_BOARD)
			{
				gameObject = obj;
				break;
			}
		}

		return gameObject;
	}
}
