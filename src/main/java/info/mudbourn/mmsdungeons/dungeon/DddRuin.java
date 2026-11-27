package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

// The kinds of labyrinth dungeon: a template theme for each of the upper and lower halves, with the Y band, tunnels and loot each half uses.
public enum DddRuin {
    LABYRINTH("ddd_labyrinth", Theme.PLAINS, false),
    LABYRINTH_DESERT("ddd_labyrinth_desert", Theme.DESERT, false),
    LABYRINTH_FROZEN("ddd_labyrinth_frozen", Theme.FROZEN, false),
    LABYRINTH_DEEP("ddd_labyrinth_deep", Theme.PLAINS, true),
    LABYRINTH_DESERT_DEEP("ddd_labyrinth_desert_deep", Theme.DESERT, true),
    LABYRINTH_FROZEN_DEEP("ddd_labyrinth_frozen_deep", Theme.FROZEN, true);

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
    private final boolean deep;

    DddRuin(String structure, Theme theme, boolean deep) {
        this.structure = MmsDungeons.id(structure);
        this.theme = theme;
        this.deep = deep;
    }

    public Theme theme() {
        return theme;
    }

    public int minY() {
        return deep ? -128 : 0;
    }

    public int maxY() {
        return deep ? DungeonDimension.PERICULUM_TOP : LabyrinthCells.TOP;
    }

    public int maxTunnels() {
        return deep ? DEEP_TUNNELS : UPPER_TUNNELS;
    }

    public int maxTunnelLength() {
        return TUNNEL_LENGTH;
    }

    public boolean richLoot() {
        return deep;
    }

    public boolean deep() {
        return deep;
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
