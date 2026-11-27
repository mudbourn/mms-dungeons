package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsrendercommon.client.entity.GeoEntityDefinition;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderState;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.geo.GeoTexture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

// A geo entity renderer whose model, texture and glowing eyes follow the entity.
public class FangsGeoRenderer<T extends LivingEntity> extends GeoEntityRenderer<T> {

    private static final String SOURCE_NAMESPACE = "fangs_n_claws";

    // One entity's model, texture and eye glow, named by path inside the source namespace, the model kept when null.
    public record Look(String geo, String texture, String eyes) {

        public static Look of(String geo, String texture, String eyes) {
            return new Look(geo, texture, eyes);
        }

        public static Look plain(String geo, String texture) {
            return new Look(geo, texture, null);
        }

        public static Look skin(String texture, String eyes) {
            return new Look(null, texture, eyes);
        }
    }

    private final Function<T, Look> looks;
    private final Map<Look, GeoEntityDefinition> variants = new HashMap<>();
    private GeoEntityDefinition cachedBase;
    private T current;

    public FangsGeoRenderer(EntityRendererProvider.Context context, Identifier definition, Function<T, Look> looks) {
        super(context, definition);
        this.looks = looks;
    }

    @Override
    public void extractRenderState(T entity, GeoEntityRenderState state, float partialTick) {
        this.current = entity;
        super.extractRenderState(entity, state, partialTick);
        this.current = null;
    }

    @Override
    public GeoEntityDefinition definition() {
        GeoEntityDefinition base = super.definition();
        if (base == null || this.current == null) {
            return base;
        }
        if (base != this.cachedBase) {
            this.variants.clear();
            this.cachedBase = base;
        }
        return this.variants.computeIfAbsent(this.looks.apply(this.current), look -> variant(base, look));
    }

    private static GeoEntityDefinition variant(GeoEntityDefinition base, Look look) {
        return new GeoEntityDefinition(
            look.geo() == null ? base.geo() : source("geo/" + look.geo() + ".geo.json"),
            new GeoTexture(source("textures/entity/" + look.texture() + ".png"), 1, 1),
            look.eyes() == null
                ? Optional.empty()
                : Optional.of(new GeoTexture(source("textures/entity/glowing_eyes/" + look.eyes() + ".png"), 1, 1)),
            base.translucent(),
            base.scale(),
            base.shadowRadius(),
            base.headBone(),
            base.walk(),
            base.attack(),
            base.death(),
            base.heldItems(),
            base.attachments(),
            base.restRotations(),
            base.armorBones(),
            base.animation()
        );
    }

    private static Identifier source(String path) {
        return Identifier.fromNamespaceAndPath(SOURCE_NAMESPACE, path);
    }
}
