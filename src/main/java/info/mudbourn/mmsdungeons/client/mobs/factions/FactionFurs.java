package info.mudbourn.mmsdungeons.client.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import info.mudbourn.mmsorigins.client.beastfolk.BeastfolkFeatureRenderer;
import info.mudbourn.mmsorigins.client.fur.FurFeatureRenderer;
import info.mudbourn.mmsorigins.client.fur.FurState;
import info.mudbourn.mmsorigins.client.wings.ButterflyFlap;
import info.mudbourn.mmsorigins.client.wings.ButterflyWingsFeatureRenderer;
import info.mudbourn.mmsorigins.client.wings.ButterflyWingsModel;
import info.mudbourn.mmsorigins.client.wings.WingState;
import info.mudbourn.mmsorigins.client.wings.WingsFeatureRenderer;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

// Draws MMS Origins' origin furs on faction members. Only touched when MMS Origins is loaded, so the class never resolves without it.
public final class FactionFurs {

    private static final List<String> FAIRY_WINGS = List.of("monarch_butterfly", "blue_butterfly", "cirno_wings", "pixie_wings", "slime_wings");
    private static final List<String> BEASTFOLK_TAILS = List.of("beastfolk_tail_fox", "beastfolk_tail_wolf");
    private static final List<String> FAIRY_WING_FURS = List.of("cirno_wings", "pixie_wings", "slime_wings");
    private static final List<String> AVIAN_WINGS = List.of(
        "feathered_wings",
        "vanilla_elytra",
        "golden_wings",
        "colored_wings",
        "paper_wings",
        "magic_wings",
        "flandre_wings",
        "ikaros_wings",
        "nymph_wings",
        "astraea_wings",
        "chaos_wings",
        "hiyori_wings",
        "melan_wings"
    );

    private FactionFurs() {
    }

    public static RenderLayer<AvatarRenderState, PlayerModel> layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        return new FurFeatureRenderer(parent);
    }

    public static RenderLayer<AvatarRenderState, PlayerModel> beastfolk(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
        return new BeastfolkFeatureRenderer(parent, context::bakeLayer);
    }

    public static RenderLayer<AvatarRenderState, PlayerModel> wings(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
        return new WingsFeatureRenderer(parent, context.bakeLayer(ModelLayers.ELYTRA));
    }

    public static RenderLayer<AvatarRenderState, PlayerModel> butterflyWings(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
        return new ButterflyWingsFeatureRenderer(parent, context.bakeLayer(ButterflyWingsModel.LAYER));
    }

    // Stamps the fur, wings, tail and claws on the render state, the wing and tail picked from the mob's id so they never change. MMS Origins' layers draw them.
    public static void stamp(AvatarRenderState state, FactionOrigin origin, UUID id, boolean fighting) {
        Identifier wing = wingFor(origin, id);
        Identifier fur = furFor(origin);
        if (origin == FactionOrigin.FAIRY && wing != null && FAIRY_WING_FURS.contains(wing.getPath())) {
            fur = Identifier.fromNamespaceAndPath("mms_origins", "fairy_" + wing.getPath().replace("_wings", ""));
        }
        ((FurState) state).mmsOrigins$setFurOrigin(fur);
        boolean beastfolk = origin == FactionOrigin.BEASTFOLK;
        ((FurState) state).mmsOrigins$setTailOption(beastfolk ? Identifier.fromNamespaceAndPath("originstweaks", BEASTFOLK_TAILS.get(Math.floorMod(id.hashCode(), BEASTFOLK_TAILS.size()))) : null);
        ((FurState) state).mmsOrigins$setClawsOut(beastfolk && fighting);
        ((WingState) state).mmsOrigins$setWingOption(wing);
        ((WingState) state).mmsOrigins$setWingFlapDegrees(ButterflyFlap.idleDegrees(state.ageInTicks));
    }

    // A fairy's or avian's wings, one of the options its players choose from.
    private static @Nullable Identifier wingFor(FactionOrigin origin, UUID id) {
        List<String> options = switch (origin) {
            case FAIRY -> FAIRY_WINGS;
            case AVIAN -> AVIAN_WINGS;
            default -> List.of();
        };
        if (options.isEmpty()) {
            return null;
        }
        String namespace = origin == FactionOrigin.FAIRY ? "mms_origins" : "originstweaks";
        return Identifier.fromNamespaceAndPath(namespace, options.get(Math.floorMod(id.hashCode(), options.size())));
    }

    // The MMS Origins fur for each origin.
    private static @Nullable Identifier furFor(FactionOrigin origin) {
        return switch (origin) {
            case ARACHNID, AVIAN, BLAZEBORN, ENDERIAN, PHANTOM, SHULK -> Identifier.fromNamespaceAndPath("origins", origin.id());
            case BEASTFOLK -> Identifier.fromNamespaceAndPath("origins", "beastfolk");
            case FISHMAN -> Identifier.fromNamespaceAndPath("origins", "fishman");
            case FAIRY, FLORAN, TRUFFLE, PIGLIN -> Identifier.fromNamespaceAndPath("mms_origins", origin.id());
            case NONE -> null;
        };
    }
}
