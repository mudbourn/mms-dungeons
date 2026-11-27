package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.mixin.dungeon.ServerLevelStructureCheckAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Runs the Deep Region rebuild: waits for an empty server or forces a countdown, closes the dungeon, then reopens it on a new generation.
public final class DeepRebuild {

    private static final Logger LOG = LoggerFactory.getLogger("mms_dungeons");
    private static final int CHECK_INTERVAL = 20;
    private static final long HOUR_MILLIS = 3_600_000L;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final List<Integer> WARN_AT = List.of(600, 300, 120, 60, 30, 10, 5, 4, 3, 2, 1);
    private static final int TITLE_FROM = 10;

    public static final Component CLOSED = Component.literal("The dungeon is closed while the Deep Region rebuilds.");

    private enum State {
        OPEN,
        COUNTDOWN,
        CLOSED
    }

    private static State state = State.OPEN;
    private static long dueSince;
    private static long countdownEnd;
    private static long closedAt;
    private static int lastWarning;

    private DeepRebuild() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeepRebuild::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            state = State.OPEN;
            dueSince = 0L;
            if (isDue(server) && server.getPlayerList().getPlayers().isEmpty()) {
                close(server);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
    }

    public static boolean isClosed() {
        return state == State.CLOSED;
    }

    // Sends a player out of the dungeon to their overworld spawn, or the world spawn when theirs is not in the overworld.
    public static void evacuate(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        TeleportTransition home = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        if (home.newLevel() != overworld) {
            BlockPos spawn = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, overworld.getRespawnData().pos());
            home = new TeleportTransition(
                overworld,
                Vec3.atBottomCenterOf(spawn),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.DO_NOTHING
            );
        }
        DungeonPortals.markPortalMove(player);
        player.teleport(home);
    }

    private static boolean isDue(MinecraftServer server) {
        long interval = (long) (FeaturesConfig.get().deepRebuildDays * DAY_MILLIS);
        return System.currentTimeMillis() >= DungeonGeneration.lastRebuild(server) + interval;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        switch (state) {
            case OPEN -> tickOpen(server, now);
            case COUNTDOWN -> tickCountdown(server, now);
            case CLOSED -> tickClosed(server, now);
        }
    }

    private static void tickOpen(MinecraftServer server, long now) {
        if (!isDue(server)) {
            return;
        }
        if (server.getPlayerList().getPlayers().isEmpty()) {
            close(server);
            return;
        }
        if (dueSince == 0L) {
            dueSince = now;
            tellDungeon(server, Component.literal("The Deep Region's tides are turning. It rebuilds once the server is empty."));
        }
        if (now >= dueSince + (long) (FeaturesConfig.get().deepRebuildForceHours * HOUR_MILLIS)) {
            startCountdown(server, FeaturesConfig.get().deepRebuildCountdownSeconds);
        }
    }

    private static void startCountdown(MinecraftServer server, int seconds) {
        state = State.COUNTDOWN;
        countdownEnd = System.currentTimeMillis() + seconds * 1000L;
        lastWarning = Integer.MAX_VALUE;
        tellDungeon(server, Component.literal("The Deep Region rebuilds in " + seconds + " seconds. Everyone still inside will be sent to the surface."));
    }

    private static void tickCountdown(MinecraftServer server, long now) {
        if (server.getPlayerList().getPlayers().isEmpty() || now >= countdownEnd) {
            close(server);
            return;
        }
        int left = (int) Math.ceil((countdownEnd - now) / 1000.0);
        for (int mark : WARN_AT) {
            if (left <= mark && mark < lastWarning) {
                lastWarning = mark;
                warn(server, mark);
                break;
            }
        }
    }

    private static void warn(MinecraftServer server, int seconds) {
        Component text = Component.literal("The Deep Region rebuilds in " + seconds + (seconds == 1 ? " second" : " seconds"));
        for (ServerPlayer player : dungeonPlayers(server)) {
            if (seconds <= TITLE_FROM) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf(seconds))));
            }
            player.sendSystemMessage(text);
        }
    }

    private static void close(MinecraftServer server) {
        state = State.CLOSED;
        closedAt = System.currentTimeMillis();
        for (ServerPlayer player : dungeonPlayers(server)) {
            evacuate(player);
            player.sendSystemMessage(CLOSED);
        }
        LOG.info("Closed the dungeon to rebuild the Deep Region");
    }

    private static void tickClosed(MinecraftServer server, long now) {
        for (ServerPlayer player : dungeonPlayers(server)) {
            evacuate(player);
            player.sendSystemMessage(CLOSED);
        }
        ServerLevel dungeon = server.getLevel(DungeonDimension.LEVEL);
        int loaded = dungeon == null ? 0 : dungeon.getChunkSource().getLoadedChunksCount();
        if (loaded > 0 && now < closedAt + FeaturesConfig.get().deepRebuildUnloadSeconds * 1000L) {
            return;
        }
        LongOpenHashSet stillLoaded = new LongOpenHashSet();
        if (dungeon != null) {
            stillLoaded.addAll(dungeon.getForceLoadedChunks());
        }
        if (loaded > 0) {
            LOG.warn("Rebuilding the Deep Region with {} dungeon chunks still loaded; they rebuild after they next unload", loaded);
        }
        rebuild(server, dungeon, stillLoaded);
    }

    private static void rebuild(MinecraftServer server, ServerLevel dungeon, LongOpenHashSet stillLoaded) {
        DungeonGeneration.advance(server, stillLoaded);
        LabyrinthCells.reset();
        CavernGrid.reset();
        if (dungeon != null) {
            ((ClearableStructureCheck) ((ServerLevelStructureCheckAccessor) dungeon).mmsDungeons$structureCheck()).mmsDungeons$clear();
        }
        state = State.OPEN;
        dueSince = 0L;
        LOG.info("Rebuilt the Deep Region as generation {}", DungeonGeneration.current());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("The Deep Region has rebuilt. The dungeon is open again."));
        }
    }

    private static List<ServerPlayer> dungeonPlayers(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().filter(player -> DungeonDimension.isDungeon(player.level())).toList();
    }

    private static void tellDungeon(MinecraftServer server, Component text) {
        dungeonPlayers(server).forEach(player -> player.sendSystemMessage(text));
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dungeonrebuild")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("now").executes(ctx -> {
                close(ctx.getSource().getServer());
                ctx.getSource().sendSuccess(() -> Component.literal("Closing the dungeon to rebuild the Deep Region."), true);
                return 1;
            }))
            .then(Commands.literal("countdown")
                .executes(ctx -> countdown(ctx.getSource(), FeaturesConfig.get().deepRebuildCountdownSeconds))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                    .executes(ctx -> countdown(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds")))))
            .then(Commands.literal("status").executes(ctx -> {
                MinecraftServer server = ctx.getSource().getServer();
                long hours = (DungeonGeneration.lastRebuild(server) + (long) (FeaturesConfig.get().deepRebuildDays * DAY_MILLIS)
                    - System.currentTimeMillis()) / HOUR_MILLIS;
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Deep Region generation " + DungeonGeneration.current() + ", " + state.name().toLowerCase() + ", next rebuild due in " + hours + " hours."
                ), false);
                return 1;
            })));
    }

    private static int countdown(CommandSourceStack source, int seconds) {
        if (state != State.OPEN) {
            source.sendFailure(Component.literal("A Deep Region rebuild is already under way."));
            return 0;
        }
        startCountdown(source.getServer(), seconds);
        source.sendSuccess(() -> Component.literal("Deep Region rebuild countdown started."), true);
        return 1;
    }
}
