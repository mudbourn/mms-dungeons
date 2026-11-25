package info.mudbourn.mmsdungeons.mobs.kobolds;

// Registers the Kobolds content: mobs, items, skulls, sounds, potions and the events that tie them together.
public final class Kobolds {

    private Kobolds() {
    }

    public static void register() {
        KoboldSounds.register();
        KoboldPotions.register();
        KoboldBlocks.register();
        KoboldMobs.register();
        KoboldItems.register();
        KoboldEvents.register();
        KoboldWork.register();
    }
}
