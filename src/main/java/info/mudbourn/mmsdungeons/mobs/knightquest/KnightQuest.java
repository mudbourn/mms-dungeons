package info.mudbourn.mmsdungeons.mobs.knightquest;

// Registers the Knight Quest content: sounds, blocks, mobs and items.
public final class KnightQuest {

    private KnightQuest() {
    }

    public static void register() {
        KnightQuestSounds.register();
        KnightQuestBlocks.register();
        KnightQuestMobs.register();
        KnightQuestItems.register();
    }
}
