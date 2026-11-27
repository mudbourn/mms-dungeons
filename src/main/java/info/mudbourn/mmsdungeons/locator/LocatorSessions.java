package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

// Per-player runtime state of a switched-on locator: search, static and pings.
final class LocatorSessions {

    private static final int SEARCH_SOUND_TICKS = 172;
    private static final int GIVE_UP_TICKS = 1200;
    private static final int PING_INTERVAL = 80;
    // A signal at most one block above or below reads as level with the player.
    private static final int LEVEL_BAND = 2;
    // Length of the static loop, 4.69 seconds.
    private static final int STATIC_TICKS = 94;
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private enum Phase {
        SEARCHING,
        LOCKED,
        // Switched on outside the dungeon: plays static and waits for the dungeon.
        STATIC
    }

    private static final class Session {
        Phase phase;
        List<LocatorTarget> targets = List.of();
        LocatorTarget target;
        int ticks;
        int sincePing;
        CompletableFuture<LocatorSearch.Hit> search;
        BlockPos found;
    }

    private LocatorSessions() {
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LocatorSessions::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
    }

    static boolean has(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static void startSearch(ServerPlayer player, ServerLevel level, List<LocatorTarget> targets) {
        Session session = new Session();
        session.phase = Phase.SEARCHING;
        session.targets = targets;
        BlockPos origin = player.blockPosition();
        session.search = CompletableFuture.supplyAsync(() -> LocatorSearch.find(level, targets, origin), Util.backgroundExecutor());
        SESSIONS.put(player.getUUID(), session);
        LocatorSounds.play(player, LocatorSounds.SEARCHING);
    }

    static void startStatic(ServerPlayer player, List<LocatorTarget> targets) {
        Session session = new Session();
        session.phase = Phase.STATIC;
        session.targets = targets;
        session.ticks = STATIC_TICKS;
        SESSIONS.put(player.getUUID(), session);
    }

    static void resumeLocked(ServerPlayer player, LocatorTarget target, BlockPos found) {
        Session session = new Session();
        session.phase = Phase.LOCKED;
        session.target = target;
        session.found = found;
        SESSIONS.put(player.getUUID(), session);
    }

    static void stop(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            silence(player, session);
        }
    }

    private static void silence(ServerPlayer player, Session session) {
        if (session.phase == Phase.SEARCHING) {
            LocatorSounds.stop(player, LocatorSounds.SEARCHING);
        } else if (session.phase == Phase.STATIC) {
            LocatorSounds.stop(player, LocatorSounds.STATIC);
        }
    }

    private static final List<ServerPlayer> startSearchLater = new ArrayList<>();

    private static void tick(MinecraftServer server) {
        tickSessions(server);
        for (ServerPlayer player : startSearchLater) {
            InteractionHand hand = LocatorItem.activeHand(player);
            List<LocatorTarget> targets = hand == null ? List.of() : LocatorItem.focused(player.getItemInHand(hand));
            if (!targets.isEmpty() && player.level() instanceof ServerLevel level) {
                startSearch(player, level, targets);
            }
        }
        startSearchLater.clear();
    }

