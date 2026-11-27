package info.mudbourn.mmsdungeons.dungeon;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// Answers whether a block lies in any of many boxes by bucketing them on a 16-block column grid.
public final class BoxIndex {

    private static final int SHIFT = 4;

    private final Long2ObjectOpenHashMap<List<BoundingBox>> columns = new Long2ObjectOpenHashMap<>();

    public BoxIndex(List<BoundingBox> boxes) {
        for (BoundingBox box : boxes) {
            for (int cx = box.minX() >> SHIFT; cx <= box.maxX() >> SHIFT; cx++) {
                for (int cz = box.minZ() >> SHIFT; cz <= box.maxZ() >> SHIFT; cz++) {
                    columns.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(box);
                }
            }
        }
    }

    public boolean contains(int x, int y, int z) {
        List<BoundingBox> boxes = columns.get(key(x >> SHIFT, z >> SHIFT));
        if (boxes == null) {
            return false;
        }
        for (BoundingBox box : boxes) {
            if (box.isInside(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
