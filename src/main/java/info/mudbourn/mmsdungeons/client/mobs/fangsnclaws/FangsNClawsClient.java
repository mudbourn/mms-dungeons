package info.mudbourn.mmsdungeons.client.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.client.mobs.fangsnclaws.FangsGeoRenderer.Look;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsBlocks;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsMobs;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.BabyMob;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.EvilBat;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.FireGhost;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Ghost;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Golem;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.HorseBat;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Imp;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Mimic;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Ogre;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.Owlbear;
import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.PurpleWormArm;
import info.mudbourn.mmsrendercommon.client.anim.GeoAnimations;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.pack.SourcePacks;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

// Client side of Fangs 'n Claws: reads its art from the installed jar and draws its mobs from it.
public final class FangsNClawsClient {

    private static final String SOURCE_NAMESPACE = "fangs_n_claws";
    private static final double RUN_SPEED = 0.18;
    private static final double DRIFT_SPEED = 0.05;
    private static final double STEP_SPEED = 0.01;
    private static final double OWLBEAR_RUN_SPEED = 0.21;
    private static final double FLAP_RISE = 0.03;
    private static final String[] WOLF_COATS =
        {"wild_wolf", "wild_wolf_brown", "wild_wolf_dark_gray", "wild_wolf_gray", "wild_wolf_white"};
    private static final String[] BABY_WOLF_COATS = {
        "black_baby_wild_wolf",
        "brown_baby_wild_wolf",
        "dark_gray_baby_wild_wolf",
        "gray_baby_wild_wolf",
        "snowy_baby_wild_wolf"
    };
    private static final String[] WEREWOLF_COATS = {"werewolf", "werewolf_brown", "werewolf_white"};
    private static final String[] BABY_SCORPION_COATS =
        {"baby_scorpion", "baby_desert_scorpion", "baby_snow_scorpion", "baby_nether_scorpion"};
    private static final String[] NIGHTMARE_COATS = {"nightmare_horse", "nightmare_horse_black"};

    private FangsNClawsClient() {
    }

    public static void register() {
        SourcePacks.accept(SOURCE_NAMESPACE);
        registerDrivers();
        registerRenderers();
        FangsNClawsTooltips.register();
        BlockEntityRendererRegistry.register(FangsNClawsBlocks.GHOST_BLOCK_ENTITY, GhostBlockRenderer::new);
    }

    private static void registerDrivers() {
        GeoAnimations.registerDriver(MmsDungeons.id("ogre"), context -> {
            boolean charging = context.holder() instanceof Ogre ogre
                && ogre.isRunning()
                && ogre.getDeltaMovement().horizontalDistance() > STEP_SPEED;
            return charging ? "run" : null;
        });
        GeoAnimations.registerDriver(MmsDungeons.id("ghost"), context -> {
            if (!(context.holder() instanceof Ghost ghost)) {
                return null;
            }
            if (ghost.isAngry()) {
                return "run";
            }
            return ghost.getDeltaMovement().horizontalDistance() > DRIFT_SPEED ? "walk" : "idle";
        });
        GeoAnimations.registerDriver(MmsDungeons.id("imp"), context -> {
            if (!(context.holder() instanceof Imp imp)) {
                return null;
            }
            double speed = imp.getDeltaMovement().horizontalDistance();
            if (imp.isFlying()) {
                return speed > RUN_SPEED ? "runFlying" : "walkFlying";
            }
            if (speed > RUN_SPEED) {
                return "run";
            }
            return speed > STEP_SPEED ? "walk" : "idle";
        });
        GeoAnimations.registerDriver(MmsDungeons.id("evil_bat"), context ->
            context.holder() instanceof EvilBat bat && bat.isResting() ? "resting" : "flying");
        GeoAnimations.registerDriver(MmsDungeons.id("mimic"), context ->
            context.holder() instanceof Mimic mimic && mimic.isRevealed() ? "idle_open" : "idle");
        GeoAnimations.registerDriver(MmsDungeons.id("owlbear"), context -> {
            if (!(context.holder() instanceof Owlbear owlbear)) {
                return null;
            }
            if (owlbear.isSleeping()) {
                return "sleep";
            }
            if (owlbear.isFlying()) {
                return flightAnimation(owlbear);
            }
            double speed = owlbear.getDeltaMovement().horizontalDistance();
            if (speed > OWLBEAR_RUN_SPEED) {
                return "run";
            }
            return speed > STEP_SPEED ? "walk" : "idle";
        });
        GeoAnimations.registerDriver(MmsDungeons.id("golem"), context -> {
            if (!(context.holder() instanceof Golem golem)) {
                return null;
            }
            return switch (golem.getState()) {
                case Golem.DORMANT -> "sleep";
                case Golem.DOWN -> "vulnerable";
                default -> null;
            };
        });
        GeoAnimations.registerDriver(MmsDungeons.id("purple_worm_arm"), context -> {
            if (!(context.holder() instanceof PurpleWormArm arm)) {
                return null;
            }
            return switch (arm.getArmState()) {
                case PurpleWormArm.SPAWN -> "spawn";
                case PurpleWormArm.ATTACK_LEFT -> "attack_left";
                case PurpleWormArm.ATTACK_RIGHT -> "attack_right";
                case PurpleWormArm.DESPAWN -> "despawn";
                default -> null;
            };
        });
        GeoAnimations.registerDriver(MmsDungeons.id("horse_bat"), context -> {
            if (!(context.holder() instanceof HorseBat bat) || !bat.isFlying()) {
                return null;
            }
            return bat.getDeltaMovement().y > FLAP_RISE ? "fly_flap" : "fly_hovering";
        });
    }

