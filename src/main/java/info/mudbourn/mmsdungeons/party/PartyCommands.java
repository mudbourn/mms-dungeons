package info.mudbourn.mmsdungeons.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// The /party command: invite, accept, leave, kick and list.
final class PartyCommands {

    private record Invite(UUID inviter, long expiresAt) {
    }

    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private PartyCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> invite(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("accept").executes(PartyCommands::accept))
            .then(Commands.literal("leave").executes(PartyCommands::leave))
            .then(Commands.literal("kick")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(memberNames(ctx), builder))
                    .executes(ctx -> kick(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list").executes(PartyCommands::list)));
    }

    private static Iterable<String> memberNames(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        PartyData data = Parties.data();
        if (player == null || data == null || data.partyOf(player.getUUID()) == null) {
            return java.util.List.of();
        }
        PartyData.Party party = data.partyOf(player.getUUID());
        return party.members.stream().map(data::nameOf).toList();
    }

    private static int invite(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyData data = Parties.data();
        if (target == player) {
            return fail(ctx, "You cannot invite yourself.");
        }
        PartyData.Party party = data.partyOf(player.getUUID());
        if (party != null && !party.leader.equals(player.getUUID())) {
            return fail(ctx, "Only the party leader can invite.");
        }
        if (party != null && party.isFull()) {
            return fail(ctx, "Your party is full.");
        }
        if (data.partyOf(target.getUUID()) != null) {
            return fail(ctx, target.getGameProfile().name() + " is already in a party.");
        }
        long expiresAt = player.level().getGameTime() + FeaturesConfig.get().inviteSeconds * 20L;
        INVITES.put(target.getUUID(), new Invite(player.getUUID(), expiresAt));
        player.sendSystemMessage(Component.literal("Invited " + target.getGameProfile().name() + " to your party."));
        target.sendSystemMessage(Component.literal(player.getGameProfile().name() + " invited you to a party. Type /party accept."));
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyData data = Parties.data();
        MinecraftServer server = Parties.serverOf(player);
        Invite invite = INVITES.remove(player.getUUID());
        if (invite == null || invite.expiresAt < player.level().getGameTime()) {
            return fail(ctx, "You have no party invite.");
        }
        if (data.partyOf(player.getUUID()) != null) {
            return fail(ctx, "Leave your party first.");
        }
        PartyData.Party party = data.partyOf(invite.inviter);
        if (party == null) {
            if (server.getPlayerList().getPlayer(invite.inviter) == null) {
                return fail(ctx, "The inviter is no longer online.");
            }
            party = data.create(invite.inviter);
        }
        if (party.isFull()) {
            return fail(ctx, "That party is full.");
        }
        data.add(party, player.getUUID());
        broadcast(server, party, player.getGameProfile().name() + " joined the party.");
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyData data = Parties.data();
        PartyData.Party party = data.partyOf(player.getUUID());
        if (party == null) {
            return fail(ctx, "You are not in a party.");
        }
        data.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("You left the party."));
        broadcast(Parties.serverOf(player), party, player.getGameProfile().name() + " left the party.");
        return 1;
    }

    private static int kick(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyData data = Parties.data();
        PartyData.Party party = data.partyOf(player.getUUID());
        if (party == null) {
            return fail(ctx, "You are not in a party.");
        }
        if (!party.leader.equals(player.getUUID())) {
            return fail(ctx, "Only the party leader can kick.");
        }
        UUID target = data.findByName(party, name);
        if (target == null) {
            return fail(ctx, name + " is not in your party.");
        }
        if (target.equals(player.getUUID())) {
            return fail(ctx, "Use /party leave to leave.");
        }
        MinecraftServer server = Parties.serverOf(player);
        ServerPlayer online = server.getPlayerList().getPlayer(target);
        data.remove(target);
        if (online != null) {
            online.sendSystemMessage(Component.literal("You were removed from the party."));
        }
        broadcast(server, party, name + " was removed from the party.");
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyData data = Parties.data();
        PartyData.Party party = data.partyOf(player.getUUID());
        if (party == null) {
            return fail(ctx, "You are not in a party.");
        }
        MinecraftServer server = Parties.serverOf(player);
        player.sendSystemMessage(Component.literal("Party (" + party.members.size() + "/" + PartyData.MAX_SIZE + "):"));
        for (UUID id : party.members) {
            String tags = (id.equals(party.leader) ? " (leader)" : "")
                + (server.getPlayerList().getPlayer(id) == null ? " (offline)" : "");
            player.sendSystemMessage(Component.literal("- " + data.nameOf(id) + tags));
        }
        return 1;
    }

    private static void broadcast(MinecraftServer server, PartyData.Party party, String message) {
        for (UUID id : party.members) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) {
                member.sendSystemMessage(Component.literal(message));
            }
        }
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
