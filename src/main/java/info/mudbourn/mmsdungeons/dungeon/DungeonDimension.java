package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DungeonDimension {

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MmsDungeons.id("dungeon"));
    public static final int SANCTUARY_BOTTOM = 81;
    public static final int PERICULUM_TOP = -1;

    private DungeonDimension() {
    }

    public static void register() {
        DungeonBlocks.register();
        PericulumRules.register();
        RegionNotices.register();
        DepthGear.register();
        DragonDepth.register();
        MutantBuffs.register();
        DungeonBosses.register();
        ForcedArrivals.register();
        DeepGate.register();
        NearSpawns.register();
        DungeonGeneration.register();
        SanctuaryStash.register();
        DeepEntities.register();
        DeepRebuild.register();
        DddEntranceTunnel.register();
        LabyrinthCorridor.register();
        CavernDisc.register();
        CavernTunnels.register();
        CavernGrid.register();
        LabyrinthCells.register();
        LabyrinthMask.register();
        LabyrinthPlacement.register();
        NetherInfectionCore.register();
        HostOres.register();
    }

    public static boolean isDungeon(Level level) {
        return level.dimension().equals(LEVEL);
    }

    // Whether a position is in the Periculum Region, the dungeon below Y 0.
    public static boolean isPericulum(Level level, BlockPos pos) {
        return isDungeon(level) && pos.getY() <= PERICULUM_TOP;
    }
}
