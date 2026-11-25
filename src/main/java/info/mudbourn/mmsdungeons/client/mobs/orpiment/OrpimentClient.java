package info.mudbourn.mmsdungeons.client.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentFluids;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.sprite.Material;

public final class OrpimentClient {

    public static final ModelLayerLocation WRAITH = layer("wraith");
    public static final ModelLayerLocation WISP = layer("wisp");
    public static final ModelLayerLocation WISP_FLIPPED = layer("wisp_flipped");
    public static final ModelLayerLocation CRAW = layer("craw");
    public static final ModelLayerLocation CRAW_SADDLE = layer("craw_saddle");
    public static final ModelLayerLocation TAR_CUBE = layer("tar_cube");
    public static final ModelLayerLocation SAP = layer("sap");
    public static final ModelLayerLocation CUBE_OUTER = layer("cube_outer");

    private OrpimentClient() {
    }

    public static void register() {
        registerLayers();
        registerRenderers();
        registerParticles();
        registerFluids();
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MmsDungeons.id("possessed_overlay"), new PossessedOverlay());
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MmsDungeons.id("orpiment_" + name), "main");
    }

    private static void registerLayers() {
        ModelLayerRegistry.registerModelLayer(WRAITH, OrpimentLayers::wraith);
        ModelLayerRegistry.registerModelLayer(WISP, OrpimentLayers::wisp);
        ModelLayerRegistry.registerModelLayer(WISP_FLIPPED, OrpimentLayers::wispFlipped);
        ModelLayerRegistry.registerModelLayer(CRAW, OrpimentLayers::craw);
        ModelLayerRegistry.registerModelLayer(CRAW_SADDLE, OrpimentLayers::crawSaddle);
        ModelLayerRegistry.registerModelLayer(TAR_CUBE, OrpimentLayers::tarCubeInner);
        ModelLayerRegistry.registerModelLayer(SAP, OrpimentLayers::sapInner);
        ModelLayerRegistry.registerModelLayer(CUBE_OUTER, OrpimentLayers::cubeOuter);
    }

    private static void registerRenderers() {
        EntityRendererRegistry.register(OrpimentMobs.TAR_CUBE, TarCubeRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.SAP, SapRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.WRAITH, WraithRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.WISP, WispRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.CRAW, CrawRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.TAR_CLUMP_PROJECTILE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(OrpimentMobs.WISP_SKULL_PROJECTILE, ThrownItemRenderer::new);
    }

    private static void registerParticles() {
        ParticleProviderRegistry particles = ParticleProviderRegistry.getInstance();
        particles.register(
            OrpimentParticles.AMBER_MOTE,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new OrpimentParticle(
                level, x, y, z, vx, vy, vz, sprites, new OrpimentParticle.Spec(1.0f, 21, 61, -0.005f, 0.01, 0, 0, false)
            )
        );
        particles.register(
            OrpimentParticles.SPARK,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new OrpimentParticle(
                level, x, y, z, vx, vy, vz, sprites, new OrpimentParticle.Spec(1.3f, 22, 44, -0.4f, 0.0, 3, 8, false)
            )
        );
        particles.register(
            OrpimentParticles.TAR_STRAND,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new OrpimentParticle(
                level, x, y, z, vx, vy, vz, sprites, new OrpimentParticle.Spec(2.6f, 22, 45, 0.0f, 0.0, 4, 7, false)
            )
        );
        particles.register(
            OrpimentParticles.WRAITH_SKULL,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new OrpimentParticle(
                level, x, y, z, vx, vy, vz, sprites, new OrpimentParticle.Spec(3.0f, 7, 12, -0.3f, 0.3, 0, 0, true)
            )
        );
        particles.register(
            OrpimentParticles.YELLOW_GLINT,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new OrpimentParticle(
                level, x, y, z, vx, vy, vz, sprites, new OrpimentParticle.Spec(1.0f, 7, 17, 0.0f, 1.0, 0, 0, false)
            )
        );
        particles.register(
            OrpimentParticles.TAR_DRIP,
            sprites -> (options, level, x, y, z, vx, vy, vz, random) -> new TarDripParticle(level, x, y, z, (int) vx, sprites)
        );
    }

    private static void registerFluids() {
        FluidModel.Unbaked model = new FluidModel.Unbaked(
            new Material(MmsDungeons.id("block/orpiment/tar_still_v1")),
            new Material(MmsDungeons.id("block/orpiment/tar_flowing_v1")),
            null,
            null
        );
        FluidRenderingRegistry.register(OrpimentFluids.TAR, OrpimentFluids.FLOWING_TAR, model);
    }
}
