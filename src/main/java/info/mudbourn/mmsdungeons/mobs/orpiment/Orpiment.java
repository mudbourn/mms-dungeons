package info.mudbourn.mmsdungeons.mobs.orpiment;

// Registers the Orpiment content: sounds, effects, fluids, blocks, mobs, items and the worldgen features that tie them together.
public final class Orpiment {

    private Orpiment() {
    }

    public static void register() {
        OrpimentSounds.register();
        OrpimentEffects.register();
        OrpimentParticles.register();
        OrpimentFluids.register();
        OrpimentBlocks.register();
        OrpimentMobs.register();
        OrpimentItems.register();
        OrpimentFeatures.register();
    }
}
