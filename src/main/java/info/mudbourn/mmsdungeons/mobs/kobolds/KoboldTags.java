package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BannerPattern;

public final class KoboldTags {

    public static final TagKey<EntityType<?>> TRADERS = entity("kobold_traders");
    public static final TagKey<BannerPattern> BANNER_PATTERN = TagKey.create(
        Registries.BANNER_PATTERN,
        MmsDungeons.id("pattern_for_kobold")
    );
    public static final TagKey<Enchantment> PREFERRED_ENCHANTMENTS = TagKey.create(
        Registries.ENCHANTMENT,
        MmsDungeons.id("kobold_preferred")
    );
    public static final TagKey<Item> ARMOR = item("kobold_armor");
    public static final TagKey<Item> WEAPONS = item("kobold_weapons");
    public static final TagKey<Item> WARRIOR_WEAPONS = item("kobold_warrior_weapons");
    public static final TagKey<Item> ENGINEER_WEAPONS = item("kobold_engineer_weapons");

    private KoboldTags() {
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, MmsDungeons.id(name));
    }

    private static TagKey<EntityType<?>> entity(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, MmsDungeons.id(name));
    }
}
