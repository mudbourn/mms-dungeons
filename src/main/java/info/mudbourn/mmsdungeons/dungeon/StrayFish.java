package info.mudbourn.mmsdungeons.dungeon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

// Keeps every Fish of Thieves creature out of the dungeon, in water or not.
public final class StrayFish {

    private static final String FISH_OF_THIEVES = "fishofthieves";

    private StrayFish() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(StrayFish::onLoad);
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (DungeonDimension.isDungeon(level) && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(FISH_OF_THIEVES)) {
            entity.discard();
        }
    }
}
