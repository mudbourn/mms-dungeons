package info.mudbourn.mmsdungeons.client.mobs.luminous;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.client.mobs.knightquest.VariantGeoRenderer;
import info.mudbourn.mmsdungeons.mobs.luminous.LuminousMobs;
import info.mudbourn.mmsdungeons.mobs.luminous.entity.SunkenSkeleton;
import info.mudbourn.mmsrendercommon.client.anim.GeoAnimations;
import info.mudbourn.mmsrendercommon.client.entity.GeoEntityRenderer;
import info.mudbourn.mmsrendercommon.client.pack.SourcePacks;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class LuminousClient {

    private static final String SOURCE_NAMESPACE = "luminous_monsters";
    private static final float MOVING_SPEED = 0.15f;

    private LuminousClient() {
    }

    public static void register() {
        SourcePacks.accept(SOURCE_NAMESPACE);
        registerDrivers();
        registerRenderers();
    }

    private static void registerDrivers() {
        GeoAnimations.registerDriver(MmsDungeons.id("luminous_zombie"), context -> {
            if (!(context.holder() instanceof Zombie zombie) || zombie.isDeadOrDying()) {
                return null;
            }
            if (zombie.getSwingAnimation(1.0f) > 0.0f) {
                return "attack";
            }
            return zombie.isAggressive() && zombie.walkAnimation.speed(1.0f) > MOVING_SPEED ? "agro" : null;
        });
        GeoAnimations.registerDriver(MmsDungeons.id("luminous_skeleton"), context -> {
            boolean aiming = context.holder() instanceof AbstractSkeleton skeleton
                && !skeleton.isDeadOrDying()
                && skeleton.isAggressive();
            return aiming ? "aim" : null;
        });
    }

    private static void registerRenderers() {
        geo(LuminousMobs.MINER_ZOMBIE, "miner_zombie");
        geo(LuminousMobs.ICY_ZOMBIE, "icy_zombie");
        geo(LuminousMobs.SWAMP_ZOMBIE, "swamp_zombie");
        geo(LuminousMobs.DARK_OAK_ZOMBIE, "dark_oak_zombie");
        geo(LuminousMobs.CHERRY_ZOMBIE, "cherry_zombie");
        geo(LuminousMobs.SAVANNA_ZOMBIE, "savanna_zombie");
        geo(LuminousMobs.CRIMSON_WALKER, "crimson_walker");
        geo(LuminousMobs.MINER_SKELETON, "miner_skeleton");
        geo(LuminousMobs.ICY_SKELETON, "icy_skeleton");
        geo(LuminousMobs.SWAMP_SKELETON, "swamp_skeleton");
        geo(LuminousMobs.DARK_OAK_SKELETON, "dark_oak_skeleton");
        geo(LuminousMobs.HOLLOW_SKELETON, "hollow_skeleton");
        geo(LuminousMobs.CHERRY_SKELETON, "cherry_skeleton");
        geo(LuminousMobs.SAVANNA_SKELETON, "savanna_skeleton");
        geo(LuminousMobs.REVENANT_SKELETON, "revenant_skeleton");
        EntityRendererRegistry.register(
            LuminousMobs.SUNKEN_SKELETON,
            context -> new VariantGeoRenderer<SunkenSkeleton>(
                context,
                MmsDungeons.id("sunken_skeleton"),
                skeleton -> source(switch (skeleton.getVariant()) {
                    case SunkenSkeleton.VARIANT_RED -> "sunken_red";
                    case SunkenSkeleton.VARIANT_YELLOW -> "sunken_yellow";
                    default -> "sunken_blue";
                })
            )
        );
    }

    private static <T extends LivingEntity> void geo(EntityType<? extends T> type, String definition) {
        EntityRendererRegistry.register(type, context -> new GeoEntityRenderer<T>(context, MmsDungeons.id(definition)));
    }

    private static Identifier source(String texture) {
        return Identifier.fromNamespaceAndPath(SOURCE_NAMESPACE, "textures/entities/" + texture + ".png");
    }
}
