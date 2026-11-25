package info.mudbourn.mmsdungeons;

import info.mudbourn.mmsdungeons.aerialhell.AerialHellPortals;
import info.mudbourn.mmsdungeons.corpse.Corpses;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.dungeon.DungeonFishing;
import info.mudbourn.mmsdungeons.guild.Guilds;
import info.mudbourn.mmsdungeons.mobs.kobolds.Kobolds;
import info.mudbourn.mmsdungeons.mobs.orpiment.Orpiment;
import info.mudbourn.mmsdungeons.party.Parties;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class MmsDungeons implements ModInitializer {

    public static final String MOD_ID = "mms_dungeons";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        DungeonDimension.register();
        DungeonFishing.register();
        Parties.register();
        Guilds.register();
        Corpses.register();
        AerialHellPortals.register();
        Kobolds.register();
        Orpiment.register();
    }
}