    private static void tickSessions(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            Session session = entry.getValue();
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }
            InteractionHand hand = LocatorItem.activeHand(player);
            boolean dungeon = level.dimension() == DungeonDimension.LEVEL;
            if (!dungeon && session.phase != Phase.STATIC) {
                // Left the dungeon while on: the lock is lost and the device falls to static.
                silence(player, session);
                LocatorItem.clearLocks(player);
                session.phase = Phase.STATIC;
                session.ticks = STATIC_TICKS;
                session.search = null;
                session.found = null;
            }
            if (session.phase == Phase.STATIC) {
                if (dungeon) {
                    // Back in range: the static gives way to a fresh search.
                    LocatorSounds.stop(player, LocatorSounds.STATIC);
                    iterator.remove();
                    if (hand != null && !session.targets.isEmpty()) {
                        startSearchLater.add(player);
                    } else {
                        LocatorItem.turnOffAll(player);
                    }
                } else {
                    tickStatic(player, session, hand);
                }
                continue;
            }
            if (session.phase == Phase.SEARCHING) {
                if (hand == null) {
                    iterator.remove();
                    LocatorSounds.stop(player, LocatorSounds.SEARCHING);
                } else if (tickSearching(player, level, session, hand)) {
                    iterator.remove();
                }
            } else if (hand != null) {
                tickLocked(player, session);
            }
        }
    }

    private static boolean tickSearching(ServerPlayer player, ServerLevel level, Session session, InteractionHand hand) {
        session.ticks++;
        // The search never generates chunks: forcing them from the server thread stalls it for seconds per chunk.
        boolean ready = false;
        if (session.search != null && session.search.isDone()) {
            LocatorSearch.Hit hit = session.search.isCompletedExceptionally() ? null : session.search.getNow(null);
            session.found = hit == null ? null : hit.pos();
            session.target = hit == null ? null : hit.target();
            session.search = null;
        }
        if (session.search == null) {
            ready = true;
        }
        if (session.ticks < SEARCH_SOUND_TICKS || !(ready || session.ticks >= GIVE_UP_TICKS)) {
            return false;
        }
        LocatorSounds.stop(player, LocatorSounds.SEARCHING);
        ItemStack stack = player.getItemInHand(hand);
        if (session.found == null) {
            LocatorItem.setOn(stack, false);
            player.sendSystemMessage(Component.translatable("locator.mms_dungeons.out_of_range"), true);
            return true;
        }
        LocatorItem.setFound(stack, session.target, session.found);
        LocatorSounds.play(player, LocatorSounds.CONNECT);
        player.sendSystemMessage(Component.translatable("locator.mms_dungeons.locked", session.target.label()), true);
        stack.hurtAndBreak(1, player, hand);
        session.phase = Phase.LOCKED;
        session.sincePing = 0;
        session.search = null;
        return false;
    }

    // Replays the static loop while the device is held, and lets it fall silent while it is put away.
    private static void tickStatic(ServerPlayer player, Session session, InteractionHand hand) {
        if (hand == null) {
            if (session.ticks < STATIC_TICKS) {
                LocatorSounds.stop(player, LocatorSounds.STATIC);
                session.ticks = STATIC_TICKS;
            }
            return;
        }
        if (session.ticks >= STATIC_TICKS) {
            session.ticks = 0;
            LocatorSounds.play(player, LocatorSounds.STATIC);
        }
        session.ticks++;
    }

    private static void tickLocked(ServerPlayer player, Session session) {
        session.sincePing++;
        if (session.sincePing < PING_INTERVAL) {
            return;
        }
        session.sincePing = 0;
        double dx = session.found.getX() + 0.5 - player.getX();
        double dz = session.found.getZ() + 0.5 - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        int relative = Math.floorMod((int) Math.round(Mth.wrapDegrees(bearing - player.getYRot()) / 45.0), 8);
        int cardinal = Math.floorMod((int) Math.round(Mth.wrapDegrees(bearing + 180.0) / 45.0), 8);
        int rise = (int) Math.round(session.found.getY() + 0.5 - player.getY());
        Component height = Math.abs(rise) < LEVEL_BAND
            ? Component.translatable("locator.mms_dungeons.level")
            : Component.translatable(rise > 0 ? "locator.mms_dungeons.up" : "locator.mms_dungeons.down", Math.abs(rise));
        LocatorSounds.play(player, LocatorSounds.PING);
        player.sendSystemMessage(
            Component.translatable(
                "locator.mms_dungeons.ping",
                session.target.label(),
                Component.translatable("locator.mms_dungeons.relative." + relative),
                CARDINALS[cardinal],
                distance,
                height),
            true);
    }
}
