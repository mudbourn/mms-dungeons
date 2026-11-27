package info.mudbourn.mmsdungeons.mobs.fangsnclaws.effect;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Stacks up with each hit and bursts into a fiery explosion at the third stack.
public class FlamebrandEffect extends MobEffect {

    private static final int STACK_TICKS = 200;
    private static final int BURST_AMPLIFIER = 2;
    private static final int BURN_TICKS = 100;
    private static final int SMOKE_INTERVAL = 8;
    private static final Map<LivingEntity, UUID> SOURCES = Collections.synchronizedMap(new WeakHashMap<>());

    private final float radius;
    private final float damage;

    public FlamebrandEffect(int color, float radius, float damage) {
        super(MobEffectCategory.HARMFUL, color);
        this.radius = radius;
        this.damage = damage;
    }

    // Adds one stack of the given flamebrand to a target and remembers who spared it from the burst.
    public static void stack(LivingEntity target, LivingEntity source, Holder<MobEffect> effect) {
        if (source != null) {
            SOURCES.put(target, source.getUUID());
        }
        MobEffectInstance current = target.getEffect(effect);
        int amplifier = current == null ? 0 : Math.min(current.getAmplifier() + 1, BURST_AMPLIFIER);
        target.addEffect(new MobEffectInstance(effect, STACK_TICKS, amplifier, false, false, true));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return amplifier >= BURST_AMPLIFIER || duration % SMOKE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (amplifier >= BURST_AMPLIFIER) {
            this.burst(level, entity);
            return false;
        }
        level.sendParticles(
            ParticleTypes.SMOKE,
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ(),
            3,
            0.3,
            0.4,
            0.3,
            0.01
        );
        return true;
    }

    private void burst(ServerLevel level, LivingEntity center) {
        UUID source = SOURCES.remove(center);
        double radiusSqr = this.radius * this.radius;
        AABB area = center.getBoundingBox().inflate(this.radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim.distanceToSqr(center.position()) >= radiusSqr || victim.getUUID().equals(source)) {
                continue;
            }
            victim.setInvulnerableTime(0);
            victim.hurtServer(level, level.damageSources().magic(), this.damage);
            victim.igniteForTicks(BURN_TICKS);
        }
        Vec3 middle = center.getBoundingBox().getCenter();
        level.sendParticles(
            ParticleTypes.FLAME,
            middle.x,
            middle.y,
            middle.z,
            (int) (this.radius * 8),
            1.0,
            1.0,
            1.0,
            0.05
        );
        level.playSound(
            null,
            center.getX(),
            center.getY(),
            center.getZ(),
            SoundEvents.GENERIC_EXPLODE,
            SoundSource.PLAYERS,
            2.5f,
            0.9f + level.getRandom().nextFloat() * 0.2f
        );
    }
}
