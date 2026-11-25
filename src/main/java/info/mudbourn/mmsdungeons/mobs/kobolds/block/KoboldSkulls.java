package info.mudbourn.mmsdungeons.mobs.kobolds.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Shared behavior of the standing and wall skulls: under a night sky they turn into their undead kobold.
final class KoboldSkulls {

    private KoboldSkulls() {
    }

    static void summonIfPowered(Level level, BlockPos pos, EntityType<?> summoned) {
        if (level instanceof ServerLevel serverLevel && level.getBestNeighborSignal(pos) > 0 && underNightSky(serverLevel, pos)) {
            summon(serverLevel, pos, summoned);
        }
    }

    static void summonIfOpen(ServerLevel level, BlockPos pos, EntityType<?> summoned) {
        if (underNightSky(level, pos)) {
            summon(level, pos, summoned);
        }
    }

    private static boolean underNightSky(ServerLevel level, BlockPos pos) {
        return level.isDarkOutside() && level.canSeeSkyFromBelowWater(pos);
    }

    private static void summon(ServerLevel level, BlockPos pos, EntityType<?> summoned) {
        level.destroyBlock(pos, false);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (bolt != null) {
            bolt.snapTo(Vec3.atBottomCenterOf(pos));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        summoned.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
    }
}
