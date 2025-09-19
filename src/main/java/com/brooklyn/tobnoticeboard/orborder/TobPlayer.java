package com.brooklyn.tobnoticeboard.orborder;

import lombok.Getter;
import lombok.Setter;

@Getter
public class TobPlayer
{
	private String name;
	private int index;

	@Setter
	private TobRole role;

	public TobPlayer(String name, int index)
	{
		this.name = name;
		this.index = index;
	}
}
