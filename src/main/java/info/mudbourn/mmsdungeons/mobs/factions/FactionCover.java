package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

// Picks the cosmetic armor a faction member wears over its real gear: one set, optionally mixed with mms-vanity's invisible kit.
public final class FactionCover {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] LEATHER = {"leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots"};
    private static final int INVISIBLE_DYE = 0xFFFFFF;
    private static final String[] FANTASY_PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private static final String[] LOWLANDS = {
        "axolotl_knight",
        "bret_corsair_o__armor_v2",
        "bret_fight_armor",
        "executioner_armor",
        "furnace_master_armor",
        "gamemaster_outfit",
        "gatekeeperarmorb",
        "guard_captain_uniform_r",
        "highlands_suitb",
        "maskerade_armor",
        "mercenary_swordman",
        "mountainmen__clothes",
        "netherborn_pirate",
        "norse_ravager_armor",
        "penitant",
        "plaguedoctor",
        "ratcatcherc01",
        "siege_armor",
        "snowtigerarmor",
        "swampfolk_outfit",
        "waldknightarmor",
        "wingedcavaleryarmor"
    };
    private static final String[] FANTASY = {
        "chess_board_knight",
        "crucible_knight",
        "dark_cover",
        "dark_lord",
        "dawncrest",
        "dead_gladiator",
        "dragonslayer",
        "eclipse_soldier",
        "evening_ghost",
        "fog_guard",
        "forgotten_trace",
        "gilded_hunt",
        "golden_execution",
        "golden_horns",
        "grave_sentinel",
        "old_knight",
        "ronin",
        "silver_knight",
        "thief",
        "twinned",
        "wandering_wizard"
    };

    private FactionCover() {
    }

    public static void apply(FactionMember mob, RandomSource random) {
        switch (mob.faction()) {
            case GUILDSMAN -> coverGuildsman(mob, random);
            case MERCENARY -> {
                if (mob.isLeader() || random.nextFloat() < FeaturesConfig.get().mercenaryCoverChance) {
                    coverLegs(mob, random);
                }
            }
            case BANDIT -> {
            }
        }
    }

    // Dark guildsmen: a full set, the invisible kit alone, or the kit on the head or the head and chest.
    private static void coverGuildsman(FactionMember mob, RandomSource random) {
        int invisibleSlots = switch (random.nextInt(4)) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            default -> 4;
        };
        boolean lowlands = random.nextBoolean();
        String set = pickSet(lowlands, random);
        for (int i = 0; i < ARMOR.length; i++) {
            mob.setCover(ARMOR[i], i < invisibleSlots ? invisiblePiece(i) : piece(lowlands, set, i));
        }
    }

    // Mercenaries: a set's leggings and boots only.
    private static void coverLegs(FactionMember mob, RandomSource random) {
        boolean lowlands = random.nextBoolean();
        String set = pickSet(lowlands, random);
        mob.setCover(EquipmentSlot.LEGS, piece(lowlands, set, 2));
        mob.setCover(EquipmentSlot.FEET, piece(lowlands, set, 3));
    }

    private static String pickSet(boolean lowlands, RandomSource random) {
        String[] sets = lowlands ? LOWLANDS : FANTASY;
        return sets[random.nextInt(sets.length)];
    }

    // A Lowlands piece is a leather base wearing the set's equipment asset; a Fantasy Armor piece is the real item.
    private static ItemStack piece(boolean lowlands, String set, int index) {
        if (!lowlands) {
            Identifier id = Identifier.fromNamespaceAndPath("fantasy_armor", set + "_" + FANTASY_PIECES[index]);
            return BuiltInRegistries.ITEM.get(id)
                .map(holder -> new ItemStack(holder.value()))
                .orElseGet(() -> invisiblePiece(index));
        }
        return leatherWearing(index, Identifier.fromNamespaceAndPath("lowlands_clothing", set));
    }

    // Swaps the barrier that older saves used to mark a hidden slot for the invisible kit piece.
    public static ItemStack upgrade(EquipmentSlot slot, ItemStack cover) {
        if (!cover.is(Items.BARRIER)) {
            return cover;
        }
        for (int i = 0; i < ARMOR.length; i++) {
            if (ARMOR[i] == slot) {
                return invisiblePiece(i);
            }
        }
        return ItemStack.EMPTY;
    }

    // A piece of mms-vanity's invisible kit: leather pointed at the structure void asset, which has no layers and draws nothing.
    private static ItemStack invisiblePiece(int index) {
        ItemStack stack = leatherWearing(index, Identifier.withDefaultNamespace("structure_void"));
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(INVISIBLE_DYE));
        return stack;
    }

    private static ItemStack leatherWearing(int index, Identifier asset) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(LEATHER[index])));
        stack.set(DataComponents.EQUIPPABLE, Equippable.builder(ARMOR[index]).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, asset)).build());
        stack.remove(DataComponents.DYED_COLOR);
        return stack;
    }
}
