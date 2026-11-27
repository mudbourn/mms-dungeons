package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

// A sand-colored scorpion whose hits leave venom, with a Husk for a rider.
public class DesertScorpion extends Scorpion {

    private static final int VENOM_TICKS = 100;

    public DesertScorpion(EntityType<? extends DesertScorpion> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends Mob> jockeyType() {
        return EntityTypes.HUSK;
    }

    @Override
    protected int babyVariant() {
        return 1;
    }

    @Override
    protected void applyHitEffect(LivingEntity target, boolean stinger) {
        FangsNClawsEffects.venom(target, VENOM_TICKS);
    }
}
