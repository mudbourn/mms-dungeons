package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

// The two kinds of Deadly Deadly Dungeon ruin and the Y band, tunnel count and block set each one uses.
public enum DddRuin {
    STONE("ddd_plains", 0, DungeonDimension.SANCTUARY_BOTTOM - 1, 4),
    DEEPSLATE("ddd_plains_deepslate", -128, DungeonDimension.PERICULUM_TOP, 6);

    private static final ThreadLocal<DddRuin> PLACING = new ThreadLocal<>();

    private final Identifier structure;
    private final int minY;
    private final int maxY;
    private final int maxTunnels;

    DddRuin(String structure, int minY, int maxY, int maxTunnels) {
        this.structure = MmsDungeons.id(structure);
        this.minY = minY;
        this.maxY = maxY;
        this.maxTunnels = maxTunnels;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int maxTunnels() {
        return maxTunnels;
    }

    public static @Nullable DddRuin of(Identifier structure) {
        for (DddRuin ruin : values()) {
            if (ruin.structure.equals(structure)) {
                return ruin;
            }
        }
        return null;
    }

    // The ruin whose start is being placed on this thread, or null.
    public static @Nullable DddRuin placing() {
        return PLACING.get();
    }

    public static void setPlacing(@Nullable DddRuin ruin) {
        if (ruin == null) {
            PLACING.remove();
        } else {
            PLACING.set(ruin);
        }
    }
}
