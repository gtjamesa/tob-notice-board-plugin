package com.brooklyn.tobnoticeboard;

import lombok.Getter;

public enum RaidStatus
{
	NOT_IN_PARTY(0),
	IN_PARTY(1),
	INSIDE(2),
	DEAD_SPECTATING(3);

	@Getter
	private final int value;

	RaidStatus(int value)
	{
		this.value = value;
	}

	public static RaidStatus fromInt(int value)
	{
		for (RaidStatus role : RaidStatus.values())
		{
			if (role.value == value)
			{
				return role;
			}
		}

		throw new IllegalArgumentException("Invalid PartyStatus value: " + value);
	}
}
