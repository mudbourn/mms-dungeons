package info.mudbourn.mmsdungeons.dungeon;

// Lowers how often ore blocks touching air are thrown out while an ore vein generates in the dungeon.
public final class OreExposure {

    // Share of each ore's air-exposure discard chance that stays in the dungeon.
    public static final float DISCARD_SCALE = 0.6F;

    private static final ThreadLocal<Boolean> IN_DUNGEON = ThreadLocal.withInitial(() -> false);

    private OreExposure() {
    }

    public static void inDungeon(boolean value) {
        IN_DUNGEON.set(value);
    }

    public static float discardChance(float chance) {
        return IN_DUNGEON.get() ? chance * DISCARD_SCALE : chance;
    }
}
