package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

// A horse raised from the dead.
public abstract class UndeadHorse extends HorseMob {

    protected UndeadHorse(EntityType<? extends UndeadHorse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 15.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 2.6);
    }
}
