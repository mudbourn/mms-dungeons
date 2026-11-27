package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.HumanFactions;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

// A cleric's buff potion. It breaks like a splash potion but only its thrower's allies take the effect.
public class ThrownBuff extends ThrowableItemProjectile {

    private static final double SPLASH_RADIUS = 4.0;
    private static final int HEAL_PER_LEVEL = 4;

    private @Nullable Holder<MobEffect> effect;
    private int duration;
    private int amplifier;
    private boolean lingering;

    public ThrownBuff(EntityType<? extends ThrownBuff> type, Level level) {
        super(type, level);
    }

    public ThrownBuff(Level level, FactionMember owner, ItemStack potion, Holder<MobEffect> effect, int duration, int amplifier, boolean lingering) {
        super(HumanFactions.THROWN_BUFF, owner, level, potion);
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
        this.lingering = lingering;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.effect != null && this.getOwner() instanceof FactionMember owner) {
            this.bless(level, owner, this.lingering ? 1 : 0);
            level.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, this.blockPosition(), this.effect.value().getColor());
            if (this.lingering) {
                this.leaveCloud(level, owner);
            }
        }
        this.discard();
    }

    // Applies the effect to every ally of the thrower in the splash, and to nobody else.
    private void bless(ServerLevel level, FactionMember owner, int extraSeconds) {
        AABB area = this.getBoundingBox().inflate(SPLASH_RADIUS, 2.0, SPLASH_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, living -> FactionCleric.ally(owner, living))) {
            if (this.effect == MobEffects.INSTANT_HEALTH) {
                living.heal(HEAL_PER_LEVEL << this.amplifier);
            } else {
                living.addEffect(new MobEffectInstance(this.effect, this.duration + extraSeconds * 20, this.amplifier), owner);
            }
        }
    }

    // A lingering bottle marks the spot, and the thrower keeps topping up allies who stand there for a few seconds.
    private void leaveCloud(ServerLevel level, FactionMember owner) {
        Holder<MobEffect> lasting = this.effect == MobEffects.INSTANT_HEALTH ? MobEffects.REGENERATION : this.effect;
        owner.cleric().linger(this.position(), lasting, this.amplifier);
    }
}