    private static void registerRenderers() {
        ogre(FangsNClawsMobs.OGRE, "ogre", "ogre", "ogre_siamese");
        ogre(FangsNClawsMobs.CAVE_OGRE, "cave_ogre", "cave_ogre", "cave_ogre_siamese");
        geo(FangsNClawsMobs.HELL_OGRE, "hell_ogre");
        geo(FangsNClawsMobs.GOBLIN, "goblin");
        geo(FangsNClawsMobs.DART_GOBLIN, "dart_goblin");
        geo(FangsNClawsMobs.IMP, "imp");
        geo(FangsNClawsMobs.EVIL_BAT, "evil_bat");
        geo(FangsNClawsMobs.SILVER_SKELETON, "silver_skeleton");
        EntityRendererRegistry.register(
            FangsNClawsMobs.GHOST,
            context -> new FangsGeoRenderer<Ghost>(context, MmsDungeons.id("ghost"), ghost -> ghostLook(ghost, "ghost"))
        );
        EntityRendererRegistry.register(
            FangsNClawsMobs.FIRE_GHOST,
            context -> new FangsGeoRenderer<FireGhost>(
                context,
                MmsDungeons.id("fire_ghost"),
                ghost -> ghostLook(ghost, "fire_ghost")
            )
        );
        EntityRendererRegistry.register(
            FangsNClawsMobs.MIMIC,
            context -> new MimicRenderer(context, MmsDungeons.id("mimic"))
        );
        EntityRendererRegistry.register(FangsNClawsMobs.POISONOUS_DART, PoisonousDartRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.VELOCITY_ARROW, VelocityArrowRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.THROWN_PITCHFORK, ThrownPitchforkRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.THROWN_TOMAHAWK, ThrownTomahawkRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.CATCHING_CLAW_HOOK, CatchingClawHookRenderer::new);
        skinned(
            FangsNClawsMobs.WILD_WOLF,
            "wild_wolf",
            wolf -> Look.skin(WOLF_COATS[wolf.getVariant()], "wild_wolf_eyes")
        );
        skinned(
            FangsNClawsMobs.BABY_WILD_WOLF,
            "baby_wild_wolf",
            baby -> Look.skin(BABY_WOLF_COATS[coat(baby, BABY_WOLF_COATS.length)], null)
        );
        skinned(
            FangsNClawsMobs.WEREWOLF,
            "werewolf",
            wolf -> Look.skin(WEREWOLF_COATS[wolf.getVariant()], "werewolf_eyes")
        );
        skinned(
            FangsNClawsMobs.BABY_SCORPION,
            "baby_scorpion",
            baby -> Look.skin(BABY_SCORPION_COATS[coat(baby, BABY_SCORPION_COATS.length)], null)
        );
        skinned(
            FangsNClawsMobs.NIGHTMARE_HORSE,
            "nightmare_horse",
            horse -> Look.skin(NIGHTMARE_COATS[horse.getVariant()], null)
        );
        skinned(FangsNClawsMobs.OWLBEAR, "owlbear", owlbear -> sleeper(owlbear, "owlbear"));
        skinned(FangsNClawsMobs.SHRIKE, "shrike", shrike -> sleeper(shrike, "shrike"));
        geo(FangsNClawsMobs.HYENA, "hyena");
        geo(FangsNClawsMobs.BABY_HYENA, "baby_hyena");
        geo(FangsNClawsMobs.BABY_OWLBEAR, "baby_owlbear");
        geo(FangsNClawsMobs.BABY_SHRIKE, "baby_shrike");
        geo(FangsNClawsMobs.SCORPION, "scorpion");
        geo(FangsNClawsMobs.DESERT_SCORPION, "desert_scorpion");
        geo(FangsNClawsMobs.FROST_SCORPION, "frost_scorpion");
        geo(FangsNClawsMobs.NETHER_SCORPION, "nether_scorpion");
        geo(FangsNClawsMobs.ZOMBIE_HORSE, "zombie_horse");
        geo(FangsNClawsMobs.SKELETON_HORSE, "skeleton_horse");
        geo(FangsNClawsMobs.HORSE_BAT, "horse_bat");
        EntityRendererRegistry.register(FangsNClawsMobs.FEATHER_PROJECTILE, FeatherProjectileRenderer::new);
        EntityRendererRegistry.register(
            FangsNClawsMobs.GOLEM,
            context -> new GolemRenderer(context, MmsDungeons.id("golem"), "golem", "golem_eyes", "golem_grass")
        );
        EntityRendererRegistry.register(
            FangsNClawsMobs.ICE_GOLEM,
            context -> new GolemRenderer(context, MmsDungeons.id("ice_golem"), "ice_golem", null, "ice_golem_snow")
        );
        EntityRendererRegistry.register(
            FangsNClawsMobs.PURPLE_WORM,
            context -> new PurpleWormRenderer(context, MmsDungeons.id("purple_worm"))
        );
        geo(FangsNClawsMobs.PURPLE_WORM_ARM, "purple_worm_arm");
        EntityRendererRegistry.register(FangsNClawsMobs.PURPLE_WORM_PART, NothingRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.ACID_CLOUD, NothingRenderer::new);
        EntityRendererRegistry.register(FangsNClawsMobs.ACID_SPLIT, AcidSplitRenderer::new);
    }

