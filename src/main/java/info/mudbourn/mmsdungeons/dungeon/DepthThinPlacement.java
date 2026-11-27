package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

// In the dungeon, keeps a position more often the lower it is, from 45 percent at the roof to 100 percent at the floor.
public final class DepthThinPlacement implements PlacementFilter {

    public static final MapCodec<DepthThinPlacement> CODEC = MapCodec.unit(new DepthThinPlacement());
    private static final int FLOOR = -128;
    private static final int SPAN = 288;
    private static final float ROOF_KEEP = 0.45f;

    public static void register() {
        Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, MmsDungeons.id("depth_thin"), CODEC);
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!context.getLevel().getLevel().dimension().equals(DungeonDimension.LEVEL)) {
            return true;
        }
        float height = Math.clamp((pos.getY() - FLOOR) / (float) SPAN, 0.0f, 1.0f);
        return random.nextFloat() < 1.0f - (1.0f - ROOF_KEEP) * height;
    }

    @Override
    public MapCodec<DepthThinPlacement> codec() {
        return CODEC;
    }
}
