package com.mithrilmania.blocktopograph.map;

import android.graphics.Color;
import android.util.SparseArray;

import androidx.annotation.ColorInt;


/*
Biome enum for MCPE -- by @mithrilmania
Reference link: https://minecraft.fandom.com/wiki/Biome/ID

--- Please attribute @mithrilmania for generating+updating this enum
 */
public enum Biome {

	OCEAN(0, "Ocean", Color.rgb(2, 0, 112)),
	PLAINS(1, "Plains", Color.rgb(140, 176, 96)),
	DESERT(2, "Desert", Color.rgb(251, 148, 27)),
	EXTREME_HILLS(3, "Mountains", Color.rgb(93, 99, 93)),
	FOREST(4, "Forest", Color.rgb(2, 99, 32)),
	TAIGA(5, "Taiga", Color.rgb(9, 102, 91)),
	SWAMPLAND(6, "Swampland", Color.rgb(4, 200, 139)),
	RIVER(7, "River", Color.rgb(1, 1, 255)),
	HELL(8, "Nether Wastes", Color.rgb(132, 65, 65)),
	THE_END(9, "The End", Color.rgb(130, 129, 254)),
	FROZEN_OCEAN(10, "Frozen Ocean", Color.rgb(142, 141, 161)),
	FROZEN_RIVER(11, "Frozen River", Color.rgb(159, 163, 255)),
	ICE_PLAINS(12, "Ice Plains", Color.rgb(255, 254, 255)),
	ICE_MOUNTAINS(13, "Ice Mountains", Color.rgb(162, 157, 157)),
	MUSHROOM_ISLAND(14, "Mushroom Fields", Color.rgb(254, 1, 255)),
	MUSHROOM_ISLAND_SHORE(15, "Mushroom Fields Shore", Color.rgb(158, 3, 253)),
	BEACH(16, "Beach", Color.rgb(250, 223, 85)),
	DESERT_HILLS(17, "Desert Hills", Color.rgb(212, 94, 15)),
	FOREST_HILLS(18, "Wooded Hills", Color.rgb(37, 86, 30)),
	TAIGA_HILLS(19, "Taiga Hills", Color.rgb(25, 54, 49)),
	EXTREME_HILLS_EDGE(20, "Mountain Edge", Color.rgb(115, 118, 157)),
	JUNGLE(21, "Jungle", Color.rgb(82, 122, 7)),
	JUNGLE_HILLS(22, "Jungle Hills", Color.rgb(46, 64, 3)),
	JUNGLE_EDGE(23, "Jungle Edge", Color.rgb(99, 142, 24)),
	DEEP_OCEAN(24, "Deep Ocean", Color.rgb(2, 0, 47)),
	STONE_BEACH(25, "Stone Shore", Color.rgb(162, 164, 132)),
	COLD_BEACH(26, "Snowy Beach", Color.rgb(250, 238, 193)),
	BIRCH_FOREST(27, "Birch Forest", Color.rgb(48, 117, 70)),
	BIRCH_FOREST_HILLS(28, "Birch Forest Hills", Color.rgb(29, 94, 51)),
	ROOFED_FOREST(29, "Dark Forest", Color.rgb(66, 82, 24)),
	COLD_TAIGA(30, "Snowy Taiga", Color.rgb(49, 85, 75)),
	COLD_TAIGA_HILLS(31, "Snowy Taiga Hills", Color.rgb(34, 61, 52)),
	MEGA_TAIGA(32, "Giant Tree Taiga", Color.rgb(92, 105, 84)),
	MEGA_TAIGA_HILLS(33, "Giant Tree Taiga Hills", Color.rgb(70, 76, 59)),
	EXTREME_HILLS_PLUS_TREE(34, "Wooded Mountains", Color.rgb(79, 111, 81)),
	SAVANNA(35, "Savanna", Color.rgb(192, 180, 94)),
	SAVANNA_PLATEAU(36, "Savanna Plateau", Color.rgb(168, 157, 98)),
	MESA(37, "Badlands", Color.rgb(220, 66, 19)),
	MESA_PLATEAU(38, "Badlands Plateau", Color.rgb(174, 152, 100)),
	MESA_PLATEAU_STONE(39, "Wooded Badlands Plateau", Color.rgb(202, 139, 98)),

