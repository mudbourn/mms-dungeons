package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKoboldZombie;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.ZomboType;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

// Picks the skin and glowing eyes each kind of kobold wears, including a few named easter eggs.
final class KoboldLooks {

    private static final String POPPER = "Popper";

    private KoboldLooks() {
    }

    static BiConsumer<AbstractKobold, KoboldRenderState> kobold(String skin) {
        return kobold(skin, null, Set.of());
    }

    // A kobold whose eyes may turn diamond or, for one name, popper; named kobolds can wear a special skin.
    static BiConsumer<AbstractKobold, KoboldRenderState> kobold(String skin, String namedSkin, Set<String> names) {
        return (kobold, state) -> {
            boolean popper = named(kobold, POPPER);
            boolean special = namedSkin != null && names.stream().anyMatch(name -> named(kobold, name));
            String chosen = popper ? "special/popper" : special ? namedSkin : skin;
            state.texture = texture(chosen);
            state.eyes = texture(kobold.isDiamond() ? "eyes/diamond" : popper ? "eyes/popper" : "eyes/base");
        };
    }

    static BiConsumer<AbstractKobold, KoboldRenderState> pirate(String skin) {
        return (kobold, state) -> {
            state.texture = texture(skin);
            state.eyes = texture(kobold.isDiamond() ? "eyes/diamond_pirate" : "eyes/base_pirate");
        };
    }

    static BiConsumer<AbstractKobold, KoboldRenderState> child() {
        return (kobold, state) -> {
            state.texture = texture("kobolds/child");
            state.eyes = texture("eyes/base");
        };
    }

    static BiConsumer<AbstractKoboldZombie, KoboldRenderState> zombold() {
        return (zombold, state) -> {
            ZomboType type = zombold.getZomboType();
            boolean james = type == ZomboType.ENCHANTER && named(zombold, "James");
            state.texture = texture(james ? "undead/zombie_james" : "undead/zombie_" + type.id());
            state.eyes = texture(type.isPirate() ? "eyes/zombie_pirate" : "eyes/zombie");
            state.isConverting = zombold.isConverting();
        };
    }

    static <T extends Mob> BiConsumer<T, KoboldRenderState> skeleton(String skin, String eyes) {
        return (skeleton, state) -> {
            state.texture = texture(skin);
            state.eyes = eyes == null ? null : texture(eyes);
        };
    }

    private static boolean named(Entity entity, String name) {
        Component custom = entity.getCustomName();
        return custom != null && name.equals(custom.getString());
    }

    private static Identifier texture(String path) {
        return MmsDungeons.id("textures/entity/" + path + ".png");
    }
}
