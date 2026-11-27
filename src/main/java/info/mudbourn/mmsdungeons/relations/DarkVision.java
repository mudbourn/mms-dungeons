package info.mudbourn.mmsdungeons.relations;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

// How much of its normal sight range a mob keeps against a target standing in the dark.
public final class DarkVision {

    private static final int FULL_SIGHT_LIGHT = 8;
    private static final double DARK_SIGHT = 0.35;

    private DarkVision() {
    }

    // One for a lit target or one carrying a light, down to DARK_SIGHT in full darkness, in the dungeon only.
    public static double factor(LivingEntity target) {
        if (!DungeonDimension.isDungeon(target.level())) {
            return 1.0;
        }
        int light = target.level().getMaxLocalRawBrightness(BlockPos.containing(target.getEyePosition()));
        if (light >= FULL_SIGHT_LIGHT || LightSources.carries(target)) {
            return 1.0;
        }
        return DARK_SIGHT + (1.0 - DARK_SIGHT) * light / FULL_SIGHT_LIGHT;
    }
}
