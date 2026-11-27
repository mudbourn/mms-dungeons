package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PurpleWorm;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

// Draws the Purple Worm over its whole height, and not at all while it lies burrowed.
public class PurpleWormRenderer extends GeoEntityRenderer<PurpleWorm> {

    private static final double HEIGHT = 15.0;

    public PurpleWormRenderer(EntityRendererProvider.Context context, Identifier definition) {
        super(context, definition);
    }

    @Override
    public boolean shouldRender(
        PurpleWorm worm,
        Frustum frustum,
        double camX,
        double camY,
        double camZ,
        float partialTick
    ) {
        return worm.isBodyShown() && super.shouldRender(worm, frustum, camX, camY, camZ, partialTick);
    }

    @Override
    protected AABB getBoundingBoxForCulling(PurpleWorm worm, float partialTick) {
        return worm.getBoundingBox().expandTowards(0.0, HEIGHT, 0.0).inflate(2.0, 0.0, 2.0);
    }
}
