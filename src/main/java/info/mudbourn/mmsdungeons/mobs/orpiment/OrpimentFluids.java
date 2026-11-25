package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.TarFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;

public final class OrpimentFluids {

    public static FlowingFluid TAR;
    public static FlowingFluid FLOWING_TAR;

    private OrpimentFluids() {
    }

    public static void register() {
        TAR = Registry.register(BuiltInRegistries.FLUID, MmsDungeons.id("tar"), new TarFluid.Source());
        FLOWING_TAR = Registry.register(BuiltInRegistries.FLUID, MmsDungeons.id("flowing_tar"), new TarFluid.Flowing());
    }
}
