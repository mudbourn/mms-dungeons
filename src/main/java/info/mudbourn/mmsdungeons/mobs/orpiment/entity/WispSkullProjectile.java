package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// A skull of ghostly fire that weakens what it strikes and vanishes on impact.
public class WispSkullProjectile extends AbstractArrow {

    public WispSkullProjectile(EntityType<? extends WispSkullProjectile> type, Level level) {
        super(type, level);
    }

    public WispSkullProjectile(EntityType<? extends WispSkullProjectile> type, LivingEntity owner, Level level) {
        super(type, owner, level, new ItemStack(OrpimentItems.WISP_SKULL), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(OrpimentItems.WISP_SKULL);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() instanceof LivingEntity living && !this.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        this.burst(hit.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        this.burst(Vec3.atCenterOf(hit.getBlockPos()));
    }

    private void burst(Vec3 at) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 10, 1.0, 1.0, 1.0, 0.001);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, 1.0, 1.0, 1.0, 0.001);
            level.playSound(null, at.x, at.y, at.z, OrpimentSounds.WISP_DEATH, SoundSource.BLOCKS, 0.7f, 1.0f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.random.nextFloat() < 0.2f) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 1, 1.0, 1.0, 1.0, 0.01);
        }
        if (this.isInGround()) {
            this.discard();
        }
    }
}
