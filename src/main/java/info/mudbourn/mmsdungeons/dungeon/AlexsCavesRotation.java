package info.mudbourn.mmsdungeons.dungeon;

// Gives each Alex's Caves biome a distance from spawn that rotates every Deep Region rebuild.
public final class AlexsCavesRotation {

    // Distance tiers from spawn, nearest first, one per biome.
    private static final double[] TIERS = {800.0, 900.0, 1000.0, 1100.0, 1200.0, 1300.0};
    public static final double SAME_BIOME_FACTOR = 2.0;

    private AlexsCavesRotation() {
    }

    // The biome that has gone longest without the nearest tier takes it, and the rest follow in the same order.
    public static double distanceFor(int rarityOffset) {
        return TIERS[Math.floorMod(rarityOffset - DungeonGeneration.current(), TIERS.length)];
    }

    public static double spacingFor(int rarityOffset) {
        return distanceFor(rarityOffset) * SAME_BIOME_FACTOR;
    }
}
