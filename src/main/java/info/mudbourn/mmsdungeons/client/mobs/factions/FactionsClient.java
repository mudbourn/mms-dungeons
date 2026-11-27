package info.mudbourn.mmsdungeons.client.mobs.factions;

import info.mudbourn.mmsdungeons.client.mobs.swing.MobAnimations;
import info.mudbourn.mmsdungeons.mobs.factions.HumanFactions;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class FactionsClient {

    public static final boolean ANIMATED = FabricLoader.getInstance().isModLoaded("player_animation_library") && FabricLoader.getInstance().isModLoaded("bettercombat");

    private FactionsClient() {
    }

    public static void register() {
        for (var type : HumanFactions.types()) {
            EntityRendererRegistry.register(type, FactionRenderer::new);
        }
        EntityRendererRegistry.register(HumanFactions.THROWN_WEAPON, ThrownItemRenderer::new);
        EntityRendererRegistry.register(HumanFactions.THROWN_BUFF, ThrownItemRenderer::new);
        if (ANIMATED) {
            MobAnimations.register();
        }
    }
}
