package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.BlowgunItem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.CatchingClawItem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.DaggerItem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.PitchforkItem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.SilverSwordItem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.item.TomahawkItem;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.DispenserBlock;

// The Fangs 'n Claws armor sets and weapons.
public final class FangsNClawsGear {

    public record ArmorSet(Item helmet, Item chestplate, Item leggings, Item boots) {

        public int count(LivingEntity wearer) {
            int count = 0;
            for (ArmorType type : ArmorType.values()) {
                Item piece = this.piece(type);
                if (piece != null && wearer.getItemBySlot(type.getSlot()).is(piece)) {
                    count++;
                }
            }
            return count;
        }

        public boolean wears(LivingEntity wearer, ArmorType type) {
            Item piece = this.piece(type);
            return piece != null && wearer.getItemBySlot(type.getSlot()).is(piece);
        }

        private Item piece(ArmorType type) {
            return switch (type) {
                case HELMET -> this.helmet;
                case CHESTPLATE -> this.chestplate;
                case LEGGINGS -> this.leggings;
                case BOOTS -> this.boots;
                default -> null;
            };
        }
    }

    public static ArmorSet FUR;
    public static ArmorSet OWL;
    public static ArmorSet SCORPION;
    public static ArmorSet SHRIKE;
    public static Item FANG_DAGGER;
    public static Item NETHERITE_DAGGER;
    public static Item SILVER_SWORD;
    public static Item TOMAHAWK;
    public static Item FIRE_PITCHFORK;
    public static Item HELLFIRE_PITCHFORK;
    public static Item BLOWGUN;
    public static Item CATCHING_CLAW;
    public static Item CATCHING_CLAW_NETHERITE;

    private static final TagKey<Item> REPAIRS_FUR = repairs("repairs_fur_armor");
    private static final TagKey<Item> REPAIRS_FEATHER = repairs("repairs_feather_armor");
    private static final TagKey<Item> REPAIRS_SCORPION = repairs("repairs_scorpion_armor");

    private static final float OWL_FALL_REDUCTION = 0.12f;
    private static final float SHRIKE_STAT_BONUS = 0.05f;

    private FangsNClawsGear() {
    }

    public static void register() {
        FUR = armorSet(
            "fur",
            material("fur", 15, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0f, REPAIRS_FUR),
            new int[] {165, 240, 225, 195},
            false,
            false
        );
        OWL = armorSet(
            "owl",
            material("owl", 15, 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0f, REPAIRS_FEATHER),
            new int[] {165, 240, 225, 195},
            true,
            false
        );
        SCORPION = armorSet(
            "scorpion",
            material("scorpion", 14, 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.5f, REPAIRS_SCORPION),
            new int[] {150, 220, 200, 175},
            false,
            false
        );
        SHRIKE = armorSet(
            "shrike",
            material("shrike", 33, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0f, REPAIRS_FEATHER),
            new int[] {363, 528, 495, 429},
            false,
            true
        );
        registerWeapons();
    }

    private static void registerWeapons() {
        FANG_DAGGER = FangsNClawsItems.register(
            "fang_dagger",
            FangsNClawsItems.properties("fang_dagger").sword(ToolMaterial.STONE, 1.0f, -1.1f).enchantable(14),
            properties -> new DaggerItem(properties, 0)
        );
        NETHERITE_DAGGER = FangsNClawsItems.register(
            "netherite_dagger",
            FangsNClawsItems.properties("netherite_dagger")
                .sword(ToolMaterial.NETHERITE, -1.0f, -1.2f)
                .fireResistant(),
            properties -> new DaggerItem(properties, 1)
        );
        SILVER_SWORD = FangsNClawsItems.register(
            "silver_sword",
            FangsNClawsItems.properties("silver_sword").sword(ToolMaterial.IRON, 1.0f, -2.4f),
            SilverSwordItem::new
        );
        TOMAHAWK = FangsNClawsItems.register(
            "tomahawk",
            FangsNClawsItems.properties("tomahawk").axe(ToolMaterial.IRON, 5.0f, -3.1f),
            TomahawkItem::new
        );
        FIRE_PITCHFORK = FangsNClawsItems.register(
            "fire_pitchfork",
            pitchfork("fire_pitchfork", 128, 4.0),
            properties -> new PitchforkItem(properties, false)
        );
        HELLFIRE_PITCHFORK = FangsNClawsItems.register(
            "hellfire_pitchfork",
            pitchfork("hellfire_pitchfork", 256, 7.0),
            properties -> new PitchforkItem(properties, true)
        );
        BLOWGUN = FangsNClawsItems.register(
            "blowgun",
            FangsNClawsItems.properties("blowgun").durability(64).enchantable(1),
            BlowgunItem::new
        );
        CATCHING_CLAW = FangsNClawsItems.register(
            "catching_claw",
            claw("catching_claw", 128).enchantable(14),
            properties -> new CatchingClawItem(properties, false)
        );
        CATCHING_CLAW_NETHERITE = FangsNClawsItems.register(
            "catching_claw_netherite",
            claw("catching_claw_netherite", 256).fireResistant().enchantable(15),
            properties -> new CatchingClawItem(properties, true)
        );
        DispenserBlock.registerProjectileBehavior(TOMAHAWK);
        DispenserBlock.registerProjectileBehavior(FIRE_PITCHFORK);
        DispenserBlock.registerProjectileBehavior(HELLFIRE_PITCHFORK);
    }

