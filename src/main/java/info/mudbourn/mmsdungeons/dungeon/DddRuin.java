package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

// The kinds of labyrinth dungeon: a template theme for each of the upper, lower and Sanctuary labyrinths, with the Y band, tunnels and loot each uses.
public enum DddRuin {
    LABYRINTH("ddd_labyrinth", Theme.PLAINS, Level.UPPER),
    LABYRINTH_DESERT("ddd_labyrinth_desert", Theme.DESERT, Level.UPPER),
    LABYRINTH_FROZEN("ddd_labyrinth_frozen", Theme.FROZEN, Level.UPPER),
    LABYRINTH_DEEP("ddd_labyrinth_deep", Theme.PLAINS, Level.DEEP),
    LABYRINTH_DESERT_DEEP("ddd_labyrinth_desert_deep", Theme.DESERT, Level.DEEP),
    LABYRINTH_FROZEN_DEEP("ddd_labyrinth_frozen_deep", Theme.FROZEN, Level.DEEP),
    LABYRINTH_SANCTUARY("ddd_labyrinth_sanctuary", Theme.PLAINS, Level.SANCTUARY),
    LABYRINTH_DESERT_SANCTUARY("ddd_labyrinth_desert_sanctuary", Theme.DESERT, Level.SANCTUARY),
    LABYRINTH_FROZEN_SANCTUARY("ddd_labyrinth_frozen_sanctuary", Theme.FROZEN, Level.SANCTUARY);

    // Which labyrinth of a cell a kind is: the Deep Region's upper half, its lower half, or the Sanctuary Region's.
    public enum Level {
        UPPER,
        DEEP,
        SANCTUARY
    }

    // Which family of Deadly Deadly Dungeon templates a labyrinth is built from.
    public enum Theme {
        PLAINS,
        DESERT,
        FROZEN
    }

    private static final int UPPER_TUNNELS = 4;
    private static final int DEEP_TUNNELS = 6;
    private static final int TUNNEL_LENGTH = 160;
    private static final ThreadLocal<DddRuin> PLACING = new ThreadLocal<>();
    private static final ThreadLocal<DddMaterial> PLACING_MATERIAL = new ThreadLocal<>();

    private final Identifier structure;
    private final Theme theme;
    private final Level level;

    DddRuin(String structure, Theme theme, Level level) {
        this.structure = MmsDungeons.id(structure);
        this.theme = theme;
        this.level = level;
    }

    public Theme theme() {
        return theme;
    }

    public int minY() {
        return switch (level) {
            case UPPER -> 0;
            case DEEP -> -128;
            case SANCTUARY -> DungeonDimension.SANCTUARY_BOTTOM;
        };
    }

    public int maxY() {
        return switch (level) {
            case UPPER -> LabyrinthCells.TOP;
            case DEEP -> DungeonDimension.PERICULUM_TOP;
            case SANCTUARY -> LabyrinthCells.SANCTUARY_TOP;
        };
    }

    public int maxTunnels() {
        return level == Level.DEEP ? DEEP_TUNNELS : UPPER_TUNNELS;
    }

    public int maxTunnelLength() {
        return TUNNEL_LENGTH;
    }

    public boolean richLoot() {
        return level != Level.UPPER;
    }

    public boolean deep() {
        return level == Level.DEEP;
    }

    public boolean sanctuary() {
        return level == Level.SANCTUARY;
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

    // The block set for the start being placed on this thread, or null.
    public static @Nullable DddMaterial placingMaterial() {
        return PLACING_MATERIAL.get();
    }

    public static void setPlacing(@Nullable DddRuin ruin, @Nullable DddMaterial material) {
        if (ruin == null) {
            PLACING.remove();
            PLACING_MATERIAL.remove();
        } else {
            PLACING.set(ruin);
            PLACING_MATERIAL.set(material);
        }
    }
}
