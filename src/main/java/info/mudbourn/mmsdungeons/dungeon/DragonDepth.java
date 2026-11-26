package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Makes wild Useless Reptile dragons stronger the deeper in the dungeon they first load.
public final class DragonDepth {

    private static final String NAMESPACE = "uselessreptile";
    private static final String SCALED_TAG = "mms_dungeons.dragon_scaled";
    private static final double DEEP_BONUS = 0.2;
    private static final double PERICULUM_BONUS = 0.4;
    // Share of the bonus applied to armour.
    private static final double ARMOR_SHARE = 0.25;
    private static final Identifier ID = MmsDungeons.id("dragon_depth");

    private DragonDepth() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DragonDepth::onLoad);
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof Mob mob)
            || !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(NAMESPACE)
            || !DungeonDimension.isDungeon(level)
            || entity instanceof OwnableEntity owned && owned.getOwnerReference() != null
            || !entity.addTag(SCALED_TAG)) {
            return;
        }
        int y = mob.getBlockY();
        double bonus = y <= DungeonDimension.PERICULUM_TOP ? PERICULUM_BONUS : y < DungeonDimension.SANCTUARY_BOTTOM ? DEEP_BONUS : 0.0;
        if (bonus == 0.0) {
            return;
        }
        scale(mob, Attributes.MAX_HEALTH, bonus);
        scale(mob, Attributes.ATTACK_DAMAGE, bonus);
        scale(mob, Attributes.ARMOR, bonus * ARMOR_SHARE);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void scale(Mob mob, Holder<Attribute> attribute, double amount) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getBaseValue() <= 0.0 || instance.hasModifier(ID)) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
