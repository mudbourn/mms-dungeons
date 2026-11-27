package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

// Arms and armors a faction member for its faction, class and tier.
public final class FactionGear {

    public static final int LUNAR_TIER = 4;

    private static final TagKey<Item> GATE_BOWS = TagKey.create(Registries.ITEM, MmsDungeons.id("gate_bows"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_NAMES = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[] LUNAR_ARMOR = {"lunatic_helmet", "lunatic_chestplate", "lunatic_leggings", "lunatic_boots"};
    private static final String[] LUNAR_WEAPONS = {"lunatic_sword", "lunatic_axe", "lunatic_spear"};
    private static final String[] SCHOOLS = {"arcane", "fire", "frost"};
    private static final float DUAL_WIELD_CHANCE = 0.25f;
    private static final float HEAVY_FRONTLINE_CHANCE = 0.3f;
    private static final float HEAVY_BACKLINE_CHANCE = 0.08f;
    private static final float HEAVY_TIER_BONUS = 0.05f;
    private static final int ENCHANT_LEVEL_MIN = 15;
    private static final int ENCHANT_LEVEL_MAX = 30;

    private FactionGear() {
    }

    // Tier 1 to 3 weighted towards the low end.
    public static int rollTier(RandomSource random) {
        int roll = random.nextInt(20);
        return roll < 10 ? 1 : roll < 17 ? 2 : 3;
    }

    public static void equip(FactionMember mob, RandomSource random) {
        RegistryAccess access = mob.level().registryAccess();
        Faction faction = mob.faction();
        int tier = mob.tier();
        boolean lunarWeapon = tier == LUNAR_TIER && mob.factionClass().melee() && random.nextBoolean();
        equipArmor(mob, faction, tier, lunarWeapon, random, access);
        equipHands(mob, faction, tier, lunarWeapon, random, access);
    }

    private static void equipArmor(FactionMember mob, Faction faction, int tier, boolean lunarWeapon, RandomSource random, RegistryAccess access) {
        boolean[] lunar = new boolean[ARMOR.length];
        if (tier == LUNAR_TIER && !lunarWeapon) {
            int pieces = 1 + random.nextInt(2);
            for (int placed = 0; placed < pieces; ) {
                int slot = random.nextInt(ARMOR.length);
                if (!lunar[slot]) {
                    lunar[slot] = true;
                    placed++;
                }
            }
        }
        boolean[] upper = upperPieces(faction, tier, random);
        boolean heavy = rollHeavy(mob, tier, random);
        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack stack;
            if (lunar[i]) {
                stack = stack("aerialhell:" + LUNAR_ARMOR[i], "minecraft:netherite_" + ARMOR_NAMES[i]);
            } else if (faction == Faction.BANDIT && tier == 1 && !upper[i]) {
                continue;
            } else {
                String material = upper[i] ? upperMaterial(faction) : lowerMaterial(faction);
                String plain = "minecraft:" + material + "_" + ARMOR_NAMES[i];
                stack = heavy ? stack("expanded_weaponry:heavy_" + material + "_" + ARMOR_NAMES[i], plain) : stack(plain, "minecraft:iron_" + ARMOR_NAMES[i]);
                if (enchanted(faction, tier, upper[i])) {
                    stack = enchant(stack, random, access);
                }
            }
            mob.setItemSlot(ARMOR[i], stack);
        }
    }

    // A roll for Expanded Weaponry heavy armor in place of the plain set: likelier for knights and paladins and at higher tiers, never for fliers.
    private static boolean rollHeavy(FactionMember mob, int tier, RandomSource random) {
        if (mob.winged()) {
            return false;
        }
        float chance = mob.factionClass().melee() ? HEAVY_FRONTLINE_CHANCE : HEAVY_BACKLINE_CHANCE;
        return random.nextFloat() < chance + HEAVY_TIER_BONUS * (tier - 1);
    }

    // Which slots carry the faction's best material; the rest take the material below it.
    private static boolean[] upperPieces(Faction faction, int tier, RandomSource random) {
        boolean[] upper = new boolean[ARMOR.length];
        if (tier >= 2) {
            Arrays.fill(upper, true);
            return upper;
        }
        int pieces = faction == Faction.BANDIT ? 2 : 1 + random.nextInt(3);
        for (int placed = 0; placed < pieces; ) {
            int slot = random.nextInt(ARMOR.length);
            if (!upper[slot]) {
                upper[slot] = true;
                placed++;
            }
        }
        return upper;
    }

    private static String upperMaterial(Faction faction) {
        return switch (faction) {
            case BANDIT -> "iron";
            case MERCENARY -> "diamond";
            case GUILDSMAN -> "netherite";
        };
    }

    private static String lowerMaterial(Faction faction) {
        return switch (faction) {
            case BANDIT, MERCENARY -> "iron";
            case GUILDSMAN -> "diamond";
        };
    }

    // Tier 3 enchants everything, tier 1 enchants the lower pieces above bandits, and the lunar tier enchants its netherite.
    private static boolean enchanted(Faction faction, int tier, boolean upper) {
        if (tier >= 3) {
            return true;
        }
        return tier == 1 && faction != Faction.BANDIT && !upper;
    }

    private static void equipHands(FactionMember mob, Faction faction, int tier, boolean lunarWeapon, RandomSource random, RegistryAccess access) {
        boolean enchant = tier >= 3;
        FactionClass type = mob.factionClass();
        ItemStack main;
        ItemStack off = ItemStack.EMPTY;
        switch (type) {
            case KNIGHT, PALADIN -> {
                main = lunarWeapon ? lunarWeapon(random) : meleeWeapon(type, faction, random);
                if (random.nextFloat() < DUAL_WIELD_CHANCE) {
                    off = meleeWeapon(type, faction, random);
                } else {
                    off = shield(type, faction);
                }
            }
            case ARCHER -> main = bow(random);
            case MAGE -> main = mageWeapon(faction, random);
            case PRIEST, CLERIC -> main = priestWeapon(faction);
            default -> main = new ItemStack(Items.IRON_SWORD);
        }
        if (enchant && !lunarWeapon) {
            main = enchant(main, random, access);
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, main);
        mob.setItemSlot(EquipmentSlot.OFFHAND, off);
        if (type != FactionClass.ARCHER && type != FactionClass.MAGE) {
            mob.setSidearm(sidearm(faction, random));
        }
    }

    private static ItemStack meleeWeapon(FactionClass type, Faction faction, RandomSource random) {
        String material = upperMaterial(faction);
        if (type == FactionClass.PALADIN) {
            String[] kinds = {"claymore", "great_hammer", "mace"};
            return stack("paladins:" + material + "_" + kinds[random.nextInt(kinds.length)], "minecraft:" + material + "_axe");
        }
        String kind = random.nextInt(3) == 0 ? "axe" : "sword";
        return stack("minecraft:" + material + "_" + kind, "minecraft:iron_sword");
    }

    private static ItemStack lunarWeapon(RandomSource random) {
        return stack("aerialhell:" + LUNAR_WEAPONS[random.nextInt(LUNAR_WEAPONS.length)], "minecraft:netherite_sword");
    }

    private static ItemStack shield(FactionClass type, Faction faction) {
        if (type == FactionClass.PALADIN) {
            return stack("paladins:" + upperMaterial(faction) + "_kite_shield", "minecraft:shield");
        }
        return new ItemStack(Items.SHIELD);
    }

    private static ItemStack bow(RandomSource random) {
        List<Item> bows = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(GATE_BOWS).forEach(holder -> bows.add(holder.value()));
        if (bows.isEmpty()) {
            return new ItemStack(Items.BOW);
        }
        return new ItemStack(bows.get(random.nextInt(bows.size())));
    }

    // The ranged weapon a fighter or healer keeps put away: bandits a bow, guildsmen a crossbow, mercenaries either.
    private static ItemStack sidearm(Faction faction, RandomSource random) {
        boolean crossbow = switch (faction) {
            case BANDIT -> false;
            case MERCENARY -> random.nextBoolean();
            case GUILDSMAN -> true;
        };
        return new ItemStack(crossbow ? Items.CROSSBOW : Items.BOW);
    }

    private static ItemStack mageWeapon(Faction faction, RandomSource random) {
        String school = SCHOOLS[random.nextInt(SCHOOLS.length)];
        String id = switch (faction) {
            case BANDIT -> "wizards:wand_" + school;
            case MERCENARY -> "wizards:staff_" + school;
            case GUILDSMAN -> "wizards:staff_netherite_" + school;
        };
        return stack(id, "wizards:wand_novice");
    }

    private static ItemStack priestWeapon(Faction faction) {
        String id = switch (faction) {
            case BANDIT -> "paladins:holy_wand";
            case MERCENARY -> "paladins:diamond_holy_wand";
            case GUILDSMAN -> "paladins:netherite_holy_staff";
        };
        return stack(id, "paladins:holy_wand");
    }

    // The item by id, else the fallback, else a stick so a missing mod never leaves the hand empty by accident.
    private static ItemStack stack(String id, String fallback) {
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(Identifier.parse(id));
        if (item.isEmpty()) {
            item = BuiltInRegistries.ITEM.get(Identifier.parse(fallback));
        }
        return item.map(holder -> new ItemStack(holder.value())).orElseGet(() -> new ItemStack(Items.STICK));
    }

    private static ItemStack enchant(ItemStack stack, RandomSource random, RegistryAccess access) {
        int level = ENCHANT_LEVEL_MIN + random.nextInt(ENCHANT_LEVEL_MAX - ENCHANT_LEVEL_MIN + 1);
        return EnchantmentHelper.enchantItem(random, stack, level, access, Optional.empty());
    }
}