    private static <T extends Ogre> void ogre(EntityType<T> type, String definition, String single, String siamese) {
        EntityRendererRegistry.register(
            type,
            context -> new FangsGeoRenderer<T>(
                context,
                MmsDungeons.id(definition),
                ogre -> ogre.isSiamese() ? eyed(siamese) : eyed(single)
            )
        );
    }

    private static String flightAnimation(Owlbear owlbear) {
        if (owlbear.isBlizzard()) {
            return "fly_blizzard";
        }
        if (owlbear.isDiving()) {
            return "fly_dive_attack_start";
        }
        return owlbear.isFlapping() ? "fly_flap" : "fly_hovering";
    }

    private static Look sleeper(Owlbear owlbear, String name) {
        return owlbear.isSleeping() ? Look.skin(name + "_sleep", null) : Look.skin(name, name + "_eyes");
    }

    private static int coat(BabyMob baby, int coats) {
        return Math.min(baby.getVariant(), coats - 1);
    }

    private static <T extends LivingEntity> void skinned(
        EntityType<? extends T> type,
        String definition,
        Function<T, Look> looks
    ) {
        EntityRendererRegistry.register(
            type,
            context -> new FangsGeoRenderer<T>(context, MmsDungeons.id(definition), looks)
        );
    }

    private static Look ghostLook(Ghost ghost, String name) {
        return ghost.isAngry() ? Look.of(name, "angry_" + name, "angry_" + name + "_eyes") : Look.plain(name, name);
    }

    private static Look eyed(String name) {
        return Look.of(name, name, name + "_eyes");
    }

    private static <T extends LivingEntity> void geo(EntityType<? extends T> type, String definition) {
        EntityRendererRegistry.register(type, context -> new GeoEntityRenderer<T>(context, MmsDungeons.id(definition)));
    }
}
