package info.mudbourn.mmsdungeons.mobs.knightquest.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

// A charge that hangs in the air, sometimes sinking, and explodes after a short fuse or on touching a block.
public class NethermanCharge extends Entity {

    private static final int MIN_FUSE = 20;
    private static final int FUSE_SPREAD = 100;
    private static final float RADIUS = 3.0f;
    private static final double SINK = 0.03;
    private static final double MAX_SINK = 0.6;

    private int fuse;
    private boolean sinking;

    public NethermanCharge(EntityType<? extends NethermanCharge> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.fuse = MIN_FUSE + this.random.nextInt(FUSE_SPREAD);
        this.sinking = this.random.nextBoolean();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.fuse--;
        if (this.sinking) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, Math.max(motion.y - SINK, -MAX_SINK), motion.z);
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.fuse <= 0 || this.horizontalCollision || this.verticalCollision) {
            level.explode(this, this.getX(), this.getY(), this.getZ(), RADIUS, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.fuse = input.getIntOr("Fuse", MIN_FUSE);
        this.sinking = input.getBooleanOr("Sinking", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Fuse", this.fuse);
        output.putBoolean("Sinking", this.sinking);
    }
}
