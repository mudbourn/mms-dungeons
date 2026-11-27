package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

// Registers the Fangs 'n Claws content.
public final class FangsNClaws {

    private FangsNClaws() {
    }

    public static void register() {
        FangsNClawsSounds.register();
        FangsNClawsEffects.register();
        FangsNClawsMobs.register();
        FangsNClawsBlocks.register();
        FangsNClawsItems.register();
        FangsNClawsArmor.register();
        FangsNClawsSpawns.register();
        MimicChests.register();
        TotemOfFrost.register();
    }
}
