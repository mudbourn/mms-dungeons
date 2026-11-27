package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmscombat.combatlog.CombatManager;
import info.mudbourn.mmsdungeons.config.GuildsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import xaero.pac.common.claims.action.api.ClaimingAction;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.action.listener.api.IClaimActionListenerAPI;
import xaero.pac.common.server.claims.action.listener.override.api.ClaimActionPermissionOverride;
import xaero.pac.common.server.claims.action.listener.override.api.ClaimActionPermissionOverrideType;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

// Holds a guild's land in the guildmaster's own claim sub-config, apart from their personal claims.
final class GuildClaims {

    private static final String SUB_ID = "guild";
    private static final int SEARCH_RADIUS = 64;

    private record Chunk(Identifier dimension, int x, int z, boolean forceload) {
    }

    private GuildClaims() {
    }

    static void register() {
        CombatManager.get().addPvpOverride(GuildClaims::banishedOnGuildLand);
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            claims(server).getActionListenerManager().register(new LimitListener()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GuildData data = Guilds.data();
            if (data == null) {
                return;
            }
            UUID id = handler.player.getUUID();
            for (Guild guild : data.guilds) {
                if (guild.moveOut.remove(id)) {
                    data.setDirty();
                    moveOut(handler.player, guild);
                }
            }
        });
    }

    private static IServerClaimsManagerAPI claims(MinecraftServer server) {
        return OpenPACServerAPI.get(server).getServerClaimsManager();
    }

    private static IPlayerConfigAPI config(MinecraftServer server, UUID player) {
        return OpenPACServerAPI.get(server).getPlayerConfigs().getLoadedConfig(player);
    }

    // Creates the guild sub-config, grants the bonus claims and keeps personal land closed to the guild party. Returns a problem or null.
    static String setUp(MinecraftServer server, Guild guild, ServerPlayer master) {
        IPlayerConfigAPI config = config(server, master.getUUID());
        if (config == null) {
            return "Your claim settings are not loaded yet. Try again in a moment.";
        }
        IPlayerConfigAPI sub;
        if (config.subConfigExists(SUB_ID)) {
            sub = config.getSubConfig(SUB_ID);
            if (sub == null || count(server, master.getUUID(), sub.getSubIndex()) > 0) {
                return "Your claim sub-config named " + SUB_ID + " already holds claims. Unclaim them first.";
            }
        } else {
            sub = config.createSubConfig(SUB_ID);
            if (sub == null) {
                return "You have no room for another claim sub-config.";
            }
        }
        guild.claimSub = sub.getSubIndex();
        sub.tryToSet(PlayerConfigOptions.CLAIMS_NAME, guild.name);
        sub.tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS_FROM_PARTY, false);
        config.tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS_FROM_PARTY, true);
        addBonus(config, GuildsConfig.get().guildClaims);
        return null;
    }

    private static void addBonus(IPlayerConfigAPI config, int amount) {
        Integer current = config.getRaw(PlayerConfigOptions.BONUS_CHUNK_CLAIMS);
        config.tryToSet(PlayerConfigOptions.BONUS_CHUNK_CLAIMS, Math.max(0, (current == null ? 0 : current) + amount));
    }

    // Moves every guild claim to the new guildmaster and sets them up with their own guild sub-config. Returns a problem or null.
    static String transfer(MinecraftServer server, Guild guild, UUID oldMaster, ServerPlayer newMaster) {
        int oldIndex = guild.claimSub;
        List<Chunk> chunks = guildChunks(server, oldMaster, oldIndex);
        String problem = setUp(server, guild, newMaster);
        if (problem != null) {
            guild.claimSub = oldIndex;
            return problem;
        }
        IServerClaimsManagerAPI claims = claims(server);
        for (Chunk chunk : chunks) {
            claims.unclaim(chunk.dimension(), chunk.x(), chunk.z());
            claims.claim(chunk.dimension(), newMaster.getUUID(), guild.claimSub, chunk.x(), chunk.z(), chunk.forceload());
        }
        IPlayerConfigAPI old = config(server, oldMaster);
        if (old != null) {
            addBonus(old, -GuildsConfig.get().guildClaims);
        }
        return null;
    }

    private static List<Chunk> guildChunks(MinecraftServer server, UUID master, int index) {
        List<Chunk> chunks = new ArrayList<>();
        IServerPlayerClaimInfoAPI info = claims(server).getPlayerInfo(master);
        if (info == null || index < 0) {
            return chunks;
        }
        info.getStream().forEach(dimension -> dimension.getValue().getStream().forEach(list -> {
            if (list.getClaimState().getSubConfigIndex() == index) {
                list.getStream().forEach(chunk -> chunks.add(new Chunk(
                    dimension.getKey(),
                    chunk.x(),
                    chunk.z(),
                    list.getClaimState().isForceloadable()
                )));
            }
        }));
        return chunks;
    }

    private static int count(MinecraftServer server, UUID master, int index) {
        return guildChunks(server, master, index).size();
    }

    // The guild whose land holds this block, or null.
    static Guild guildAt(MinecraftServer server, ServerLevel level, BlockPos pos) {
        GuildData data = Guilds.data();
        if (data == null) {
            return null;
        }
        IPlayerChunkClaimAPI claim = claims(server).get(level.dimension().identifier(), pos);
        if (claim == null) {
            return null;
        }
        for (Guild guild : data.guilds) {
            if (guild.claimSub >= 0 && guild.claimSub == claim.getSubConfigIndex() && claim.getPlayerId().equals(guild.master)) {
                return guild;
            }
        }
        return null;
    }

    private static boolean onLandOf(MinecraftServer server, ServerLevel level, BlockPos pos, Guild guild) {
        return guildAt(server, level, pos) == guild;
    }

    // Moves a player standing on the guild's land to the nearest chunk outside it.
    static void moveOut(ServerPlayer player, Guild guild) {
        MinecraftServer server = player.level().getServer();
        ServerLevel level = (ServerLevel) player.level();
        if (!onLandOf(server, level, player.blockPosition(), guild)) {
            return;
        }
        ChunkPos origin = player.chunkPosition();
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    ChunkPos chunk = new ChunkPos(origin.x() + dx, origin.z() + dz);
                    if (!onLandOf(server, level, chunk.getMiddleBlockPosition(0), guild)) {
                        level.getChunk(chunk.x(), chunk.z());
                        int x = chunk.getMiddleBlockX();
                        int z = chunk.getMiddleBlockZ();
                        player.teleportTo(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
                        player.sendSystemMessage(Component.literal("You were moved off the land of " + guild.name + "."));
                        return;
                    }
                }
            }
        }
    }

    private static boolean banishedOnGuildLand(ServerPlayer player) {
        GuildData data = Guilds.data();
        if (data == null || data.guilds.stream().noneMatch(guild -> guild.banished.contains(player.getUUID()))) {
            return false;
        }
        Guild land = guildAt(player.level().getServer(), (ServerLevel) player.level(), player.blockPosition());
        return land != null && land.banished.contains(player.getUUID());
    }

    private static final class LimitListener implements IClaimActionListenerAPI {

        @Override
        public String getName() {
            return "mms_dungeons_guild_claims";
        }

        @Override
        public ClaimActionPermissionOverride overrideClaimingActionPermission(
            UUID player,
            Identifier dimension,
            int x,
            int z,
            ClaimingAction action,
            IServerClaimsManagerAPI manager,
            ClaimActionPermissionOverride current,
            MinecraftServer server
        ) {
            ClaimActionPermissionOverride pass = new ClaimActionPermissionOverride(ClaimActionPermissionOverrideType.PASS, Component.empty());
            GuildData data = Guilds.data();
            if (action != ClaimingAction.CLAIM || data == null) {
                return pass;
            }
            Guild guild = data.guilds.stream().filter(candidate -> player.equals(candidate.master) && candidate.claimSub >= 0).findFirst().orElse(null);
            IPlayerConfigAPI config = guild == null ? null : config(server, player);
            if (config == null) {
                return pass;
            }
            int limit = GuildsConfig.get().guildClaims;
            int guildCount = count(server, player, guild.claimSub);
            if (config.getUsedSubConfig().getSubIndex() == guild.claimSub) {
                return guildCount >= limit ? forbid("Guild land is limited to " + limit + " chunks.") : pass;
            }
            int personalCount = manager.getPlayerInfo(player).getClaimCount() - guildCount;
            if (personalCount >= manager.getPlayerFullClaimLimit(player) - limit) {
                return forbid("The guild's " + limit + " chunks cannot be spent on personal land.");
            }
            return pass;
        }

        @Override
        public void handleSuccessfulClaimingAction(
            UUID player,
            Identifier dimension,
            int x,
            int z,
            ClaimingAction action,
            IServerClaimsManagerAPI manager,
            MinecraftServer server
        ) {
        }

        private static ClaimActionPermissionOverride forbid(String reason) {
            return new ClaimActionPermissionOverride(ClaimActionPermissionOverrideType.FORBID, Component.literal(reason));
        }
    }
}
