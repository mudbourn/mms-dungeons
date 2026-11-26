package info.mudbourn.mmsdungeons.client.mobs.knightquest;

import info.mudbourn.mmsrendercommon.client.entity.GeoEntityDefinition;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderState;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.geo.GeoTexture;
import java.util.function.Function;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

// A geo entity renderer whose texture, and glow mask beside it, follows the entity.
public class VariantGeoRenderer<T extends LivingEntity> extends GeoEntityRenderer<T> {

    private final Function<T, Identifier> variant;

    public VariantGeoRenderer(EntityRendererProvider.Context context, Identifier definition, Function<T, Identifier> variant) {
        super(context, definition);
        this.variant = variant;
    }

    @Override
    public void extractRenderState(T entity, GeoEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        GeoEntityDefinition definition = state.definition;
        if (definition == null) {
            return;
        }
        Identifier texture = this.variant.apply(entity);
        state.definition = new GeoEntityDefinition(
            definition.geo(),
            new GeoTexture(texture, 1, 1),
            definition.emissiveTexture().map(glow -> new GeoTexture(glowMask(texture), 1, 1)),
            definition.translucent(),
            definition.scale(),
            definition.shadowRadius(),
            definition.headBone(),
            definition.walk(),
            definition.attack(),
            definition.death(),
            definition.heldItems(),
            definition.attachments(),
            definition.animation()
        );
    }

    private static Identifier glowMask(Identifier texture) {
        return texture.withPath(path -> path.replace(".png", "_glowmask.png"));
    }
}
