package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.Mth;

// Floats the items a Sap carries inside its body.
public class SapItemsLayer extends RenderLayer<SapRenderState, CubeModel<SapRenderState>> {

    public SapItemsLayer(RenderLayerParent<SapRenderState, CubeModel<SapRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SapRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        int size = state.size;
        float squish = state.squish / (size * 0.5f + 1.0f);
        float scaleXz = 1.0f / (squish + 1.0f);
        float scaleY = 1.0f / scaleXz;
        for (int slot = 0; slot < state.items.size(); slot++) {
            ItemStackRenderState item = state.items.get(slot);
            if (item.isEmpty()) {
                continue;
            }
            Random random = new Random(state.seed ^ slot * 7809847782465536322L);
            float phi = random.nextFloat() * (float) (Math.PI * 2);
            float theta = (random.nextFloat() * (0.7f / size) + 0.15f) * (float) Math.PI;
            float radius = (0.04f + random.nextFloat() * 0.2f) * (1.2f / size);
            float x = radius * Mth.sin(theta) * Mth.cos(phi);
            float y = Mth.clamp(radius * Mth.cos(theta), -0.25f, 0.25f);
            float z = radius * Mth.sin(theta) * Mth.sin(phi);
            float rotX = random.nextFloat() * 360.0f;
            float rotY = random.nextFloat() * 360.0f;
            float rotZ = random.nextFloat() * 360.0f;
            float itemScale = (0.12f + random.nextFloat() * 0.04f) * (float) Math.sqrt(size);
            float bounce = (float) Math.sin(state.ageInTicks * 0.02f + slot) * 0.003f;
            poseStack.pushPose();
            poseStack.translate(x * scaleXz, (1.15f + y + bounce) * scaleY, z * scaleXz);
            poseStack.scale(scaleXz, scaleY, scaleXz);
            poseStack.rotateDegrees(Axis.XP, rotX);
            poseStack.rotateDegrees(Axis.YP, rotY + state.ageInTicks * 0.04f);
            poseStack.rotateDegrees(Axis.ZP, rotZ);
            poseStack.scale(itemScale, itemScale, itemScale);
            item.submit(poseStack, collector, light, LivingEntityRenderer.getOverlayCoords(state, 0.0f), state.outlineColor);
            poseStack.popPose();
        }
    }
}
