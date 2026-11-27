package info.mudbourn.mmsdungeons.mobs.luminous;

// Registers the Luminous Monsters content: sounds, effects, mobs, items and the hit effects.
public final class Luminous {

    private Luminous() {
    }

    public static void register() {
        LuminousSounds.register();
        LuminousEffects.register();
        LuminousMobs.register();
        LuminousItems.register();
        LuminousEvents.register();
    }
}
