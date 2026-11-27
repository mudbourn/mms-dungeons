package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.HumanFactions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// A faction fighter's weapon thrown at a player on low health; the thrower draws it again a moment later.
public class ThrownWeapon extends ThrowableItemProjectile {

    private float damage = 6.0f;

    public ThrownWeapon(EntityType<? extends ThrownWeapon> type, Level level) {
        super(type, level);
    }

    public ThrownWeapon(Level level, LivingEntity owner, ItemStack weapon, float damage) {
        super(HumanFactions.THROWN_WEAPON, owner, level, weapon);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel level && result.getEntity() != this.getOwner()) {
            LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
            result.getEntity().hurtServer(level, this.damageSources().thrown(this, owner), this.damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
