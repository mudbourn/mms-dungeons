package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

// A fire-proof scorpion whose hits set their target alight, with a Wither Skeleton for a rider.
public class NetherScorpion extends Scorpion {

    private static final float BURN_SECONDS = 5.0f;

    public NetherScorpion(EntityType<? extends NetherScorpion> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends Mob> jockeyType() {
        return EntityTypes.WITHER_SKELETON;
    }

    @Override
    protected int babyVariant() {
        return 3;
    }

    @Override
    protected void applyHitEffect(LivingEntity target, boolean stinger) {
        target.igniteForSeconds(BURN_SECONDS);
    }
}
