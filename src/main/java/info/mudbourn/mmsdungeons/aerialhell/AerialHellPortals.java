package info.mudbourn.mmsdungeons.aerialhell;

import fr.factionbedrock.aerialhell.Registry.AerialHellBlocks;
import fr.factionbedrock.aerialhell.Registry.Worldgen.AerialHellDimensions;
import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.net.PlayerSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// Makes an Aerial Hell portal lit outside the dungeon and Aerial Hell roar, root its lighter, then blast.
public final class AerialHellPortals {

    private static final int ROAR_TICKS = 102;
    private static final int MAX_PORTAL_BLOCKS = 600;
    private static final SoundEvent ROAR = SoundEvent.createVariableRangeEvent(MmsDungeons.id("chained_god_roar"));
    private static final Identifier IMMOBILIZE = Identifier.fromNamespaceAndPath("spell_engine", "immobilize");
    private static final ResourceKey<DamageType> CHAINED_GOD = ResourceKey.create(Registries.DAMAGE_TYPE, MmsDungeons.id("chained_god"));
    private static final List<Blast> PENDING = new ArrayList<>();

    private record Blast(ResourceKey<Level> dimension, List<BlockPos> blocks, Vec3 center, UUID lighter, int endTick) {
    }

    private AerialHellPortals() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ROAR.location(), ROAR);
        ServerTickEvents.END_SERVER_TICK.register(AerialHellPortals::tick);
    }

    // Whether portals work normally in this dimension.
    public static boolean allowsPortals(Level level) {
        return DungeonDimension.isDungeon(level) || level.dimension().equals(AerialHellDimensions.AERIAL_HELL_DIMENSION);
    }

    // Starts the roar and the countdown to the blast for a portal just lit by a player.
    public static void onLit(ServerLevel level, BlockPos pos, ServerPlayer lighter) {
        if (allowsPortals(level)) {
            return;
        }
        List<BlockPos> blocks = portalBlocks(level, pos);
        if (blocks.isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        Vec3 center = center(blocks);
        Holder<SoundEvent> roar = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ROAR);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSounds.play(player, roar, SoundSource.MASTER, 1.0F, 1.0F);
        }
        root(lighter);
        warn(level, center);
        PENDING.add(new Blast(level.dimension(), blocks, center, lighter.getUUID(), server.getTickCount() + ROAR_TICKS));
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Blast> it = PENDING.iterator();
        while (it.hasNext()) {
            Blast blast = it.next();
            if (server.getTickCount() >= blast.endTick) {
                it.remove();
                detonate(server, blast);
            }
        }
    }

    private static void detonate(MinecraftServer server, Blast blast) {
        ServerLevel level = server.getLevel(blast.dimension);
        if (level == null) {
            return;
        }
        Set<BlockPos> portal = new HashSet<>(blast.blocks);
        for (BlockPos pos : blast.blocks) {
            if (level.getBlockState(pos).is(AerialHellBlocks.AERIAL_HELL_PORTAL)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        shatterFrame(level, portal);
        level.explode(
            null,
            blast.center.x,
            blast.center.y,
            blast.center.z,
            FeaturesConfig.get().portalBlastPower,
            Level.ExplosionInteraction.NONE);
        ServerPlayer lighter = server.getPlayerList().getPlayer(blast.lighter);
        if (lighter != null && lighter.isAlive() && lighter.level() == level) {
            lighter.hurtServer(level, level.damageSources().source(CHAINED_GOD), Float.MAX_VALUE);
            if (lighter.isAlive()) {
                lighter.kill(level);
            }
        }
    }

    // Destroys every frame block around the portal, corners included, so it must be rebuilt.
    private static void shatterFrame(ServerLevel level, Set<BlockPos> portal) {
        for (BlockPos pos : portal) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos frame = pos.offset(dx, dy, dz);
                        if (!portal.contains(frame) && !level.getBlockState(frame).isAir() && level.getBlockState(frame).getDestroySpeed(level, frame) >= 0.0F) {
                            level.setBlock(frame, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private static void root(ServerPlayer player) {
        BuiltInRegistries.MOB_EFFECT.get(IMMOBILIZE).ifPresent((Holder<MobEffect> effect) ->
            player.addEffect(new MobEffectInstance(effect, ROAR_TICKS + 2, 0, false, false, true)));
    }

    private static void warn(ServerLevel level, Vec3 center) {
        double radius = FeaturesConfig.get().portalWarningRadius;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= radius * radius) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Someone enraged the Chained God...")));
            }
        }
    }

    // Every connected portal block reachable from a lit position.
    private static List<BlockPos> portalBlocks(ServerLevel level, BlockPos start) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        for (Direction direction : Direction.values()) {
            queue.add(start.relative(direction));
        }
        while (!queue.isEmpty() && found.size() < MAX_PORTAL_BLOCKS) {
            BlockPos pos = queue.poll();
            if (!seen.add(pos) || !level.getBlockState(pos).is(AerialHellBlocks.AERIAL_HELL_PORTAL)) {
                continue;
            }
            found.add(pos);
            for (Direction direction : Direction.values()) {
                queue.add(pos.relative(direction));
            }
        }
        return found;
    }

    private static Vec3 center(List<BlockPos> blocks) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (BlockPos pos : blocks) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        return new Vec3(x / blocks.size(), y / blocks.size(), z / blocks.size());
    }
}