	//fix the colors for these
	WARM_OCEAN(40, "Warm Ocean", Color.rgb(202, 139, 98)),
	LUKEWARM_OCEAN(41, "Lukewarm Ocean", Color.rgb(202, 139, 98)),
	COLD_OCEAN(42, "Cold Ocean", Color.rgb(202, 139, 98)),
	DEEP_WARM_OCEAN(43, "Deep Warm Ocean", Color.rgb(202, 139, 98)),
	DEEP_LUKEWARM_OCEAN(44, "Deep Lukewarm Ocean", Color.rgb(202, 139, 98)),
	DEEP_COLD_OCEAN(45, "Deep Cold Ocean", Color.rgb(202, 139, 98)),
	DEEP_FROZEN_OCEAN(46, "Deep Frozen Ocean", Color.rgb(202, 139, 98)),
	LEGACY_FROZEN_OCEAN(47, "Legacy Frozen Ocean", Color.rgb(202, 139, 98)),

	//  Not sure are available ( need tester )
//	OCEAN_M(128, "Ocean M", Color.rgb(81, 79, 195)),

	SUNFLOWER_PLAINS(129, "Sunflower Plains", Color.rgb(220, 255, 177)),
	DESERT_MUTATED(130, "Desert Lakes", Color.rgb(255, 230, 101)),
	EXTREME_HILLS_MUTATED(131, "Gravelly Mountains", Color.rgb(177, 176, 174)),
	FLOWER_FOREST(132, "Flower Forest", Color.rgb(82, 180, 110)),
	TAIGA_MUTATED(133, "Taiga Mountains", Color.rgb(90, 182, 171)),
	SWAMPLAND_MUTATED(134, "Swamp Hills", Color.rgb(87, 255, 255)),

	// Not sure available ( need tester )
//	RIVER_M(135, "River M", Color.rgb(82, 79, 255)),
//	HELL_M(136, "Hell M", Color.rgb(255, 80, 83)),
//	SKY_M(137, "Sky M", Color.rgb(210, 211, 255)),
//	FROZEN_OCEAN_M(138, "Frozen Ocean M", Color.rgb(226, 224, 241)),
//	FROZEN_RIVER_M(139, "Frozen River M", Color.rgb(239, 242, 255)),

	ICE_PLAINS_SPIKES(140, "Ice Spikes", Color.rgb(223, 255, 255)),

	// Not sure available ( need tester )
//	ICE_MOUNTAINS_M(141, "Ice Mountains M", Color.rgb(237, 237, 238)),
//	MUSHROOM_ISLAND_M(142, "Mushroom Island M", Color.rgb(255, 82, 255)),
//	MUSHROOM_ISLAND_SHORE_M(143, "Mushroom Island Shore M", Color.rgb(243, 82, 255)),
//	BEACH_M(144, "Beach M", Color.rgb(255, 255, 162)),
//	DESERT_HILLS_M(145, "Desert Hills M", Color.rgb(255, 177, 100)),
//	FOREST_HILLS_M(146, "Forest Hills M", Color.rgb(113, 167, 109)),
//	TAIGA_HILLS_M(147, "Taiga Hills M", Color.rgb(103, 135, 134)),
//	EXTREME_HILLS_EDGE_M(148, "Extreme Hills Edge M", Color.rgb(196, 203, 234)),

