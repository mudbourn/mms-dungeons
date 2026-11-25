package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Picks the weapons kobolds of every kind spawn with. Each family has a tag per material listing the vanilla or
// kobold weapon and its modded variants (longswords, battleaxes, longbows and so on), and a kobold takes one at
// random. A variant mod that is not installed simply drops out of the tag.
public final class KoboldArms {

    public enum Family {
        SWORD("sword"),
        AXE("axe");

        final String name;

        Family(String name) {
            this.name = name;
        }
    }

    // Depth materials, matching the bands in DepthGear.
    public static final String IRON = "iron";
    public static final String DIAMOND = "diamond";
    public static final String NETHERITE = "netherite";

    private static final TagKey<Item> SWORDS = tag("sword");
    private static final TagKey<Item> AXES = tag("axe");
    private static final TagKey<Item> CROSSBOWS = tag("crossbows");
    private static final TagKey<Item> RANGED = tag("ranged");

    private KoboldArms() {
    }

    public static ItemStack melee(Family family, String material, RandomSource random) {
        Item fallback = switch (family) {
            case SWORD -> IRON.equals(material) ? KoboldItems.KOBOLD_IRON_SWORD : DIAMOND.equals(material) ? Items.DIAMOND_SWORD : Items.NETHERITE_SWORD;
            case AXE -> IRON.equals(material) ? KoboldItems.KOBOLD_IRON_AXE : DIAMOND.equals(material) ? Items.DIAMOND_AXE : Items.NETHERITE_AXE;
        };
        return pick(tag(family.name + "_" + material), fallback, random);
    }

    // Any crossbow or bow a kobold can shoot, or crossbows only for kobolds whose AI only knows crossbows.
    public static ItemStack ranged(boolean crossbowsOnly, RandomSource random) {
        return pick(crossbowsOnly ? CROSSBOWS : RANGED, Items.CROSSBOW, random);
    }

    // The melee family a held weapon belongs to, or null for ranged weapons and anything else.
    public static Family familyOf(ItemStack stack) {
        if (stack.is(SWORDS)) {
            return Family.SWORD;
        }
        return stack.is(AXES) ? Family.AXE : null;
    }

    private static ItemStack pick(TagKey<Item> tag, Item fallback, RandomSource random) {
        List<Item> options = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            options.add(holder.value());
        }
        return new ItemStack(options.isEmpty() ? fallback : options.get(random.nextInt(options.size())));
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, MmsDungeons.id("kobold_arms/" + name));
    }
}