    private static Item.Properties pitchfork(String name, int durability, double damage) {
        return FangsNClawsItems.properties(name)
            .durability(durability)
            .attributes(attackModifiers(damage, -2.9))
            .component(DataComponents.TOOL, TridentItem.createToolProperties())
            .enchantable(14)
            .component(DataComponents.WEAPON, new Weapon(1));
    }

    private static Item.Properties claw(String name, int durability) {
        return FangsNClawsItems.properties(name)
            .durability(durability)
            .attributes(
                ItemAttributeModifiers.builder()
                    .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                    )
                    .build()
            );
    }

    private static ItemAttributeModifiers attackModifiers(double damage, double speed) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    private static TagKey<Item> repairs(String name) {
        return TagKey.create(Registries.ITEM, MmsDungeons.id(name));
    }

    private static ArmorMaterial material(
        String name,
        int durability,
        int enchantability,
        Holder<SoundEvent> sound,
        float toughness,
        TagKey<Item> repair
    ) {
        return new ArmorMaterial(
            durability,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2),
            enchantability,
            sound,
            toughness,
            0.0f,
            repair,
            ResourceKey.create(EquipmentAssets.ROOT_ID, MmsDungeons.id(name))
        );
    }

    private static ArmorSet armorSet(
        String name,
        ArmorMaterial material,
        int[] durabilities,
        boolean feathered,
        boolean shrike
    ) {
        return new ArmorSet(
            piece(name, material, ArmorType.HELMET, durabilities[0], feathered, shrike),
            piece(name, material, ArmorType.CHESTPLATE, durabilities[1], feathered, shrike),
            piece(name, material, ArmorType.LEGGINGS, durabilities[2], feathered, shrike),
            piece(name, material, ArmorType.BOOTS, durabilities[3], feathered, shrike)
        );
    }

    private static Item piece(
        String set,
        ArmorMaterial material,
        ArmorType type,
        int durability,
        boolean feathered,
        boolean shrike
    ) {
        String name = set + "_" + type.getName();
        Item.Properties properties = FangsNClawsItems.properties(name)
            .humanoidArmor(material, type)
            .durability(durability);
        if (feathered || shrike) {
            properties.attributes(extendedModifiers(material, type, feathered));
        }
        if (type == ArmorType.CHESTPLATE && (feathered || shrike)) {
            properties.component(DataComponents.GLIDER, Unit.INSTANCE);
        }
        return FangsNClawsItems.register(name, properties, Item::new);
    }

    private static ItemAttributeModifiers extendedModifiers(ArmorMaterial material, ArmorType type, boolean feathered) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        material.createAttributes(type).modifiers().forEach(
            entry -> builder.add(entry.attribute(), entry.modifier(), entry.slot())
        );
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        String suffix = type.getName();
        if (feathered) {
            builder.add(
                Attributes.FALL_DAMAGE_MULTIPLIER,
                new AttributeModifier(
                    MmsDungeons.id("owl_feather_fall_" + suffix),
                    -OWL_FALL_REDUCTION,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                slot
            );
            return builder.build();
        }
        builder.add(
            Attributes.JUMP_STRENGTH,
            new AttributeModifier(
                MmsDungeons.id("shrike_jump_" + suffix),
                SHRIKE_STAT_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ),
            slot
        );
        builder.add(
            Attributes.SAFE_FALL_DISTANCE,
            new AttributeModifier(
                MmsDungeons.id("shrike_safe_fall_" + suffix),
                SHRIKE_STAT_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ),
            slot
        );
        builder.add(
            Attributes.FALL_DAMAGE_MULTIPLIER,
            new AttributeModifier(
                MmsDungeons.id("shrike_fall_damage_" + suffix),
                -SHRIKE_STAT_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ),
            slot
        );
        return builder.build();
    }
}
