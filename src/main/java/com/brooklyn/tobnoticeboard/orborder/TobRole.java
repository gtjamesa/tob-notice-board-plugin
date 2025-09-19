package com.brooklyn.tobnoticeboard.orborder;

import lombok.Getter;

public enum TobRole
{
	SFRZ(1406),
	MFRZ(1400), // 24x24
	RDPS(1404),
	MDPS(1401),
	MDPS2(1401),
	SOLO(5735);

	@Getter
	private final int spriteId;

	TobRole(int spriteId)
	{
		this.spriteId = spriteId;
	}
}
