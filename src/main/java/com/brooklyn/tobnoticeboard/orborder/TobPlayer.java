package com.brooklyn.tobnoticeboard.orborder;

import lombok.Data;

@Data
public class TobPlayer
{
	private final String name;
	private int orb = -1; // current position
	private int savedOrb = -1; // position when raid started
	private TobRole role;
}
