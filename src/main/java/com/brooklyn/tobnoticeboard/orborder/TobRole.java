package com.brooklyn.tobnoticeboard.orborder;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TobRole
{
	SFRZ("SFRZ", List.of("sfrz", "frz"), 1406),
	MFRZ("MFRZ", List.of("mfrz", "nfrz"), 1400), // 24x24
	RDPS("RDPS", List.of("rdps", "range"), 1404),
	MDPS("MDPS", List.of("mdps", "melee"), 1401),
	SOLO("SOLO", null, 5735);

	private final String name;
	private final List<String> aliases;
	private final int spriteId;
}
