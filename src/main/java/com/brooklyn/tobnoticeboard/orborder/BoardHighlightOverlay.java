/*
 * Copyright (c) 2018, Rheon <https://github.com/Rheon-D>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.brooklyn.tobnoticeboard.orborder;

import com.brooklyn.tobnoticeboard.TobNoticeBoardConfig;
import com.brooklyn.tobnoticeboard.data.NoticeBoard;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class BoardHighlightOverlay extends Overlay
{
	private final TobNoticeBoardConfig config;
	private final NoticeBoard noticeBoard;
	private final OrbOrderManager orbOrder;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private BoardHighlightOverlay(TobNoticeBoardConfig config, NoticeBoard noticeBoard, OrbOrderManager orbOrder, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.config = config;
		this.noticeBoard = noticeBoard;
		this.orbOrder = orbOrder;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		GameObject boardObject = noticeBoard.getGameObject();

		if (boardObject == null || !config.orbOrderEnabled() || !orbOrder.getOrbStatus().equals(OrbStatus.INCORRECT))
		{
			return null;
		}

		String text = "The orb order has changed!";
		Point textLocation = boardObject.getCanvasTextLocation(graphics, text, boardObject.getRenderable().getModelHeight() + 40);
		Color color = config.noticeBoardHighlightColor();

		if (config.orbOrderDebug())
		{
			text = text + " (" + orbOrder.getOrbStatus() + ")";
		}

		modelOutlineRenderer.drawOutline(boardObject, 2, color, 1);
		OverlayUtil.renderTextLocation(graphics, textLocation, text, color);

		return null;
	}
}