	JUNGLE_MUTATED(149, "Modified Jungle", Color.rgb(160, 203, 92)),
	//	JUNGLE_HILLS_M(150, "Jungle Hills M", Color.rgb(127, 146, 86)),
	JUNGLE_EDGE_MUTATED(151, "Modified Jungle Edge", Color.rgb(179, 217, 105)),
	//	DEEP_OCEAN_M(152, "Deep Ocean M", Color.rgb(82, 79, 130)),
//	STONE_BEACH_M(153, "Stone Beach M", Color.rgb(242, 243, 209)),
//	COLD_BEACH_M(154, "Cold Beach M", Color.rgb(255, 255, 255)),
	BIRCH_FOREST_MUTATED(155, "Tall Birch Forest", Color.rgb(131, 194, 148)),
	BIRCH_FOREST_HILLS_MUTATED(156, "Tall Birch Hills", Color.rgb(111, 175, 133)),
	ROOFED_FOREST_MUTATED(157, "Dark Forest Hills", Color.rgb(143, 158, 109)),
	COLD_TAIGA_MUTATED(158, "Snowy Taiga Mountains", Color.rgb(132, 163, 156)),
	//	COLD_TAIGA_HILLS_M(159, "Cold Taiga Hills M", Color.rgb(113, 143, 136)),
	REDWOOD_TAIGA_MUTATED(160, "Giant Spruce Taiga", Color.rgb(168, 180, 164)),
	REDWOOD_TAIGA_HILLS_MUTATED(161, "Giant Spruce Taiga Hills", Color.rgb(150, 158, 140)),
	EXTREME_HILLS_PLUS_TREE_MUTATED(162, "Gravelly Mountains", Color.rgb(161, 194, 158)),
	SAVANNA_MUTATED(163, "Shattered Savanna", Color.rgb(255, 255, 173)),
	SAVANNA_PLATEAU_MUTATED(164, "Shattered Savanna Plateau", Color.rgb(247, 238, 180)),
	MESA_BRYCE(165, "Eroded Badlands", Color.rgb(255, 151, 101)),
	MESA_PLATEAU_MUTATED(166, "Modified Badlands Plateau", Color.rgb(255, 234, 179)),
	MESA_PLATEAU_STONE_MUTATED(167, "Modified Wooded Badlands Plateau", Color.rgb(255, 220, 184)),
	BAMBOO_JUNGLE(168, "Bamboo Jungle", Color.rgb(255, 220, 184)),
	BAMBOO_JUNGLE_HILLS(169, "Bamboo Jungle Hills", Color.rgb(255, 220, 184)),

	// 1.16 BIOME
	SOUL_SAND_VALLEY(178, "Soul Sand Valley", Color.rgb(66, 113, 114)),
	CRIMSON_FOREST(179, "Crimson Forest", Color.rgb(141, 30, 40)),
	WARPED_FOREST(180, "Warped Forest", Color.rgb(22, 126, 134)),
	BASALT_DELTAS(181, "Basalt Deltas", Color.rgb(75, 69, 71)),

	// 1.17 BIOME ?
	JAGGED_PEAKS(182, "Jagged Peaks", Color.rgb(0, 0, 0)),
	FROZEN_PEAKS(183, "Frozen Peaks", Color.rgb(0, 0, 0)),
	SNOWY_SLOPES(184, "Snowy Slopes", Color.rgb(0, 0, 0)),
	GROVE(185, "Grove", Color.rgb(0, 0, 0)),
	MEADOW(186, "Meadow", Color.rgb(0, 0, 0)),
	LUSH_CAVES(187, "Lush Caves", Color.rgb(0, 0, 0)),
	DRIPSTONE_CAVES(188, "Dripstone Caves", Color.rgb(0, 0, 0)),
	STONY_PEAKS(189, "Stony Peaks", Color.rgb(0, 0, 0))
	;

	private static final SparseArray<Biome> biomeMap;

	static {
		biomeMap = new SparseArray<>();
		for (Biome b : Biome.values()) {
			biomeMap.put(b.id, b);
		}
	}

	public final int id;
	public final String name;
	@ColorInt
	public final int color;

	Biome(int id, String name, @ColorInt int color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}

	public static Biome getBiome(int id) {
		return biomeMap.get(id);
	}


	public String getName() {
		return name;
	}
}
