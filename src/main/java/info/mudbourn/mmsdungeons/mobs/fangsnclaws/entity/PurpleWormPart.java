package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// One hit box along the Purple Worm's body: blows to it land on the worm.
public class PurpleWormPart extends Entity {

    private PurpleWorm worm;

    public PurpleWormPart(EntityType<? extends PurpleWormPart> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void attach(PurpleWorm worm) {
        this.worm = worm;
    }

    public boolean isAttachedTo(PurpleWorm other) {
        return this.worm == other;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && (this.worm == null || !this.worm.isAlive())) {
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.worm != null && this.worm.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPickable() {
        return this.worm != null && this.worm.isBodyShown();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isPickable();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean is(Entity other) {
        return this == other || this.worm == other;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
