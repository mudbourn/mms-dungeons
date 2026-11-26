package info.mudbourn.mmsdungeons.client.mobs.knightquest;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.EldBomb;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Gremlin;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Ratman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Samhain;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Swampman;
import info.mudbourn.mmsrendercommon.client.anim.GeoAnimations;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.pack.SourcePacks;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class KnightQuestClient {

    private static final String SOURCE_NAMESPACE = "knightquest";
    private static final float FUSE_FLASH_TICKS = 10.0f;

    private KnightQuestClient() {
    }

    public static void register() {
        SourcePacks.accept(SOURCE_NAMESPACE);
        registerDrivers();
        registerRenderers();
    }

    private static void registerDrivers() {
        GeoAnimations.registerDriver(
            MmsDungeons.id("eld_bomb"),
            context -> context.holder() instanceof EldBomb bomb && bomb.swellTicks(1.0f) > FUSE_FLASH_TICKS ? "sneak" : null
        );
        GeoAnimations.registerDriver(MmsDungeons.id("netherman"), context -> {
            if (!(context.holder() instanceof Netherman netherman) || netherman.isDeadOrDying()) {
                return null;
            }
            if (netherman.isSummoning()) {
                return "summon";
            }
            if (netherman.getPhase() == 3 && netherman.getSwitch3() < Netherman.SWITCH_3_TICKS) {
                return "phase3";
            }
            if (netherman.getPhase() == 2 && netherman.getSwitch2() < Netherman.SWITCH_2_TICKS) {
                return "phase2";
            }
            return switch (netherman.getAction()) {
                case Netherman.ACTION_FLAME -> "attack_special";
                case Netherman.ACTION_CLONES -> "attack_special2";
                case Netherman.ACTION_DARKNESS -> "attack_special3";
                default -> null;
            };
        });
        GeoAnimations.registerDriver(MmsDungeons.id("samhain"), context -> {
            if (!(context.holder() instanceof Samhain samhain) || !samhain.isInSittingPose()) {
                return null;
            }
            return switch (samhain.getSitVariation()) {
                case 0 -> "sit";
                case 1 -> "sit3";
                default -> "sit2";
            };
        });
    }

    private static void registerRenderers() {
        EntityRendererRegistry.register(
            KnightQuestMobs.GREMLIN,
            context -> new VariantGeoRenderer<Gremlin>(
                context,
                MmsDungeons.id("gremlin"),
                gremlin -> source(gremlin.getPhase() == 2 ? "gremlin_angry" : "gremlin")
            )
        );
        EntityRendererRegistry.register(
            KnightQuestMobs.ELD_BOMB,
            context -> new VariantGeoRenderer<EldBomb>(
                context,
                MmsDungeons.id("eld_bomb"),
                bomb -> source(bomb.swellTicks(1.0f) > FUSE_FLASH_TICKS && bomb.tickCount / 5 % 2 == 0 ? "eldbomb_white" : "eldbomb")
            )
        );
        EntityRendererRegistry.register(
            KnightQuestMobs.SWAMPMAN,
            context -> new VariantGeoRenderer<Swampman>(
                context,
                MmsDungeons.id("swampman"),
                swampman -> source(swampman.getPhase() == 2 ? "swampman_2" : "swampman")
            )
        );
        EntityRendererRegistry.register(
            KnightQuestMobs.RATMAN,
            context -> new VariantGeoRenderer<Ratman>(context, MmsDungeons.id("ratman"), ratman -> source("ratman" + ratman.getVariant()))
        );
        geo(KnightQuestMobs.BAD_PATCH, "bad_patch");
        geo(KnightQuestMobs.ELD_KNIGHT, "eld_knight");
        geo(KnightQuestMobs.LIZZY, "lizzy");
        geo(KnightQuestMobs.MOMMA_LIZZY, "momma_lizzy");
        geo(KnightQuestMobs.SAMHAIN, "samhain");
        geo(KnightQuestMobs.GHASTLING, "ghastling");
        geo(KnightQuestMobs.GHOSTY, "ghosty");
        geo(KnightQuestMobs.FALLEN_KNIGHT, "fallen_knight");
        geo(KnightQuestMobs.NETHERMAN_CLONE, "netherman_clone");
        EntityRendererRegistry.register(KnightQuestMobs.NETHERMAN, context -> new NethermanRenderer(context, MmsDungeons.id("netherman")));
        EntityRendererRegistry.register(KnightQuestMobs.NETHERMAN_CHARGE, NethermanChargeRenderer::new);
        EntityRendererRegistry.register(KnightQuestMobs.SWAMPMAN_AXE, SwampmanAxeRenderer::new);
    }

    private static <T extends LivingEntity> void geo(EntityType<? extends T> type, String definition) {
        EntityRendererRegistry.register(type, context -> new GeoEntityRenderer<T>(context, MmsDungeons.id(definition)));
    }

    private static Identifier source(String texture) {
        return Identifier.fromNamespaceAndPath(SOURCE_NAMESPACE, "textures/entity/" + texture + ".png");
    }
}
