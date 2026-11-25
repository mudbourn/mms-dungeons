package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.feature.AmberDruseFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class OrpimentFeatures {

    private OrpimentFeatures() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, MmsDungeons.id("orpiment_amber_druse"), AmberDruseFeature.CODEC);
    }
}
