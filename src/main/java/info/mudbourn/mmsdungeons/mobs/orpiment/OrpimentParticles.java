package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class OrpimentParticles {

    public static SimpleParticleType AMBER_MOTE;
    public static SimpleParticleType SPARK;
    public static SimpleParticleType TAR_DRIP;
    public static SimpleParticleType TAR_STRAND;
    public static SimpleParticleType WRAITH_SKULL;
    public static SimpleParticleType YELLOW_GLINT;

    private OrpimentParticles() {
    }

    public static void register() {
        AMBER_MOTE = register("orpiment_amber_mote");
        SPARK = register("orpiment_spark");
        TAR_DRIP = register("orpiment_tar_drip");
        TAR_STRAND = register("orpiment_tar_strand");
        WRAITH_SKULL = register("orpiment_wraith_skull");
        YELLOW_GLINT = register("orpiment_yellow_glint");
    }

    private static SimpleParticleType register(String name) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, MmsDungeons.id(name), FabricParticleTypes.simple());
    }
}
