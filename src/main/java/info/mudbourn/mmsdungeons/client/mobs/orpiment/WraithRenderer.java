package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.Wraith;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class WraithRenderer extends MobRenderer<Wraith, WraithRenderState, WraithModel> {

    private static final Identifier NORMAL = MmsDungeons.id("textures/entity/orpiment/wraith_v2.png");
    private static final Identifier PHASE_2 = MmsDungeons.id("textures/entity/orpiment/wraith_phase_2.png");
    private static final Identifier EYES = MmsDungeons.id("textures/entity/orpiment/wraith_eyes.png");

    public WraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WraithModel(context.bakeLayer(OrpimentClient.WRAITH)), 0.5f);
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                state -> EYES,
                (state, age) -> 1.0f,
                new WraithModel(context.bakeLayer(OrpimentClient.WRAITH)),
                RenderTypes::eyes,
                false
            )
        );
    }

    @Override
    public WraithRenderState createRenderState() {
        return new WraithRenderState();
    }

    @Override
    public void extractRenderState(Wraith wraith, WraithRenderState state, float partialTick) {
        super.extractRenderState(wraith, state, partialTick);
        state.phase2 = wraith.isPhase2();
        state.windingUp = wraith.isWindingUp();
        state.dazed = wraith.isDazed();
        state.summon.copyFrom(wraith.summonState);
        state.idle.copyFrom(wraith.idleState);
        state.fire.copyFrom(wraith.fireState);
        state.move.copyFrom(wraith.moveState);
    }

    @Override
    protected void scale(WraithRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.translate(0.0, -0.9, 0.0);
        float age = state.ageInTicks;
        if (state.windingUp) {
            poseStack.translate(Math.sin(age * 3.1) * 0.06, Math.sin(age * 4.3) * 0.03, Math.cos(age * 2.7) * 0.06);
        }
        if (state.dazed) {
            poseStack.mulPose(new Matrix4f().rotationZ((float) Math.toRadians(Math.sin(age * 0.25) * 18.0)));
        }
    }

    @Override
    public Identifier getTextureLocation(WraithRenderState state) {
        return state.phase2 ? PHASE_2 : NORMAL;
    }
}
