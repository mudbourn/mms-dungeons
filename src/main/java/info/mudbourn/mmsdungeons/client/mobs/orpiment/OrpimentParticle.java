package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

// A sprite particle drifting with a fixed gravity, optionally cycling through its frames.
public class OrpimentParticle extends SingleQuadParticle {

    public record Spec(float scale, int lifetime, int lifetimeSpread, float gravity, double velocity, int frameTicks, int frames, boolean fullbright) {
    }

    private final SpriteSet sprites;
    private final Spec spec;

    public OrpimentParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Spec spec) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.spec = spec;
        this.setSize(0.2f, 0.2f);
        this.quadSize *= spec.scale();
        this.lifetime = Math.max(1, spec.lifetime() + this.random.nextInt(spec.lifetimeSpread() * 2 + 1) - spec.lifetimeSpread());
        this.gravity = spec.gravity();
        this.hasPhysics = true;
        this.xd = vx * spec.velocity();
        this.yd = vy * spec.velocity();
        this.zd = vz * spec.velocity();
        if (spec.frames() > 0) {
            this.setSpriteFromAge(sprites);
        } else {
            this.setSprite(sprites.get(this.random));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed && this.spec.frames() > 0) {
            this.setSprite(this.sprites.get(this.age / this.spec.frameTicks() % this.spec.frames() + 1, this.spec.frames()));
        }
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return this.spec.fullbright() ? 15728880 : super.getLightCoords(partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }
}
