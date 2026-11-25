package info.mudbourn.mmsdungeons.mobs.orpiment.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentEffects;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// A thrown clump of tar that leaves the target Tarred.
public class TarClumpProjectile extends ThrowableItemProjectile {

    private static final float DAMAGE = 1.3f;

    public TarClumpProjectile(EntityType<? extends TarClumpProjectile> type, Level level) {
        super(type, level);
    }

    public TarClumpProjectile(EntityType<? extends TarClumpProjectile> type, double x, double y, double z, Level level, ItemStack stack) {
        super(type, x, y, z, level, stack);
    }

    public TarClumpProjectile(EntityType<? extends TarClumpProjectile> type, LivingEntity owner, Level level, ItemStack stack) {
        super(type, owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return OrpimentItems.TAR_CLUMP;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != 3) {
            return;
        }
        ParticleOptions particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem());
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(
                particle,
                this.getX(),
                this.getY(),
                this.getZ(),
                this.random.nextGaussian() * 0.15,
                this.random.nextGaussian() * 0.15,
                this.random.nextGaussian() * 0.15
            );
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity target = hit.getEntity();
        if (this.level() instanceof ServerLevel level) {
            target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(OrpimentEffects.TARRED, 100, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
