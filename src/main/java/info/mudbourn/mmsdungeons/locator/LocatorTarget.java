package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

// One thing the locator can find: a preset, or any single structure or biome.
public record LocatorTarget(
    Kind kind,
    Identifier id,
    TagKey<Structure> structures,
    ResourceKey<Structure> structure,
    ResourceKey<Biome> biome
) {

    public enum Kind {
        PRESET,
        STRUCTURE,
        BIOME;

        public static Kind byName(String name) {
            for (Kind kind : values()) {
                if (kind.name().equalsIgnoreCase(name)) {
                    return kind;
                }
            }
            return null;
        }
    }

    public static LocatorTarget presetStructure(String name) {
        return new LocatorTarget(
            Kind.PRESET,
            MmsDungeons.id(name),
            TagKey.create(Registries.STRUCTURE, MmsDungeons.id("locator/" + name)),
            null,
            null);
    }

    public static LocatorTarget presetBiome(String name) {
        return new LocatorTarget(
            Kind.PRESET,
            MmsDungeons.id(name),
            null,
            null,
            ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("alexscaves", name)));
    }

    public static LocatorTarget single(Kind kind, Identifier id) {
        return switch (kind) {
            case STRUCTURE -> new LocatorTarget(kind, id, null, ResourceKey.create(Registries.STRUCTURE, id), null);
            case BIOME -> new LocatorTarget(kind, id, null, null, ResourceKey.create(Registries.BIOME, id));
            case PRESET -> Locators.preset(id);
        };
    }

    public boolean isStructure() {
        return structures != null || structure != null;
    }

    public Component label() {
        return switch (kind) {
            case PRESET -> Component.translatable("locator.mms_dungeons.target." + id.getPath());
            case STRUCTURE -> displayName(true, id);
            case BIOME -> displayName(false, id);
        };
    }

    public static Component displayName(boolean structure, Identifier id) {
        String key = (structure ? "structure." : "biome.") + id.getNamespace() + "." + id.getPath().replace('/', '.');
        return Component.translatableWithFallback(key, pretty(id.getPath()));
    }

    private static String pretty(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.startsWith("ddd_")) {
            name = name.substring(4);
        }
        StringBuilder out = new StringBuilder();
        for (String word : name.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
