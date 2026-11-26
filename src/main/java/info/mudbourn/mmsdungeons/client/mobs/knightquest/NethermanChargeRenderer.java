package info.mudbourn.mmsdungeons.client.mobs.knightquest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.NethermanCharge;
import info.mudbourn.mmsrendercommon.client.geo.GeoAssets;
import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

// Draws a Netherman charge as its spinning geo model.
public class NethermanChargeRenderer extends EntityRenderer<NethermanCharge, EntityRenderState> {

    private static final Identifier GEO = Identifier.fromNamespaceAndPath("knightquest", "geo/netherman_projectile_charge.geo.json");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("knightquest", "textures/entity/netherman_projectile_charge.png");
    private static final float SCALE = 2.0f;
    private static final float DEGREES_PER_TICK = 18.0f;

    public NethermanChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        GeoModel model = GeoAssets.model(GEO);
        if (model != null) {
            poseStack.pushPose();
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.rotateDegrees(Axis.YP, state.ageInTicks * DEGREES_PER_TICK);
            int light = state.lightCoords;
            collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(TEXTURE),
                (pose, consumer) -> model.draw(pose, consumer, light, OverlayTexture.NO_OVERLAY, -1)
            );
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
