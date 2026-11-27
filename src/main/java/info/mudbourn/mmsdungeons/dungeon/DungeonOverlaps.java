package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Keeps the dungeon's other structures out of labyrinth cells.
public final class DungeonOverlaps {

    // The way into Aerial Hell; it always stays.
    private static final Identifier ABANDONED_PORTAL = MmsDungeons.id("cavern_abandoned_portal");

    private DungeonOverlaps() {
    }

    public static boolean clashes(Identifier structure, BoundingBox box) {
        return !structure.equals(ABANDONED_PORTAL) && LabyrinthCells.touchesLabyrinth(box);
    }
}
