package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class WraithRenderState extends LivingEntityRenderState {

    public final AnimationState summon = new AnimationState();
    public final AnimationState idle = new AnimationState();
    public final AnimationState fire = new AnimationState();
    public final AnimationState move = new AnimationState();
    public boolean phase2;
    public boolean windingUp;
    public boolean dazed;
}
