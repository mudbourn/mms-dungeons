package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmscombat.combatlog.CombatManager;
import info.mudbourn.mmscombat.zone.Zone;
import info.mudbourn.mmscombat.zone.ZoneStore;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Periculum Region rules: always-on PvP through an mms-combat band zone, no logout body, and a two-day ban for logging out inside.
public final class PericulumRules extends SavedData {

    public static final String ZONE_NAME = "mms_dungeons_periculum";
    // Two Minecraft days.
    private static final long BAN_TICKS = 48000L;
    private static final int CHECK_INTERVAL = 20;

    public static final Codec<PericulumRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).fieldOf("bans").forGetter(rules -> rules.bans),
        UUIDUtil.STRING_CODEC.listOf().fieldOf("relocate").forGetter(rules -> List.copyOf(rules.relocate))
    ).apply(instance, PericulumRules::new));

    public static final SavedDataType<PericulumRules> TYPE = new SavedDataType<>(
        MmsDungeons.id("periculum_rules"),
        PericulumRules::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    private final Map<UUID, Long> bans;
    private final Set<UUID> relocate;

    public PericulumRules() {
        this.bans = new HashMap<>();
        this.relocate = new HashSet<>();
    }

    private PericulumRules(Map<UUID, Long> bans, List<UUID> relocate) {
        this.bans = new HashMap<>(bans);
        this.relocate = new HashSet<>(relocate);
    }

    public static PericulumRules get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static void register() {
        CombatManager.get().addLogoutBodyVeto(player -> DungeonDimension.isPericulum(player.level(), player.blockPosition()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ensureZone());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.player));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(PericulumRules::tick);
    }

    // Creates the band zone that keeps PvP on below Y 0 in the dungeon, with natural spawns left on.
    private static void ensureZone() {
        if (ZoneStore.byName(ZONE_NAME) != null) {
            return;
        }
        Zone zone = Zone.band(
            ZONE_NAME,
            DungeonDimension.LEVEL.identifier().toString(),
            -128,
            DungeonDimension.PERICULUM_TOP
        );
        zone.suppressNaturalSpawns = false;
        ZoneStore.add(zone);
    }

    private static void onDisconnect(ServerPlayer player) {
        if (DungeonDimension.isPericulum(player.level(), player.blockPosition())) {
            PericulumRules rules = get(player.level().getServer());
            rules.relocate.add(player.getUUID());
            rules.setDirty();
        }
    }

    private static void onJoin(ServerPlayer player) {
        PericulumRules rules = get(player.level().getServer());
        if (!rules.relocate.remove(player.getUUID())) {
            return;
        }
        long until = player.level().getServer().overworld().getGameTime() + BAN_TICKS;
        rules.bans.put(player.getUUID(), until);
        rules.setDirty();
        sendHome(player, "You left the Periculum Region mid-fight. It is closed to you for two days.");
    }

    // Sends banned players back out if they cross below Y 0.
    private static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % CHECK_INTERVAL != 0) {
            return;
        }
        PericulumRules rules = get(server);
        if (rules.bans.values().removeIf(until -> until <= now)) {
            rules.setDirty();
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean banned = rules.bans.containsKey(player.getUUID());
            if (banned && DungeonDimension.isPericulum(player.level(), player.blockPosition())) {
                sendHome(player, "The Periculum Region is closed to you for now.");
            }
        }
    }

    private static void sendHome(ServerPlayer player, String message) {
        player.teleport(TeleportTransition.createDefault(player, TeleportTransition.DO_NOTHING));
        player.sendSystemMessage(Component.literal(message));
    }
}
