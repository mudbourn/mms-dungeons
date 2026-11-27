package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A wolf-sized scavenger that hunts zombies and only turns on players who come close.
public class Hyena extends WildWolf {

    private static final double PLAYER_RANGE = 4.0;

    public Hyena(EntityType<? extends Hyena> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityType<? extends BabyMob> babyType() {
        return FangsNClawsMobs.BABY_HYENA;
    }

    @Override
    protected void registerTargets() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            protected double getFollowDistance() {
                return PLAYER_RANGE;
            }
        });
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FangsNClawsSounds.HYENA_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FangsNClawsSounds.HYENA_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FangsNClawsSounds.HYENA_DEATH;
    }
}
