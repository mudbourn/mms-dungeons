package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmsdungeons.config.GuildsConfig;
import info.mudbourn.mmsdungeons.net.BountyActionPayload;
import info.mudbourn.mmsdungeons.net.BoardNoticePayload;
import info.mudbourn.mmsdungeons.net.BountyListPayload;
import info.mudbourn.mmsdungeons.net.OpenBoardPayload;
import info.mudbourn.mmsdungeons.net.PostBountyPayload;
import info.mudbourn.mmsdungeons.net.RequestBountiesPayload;
import info.mudbourn.mmseconomy.economy.Billing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// The packets between the quest board screens and the quest and contract rules.
final class GuildNetworking {

    private record Board(UUID guild, String dimension, BlockPos pos) {
    }

    private static final Map<UUID, Board> OPEN = new HashMap<>();

    private GuildNetworking() {
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenBoardPayload.TYPE, OpenBoardPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BountyListPayload.TYPE, BountyListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BoardNoticePayload.TYPE, BoardNoticePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestBountiesPayload.TYPE, RequestBountiesPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PostBountyPayload.TYPE, PostBountyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BountyActionPayload.TYPE, BountyActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestBountiesPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (payload.guildId().isPresent()) {
                Guild guild = boardGuild(player);
                if (guild == null || !guild.id.equals(payload.guildId().get())) {
                    return;
                }
            }
            sendList(player, payload.guildId());
        });
        ServerPlayNetworking.registerGlobalReceiver(PostBountyPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            Guild guild = boardGuild(player);
            String error = guild == null ? "Stand at a quest board." : BountyService.post(player, guild, payload);
            finish(player, guild, error, "Posted.");
        });
        ServerPlayNetworking.registerGlobalReceiver(BountyActionPayload.TYPE, (payload, context) ->
            act(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OPEN.remove(handler.player.getUUID()));
    }

    static void forgetAll() {
        OPEN.clear();
    }

    // Opens the board screen and starts tracking which board the player stands at.
    static void open(ServerPlayer player, Guild guild, BlockPos pos) {
        OPEN.put(player.getUUID(), new Board(guild.id, player.level().dimension().identifier().toString(), pos));
        boolean manager = guild.master != null && guild.master.equals(player.getUUID())
            || guild.isPublic && Guilds.isAdmin(player);
        ServerPlayNetworking.send(player, new OpenBoardPayload(guild.id, guild.name, manager));
        sendList(player, Optional.of(guild.id));
    }

    private static void act(ServerPlayer player, BountyActionPayload payload) {
        boolean atBoard = payload.action() == BountyActionPayload.Action.TAKE
            || payload.action() == BountyActionPayload.Action.HAND_IN;
        Guild guild = boardGuild(player);
        Bounty target = Guilds.data().bounty(payload.bounty());
        Guild scope = target == null ? guild : Guilds.data().byId(target.guild);
        String error;
        String done;
        if (atBoard && guild == null) {
            error = "Stand at a quest board.";
            done = "";
        } else {
            Bounty bounty = Guilds.data().bounty(payload.bounty());
            if (atBoard && (bounty == null || !bounty.guild.equals(guild.id))) {
                error = "That posting is not on this board.";
                done = "";
            } else {
                switch (payload.action()) {
                    case TAKE -> {
                        error = BountyService.take(player, payload.bounty());
                        done = "Taken for your party.";
                    }
                    case HAND_IN -> {
                        error = BountyService.handIn(player, payload.bounty());
                        done = "Handed in.";
                    }
                    case ABANDON -> {
                        error = BountyService.abandon(player, payload.bounty());
                        done = "Abandoned.";
                    }
                    default -> {
                        error = BountyService.delete(player, payload.bounty());
                        done = "Deleted.";
                    }
                }
            }
        }
        finish(player, scope, error, done);
    }

    // Tells the player the outcome and refreshes everyone looking at the same guild's board.
    private static void finish(ServerPlayer player, Guild guild, String error, String done) {
        ServerPlayNetworking.send(player, new BoardNoticePayload(error != null ? error : done, error != null));
        if (error != null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        for (Map.Entry<UUID, Board> entry : new ArrayList<>(OPEN.entrySet())) {
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
            if (viewer != null && viewer != player && guild != null && entry.getValue().guild().equals(guild.id)) {
                sendList(viewer, Optional.of(guild.id));
            }
        }
    }

    // The guild of the board the player stands at, or null when they are too far or none is open.
    private static Guild boardGuild(ServerPlayer player) {
        Board board = OPEN.get(player.getUUID());
        if (board == null || !board.dimension().equals(player.level().dimension().identifier().toString())) {
            return null;
        }
        double reach = GuildsConfig.get().boardReach;
        BlockPos pos = board.pos();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > reach * reach) {
            return null;
        }
        if (!(player.level().getBlockEntity(pos) instanceof QuestBoardBlockEntity entity)
            || !board.guild().equals(entity.guild())) {
            return null;
        }
        return Guilds.data().byId(board.guild());
    }

    private static void sendList(ServerPlayer player, Optional<UUID> guildId) {
        GuildData data = Guilds.data();
        long today = Billing.today(player.level().getServer());
        List<Bounty> chosen = new ArrayList<>();
        if (guildId.isPresent()) {
            if (data.byId(guildId.get()) == null) {
                return;
            }
            chosen.addAll(data.bountiesOf(guildId.get()));
        } else {
            for (Bounty bounty : data.bounties) {
                boolean posted = bounty.poster.equals(player.getUUID());
                boolean held = bounty.status == BountyStatus.IN_PROGRESS
                    && Guilds.partyMembers(bounty.taker).contains(player.getUUID());
                if (posted || held) {
                    chosen.add(bounty);
                }
            }
        }
        List<BountyView> views = new ArrayList<>();
        for (Bounty bounty : chosen) {
            views.add(BountyService.view(player, data, bounty, today));
        }
        ServerPlayNetworking.send(player, new BountyListPayload(guildId, views));
    }
}
