package info.mudbourn.mmsdungeons.client.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldMobs;
import info.mudbourn.mmsdungeons.mobs.kobolds.block.KoboldSkullType;
import java.util.Set;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public final class KoboldsClient {

    public static final ModelLayerLocation KOBOLD = layer("kobold");
    public static final ModelLayerLocation KOBOLD_RASCAL = layer("kobold_rascal");
    public static final ModelLayerLocation KOBOLD_CHILD = layer("kobold_child");
    public static final ModelLayerLocation SKELEBOLD = layer("skelebold");
    public static final ModelLayerLocation SKULL = layer("kobold_skull");

    private static final float ADULT_SCALE = 0.875f;
    private static final float CHILD_SCALE = 0.45f;

    private KoboldsClient() {
    }

    public static void register() {
        registerLayers();
        registerRenderers();
    }

    public static Identifier skullTexture(KoboldSkullType type) {
        String skin = type == KoboldSkullType.SKELEBOLD ? "skeleton" : "skeleton_wither";
        return MmsDungeons.id("textures/entity/undead/" + skin + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MmsDungeons.id(name), "main");
    }

    private static void registerLayers() {
        ModelLayerRegistry.registerModelLayer(KOBOLD, KoboldModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(KOBOLD_RASCAL, RascalModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(KOBOLD_CHILD, KoboldChildModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(SKELEBOLD, SkeleboldModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(SKULL, KoboldSkullModel::createBodyLayer);
        ModelLayerRegistry.registerArmorModelLayers(KoboldArmor.LAYERS, KoboldArmor::createSet);
    }

    private static void registerRenderers() {
        EntityRendererRegistry.register(KoboldMobs.KOBOLD, context -> adult(context, KoboldLooks.kobold("kobolds/base")));
        EntityRendererRegistry.register(KoboldMobs.KOBOLD_WARRIOR, context -> adult(context, KoboldLooks.kobold("kobolds/warrior")));
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_ENCHANTER,
            context -> adult(context, KoboldLooks.kobold("kobolds/enchanter", "special/james", Set.of("James")))
        );
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_ENGINEER,
            context -> adult(context, KoboldLooks.kobold("kobolds/engineer", "special/dell", Set.of("Dell", "Conagher", "Dell Conagher")))
        );
        EntityRendererRegistry.register(KoboldMobs.KOBOLD_PIRATE, context -> adult(context, KoboldLooks.pirate("kobolds/pirate")));
        EntityRendererRegistry.register(KoboldMobs.KOBOLD_CAPTAIN, context -> adult(context, KoboldLooks.pirate("kobolds/pirate_captain")));
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_RASCAL,
            context -> new KoboldRenderer<>(
                context,
                new RascalModel<>(context.bakeLayer(KOBOLD_RASCAL)),
                ADULT_SCALE,
                KoboldLooks.kobold("kobolds/rascal")
            )
                .holdItems(true)
                .dress(context, RascalModel::new)
        );
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_CHILD,
            context -> new KoboldRenderer<>(
                context,
                new KoboldChildModel<>(context.bakeLayer(KOBOLD_CHILD)),
                CHILD_SCALE,
                KoboldLooks.child()
            )
        );
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_ZOMBIE,
            context -> new KoboldRenderer<>(
                context,
                new ZomboldModel<>(context.bakeLayer(KOBOLD)),
                ADULT_SCALE,
                KoboldLooks.zombold()
            )
                .holdItems(false)
                .dress(context, ZomboldModel::new)
        );
        EntityRendererRegistry.register(
            KoboldMobs.KOBOLD_SKELETON,
            context -> skeletal(context, KoboldLooks.skeleton("undead/skeleton", null))
        );
        EntityRendererRegistry.register(
            KoboldMobs.WITHERBOLD,
            context -> skeletal(context, KoboldLooks.skeleton("undead/skeleton_wither", "eyes/skeleton_wither"))
        );
    }

    private static <T extends Mob> KoboldRenderer<T, KoboldModel<KoboldRenderState>> adult(
        EntityRendererProvider.Context context,
        BiConsumer<? super T, KoboldRenderState> look
    ) {
        return new KoboldRenderer<T, KoboldModel<KoboldRenderState>>(
            context,
            new KoboldModel<>(context.bakeLayer(KOBOLD)),
            ADULT_SCALE,
            look
        )
            .holdItems(false)
            .dress(context, KoboldModel::new);
    }

    private static <T extends Mob> KoboldRenderer<T, SkeleboldModel<KoboldRenderState>> skeletal(
        EntityRendererProvider.Context context,
        BiConsumer<? super T, KoboldRenderState> look
    ) {
        return new KoboldRenderer<T, SkeleboldModel<KoboldRenderState>>(
            context,
            new SkeleboldModel<>(context.bakeLayer(SKELEBOLD)),
            ADULT_SCALE,
            look
        )
            .holdItems(false)
            .dress(context, SkeleboldModel::new);
    }
}
