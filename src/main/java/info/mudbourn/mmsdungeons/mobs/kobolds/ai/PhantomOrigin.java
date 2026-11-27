package info.mudbourn.mmsdungeons.mobs.kobolds.ai;

import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

// Whether a player has taken the Phantom origin, which kobolds treat like the phantoms it is kin to.
// Only touches the Origins API when Origins is loaded.
public final class PhantomOrigin {

    private static final boolean ORIGINS = FabricLoader.getInstance().isModLoaded("origins");
    private static final Identifier ORIGIN_LAYER = Identifier.fromNamespaceAndPath("origins", "origin");
    private static final Identifier PHANTOM = Identifier.fromNamespaceAndPath("mms_origins", "phantom");

    private PhantomOrigin() {
    }

    public static boolean is(Player player) {
        return ORIGINS && Reader.isPhantom(player);
    }

    // Kept in its own class so the Origins classes load only when Origins is present.
    private static final class Reader {

        static boolean isPhantom(Player player) {
            OriginLayer layer;
            try {
                layer = OriginLayers.getLayer(ORIGIN_LAYER);
            } catch (RuntimeException e) {
                return false;
            }
            if (layer == null) {
                return false;
            }
            OriginComponent component = ModComponents.ORIGIN.get(player);
            if (!component.hasOrigin(layer)) {
                return false;
            }
            Origin origin = component.getOrigin(layer);
            return origin != null && PHANTOM.equals(origin.getIdentifier());
        }
    }
}
