package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

// A bead of tar that hangs, falls and splashes in three phases.
public class TarDripParticle extends SingleQuadParticle {

    private static final int HANGING = 0;
    private static final int FALLING = 1;

    private final int phase;

    public TarDripParticle(ClientLevel level, double x, double y, double z, int phase, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.phase = phase;
        this.setColor(0.05f, 0.05f, 0.05f);
        this.setSize(0.01f, 0.01f);
        if (phase == HANGING) {
            this.gravity = 0.0012f;
            this.lifetime = 40;
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        } else {
            this.gravity = 0.06f;
            this.lifetime = (int) ((phase == FALLING ? 64.0 : 16.0) / (Math.random() * 0.8 + 0.2));
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (--this.lifetime <= 0) {
            this.remove();
            if (this.phase == HANGING) {
                this.level.addParticle(OrpimentParticles.TAR_DRIP, this.x, this.y, this.z, 1.0, 0.0, 0.0);
            }
            return;
        }
        if (this.phase == HANGING) {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        } else if (this.phase == FALLING) {
            this.yd -= this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.98f;
            this.yd *= 0.98f;
            this.zd *= 0.98f;
            if (this.onGround) {
                this.remove();
                this.level.addParticle(OrpimentParticles.TAR_DRIP, this.x, this.y, this.z, 2.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }
}
