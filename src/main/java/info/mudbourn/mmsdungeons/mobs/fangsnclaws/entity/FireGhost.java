package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

// A burning ghost whose touch sets its target alight.
public class FireGhost extends Ghost {

    private static final float BURN_SECONDS = 5.0f;

    public FireGhost(EntityType<? extends FireGhost> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
            .add(Attributes.FLYING_SPEED, 0.14)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 1.8);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.igniteForSeconds(BURN_SECONDS);
        }
        return hit;
    }
}
