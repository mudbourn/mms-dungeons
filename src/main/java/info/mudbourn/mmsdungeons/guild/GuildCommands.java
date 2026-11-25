package info.mudbourn.mmsdungeons.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import info.mudbourn.mmsdungeons.config.GuildsConfig;
import info.mudbourn.mmseconomy.deal.Spot;
import info.mudbourn.mmseconomy.economy.Billing;
import info.mudbourn.mmseconomy.economy.Currency;
import info.mudbourn.mmseconomy.economy.Tax;
import info.mudbourn.mmseconomy.economy.TaxKind;
import info.mudbourn.mmseconomy.economy.Wallet;
import info.mudbourn.mmseconomy.history.History;
import info.mudbourn.mmseconomy.history.HistoryCategory;
import info.mudbourn.mmseconomy.history.HistoryEntry;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// The /guild command: found, join, leave, manage and inspect guilds.
final class GuildCommands {

    private static final double LOOK_REACH = 6.0;

    private GuildCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guild")
            .then(Commands.literal("found")
                .then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> found(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(guildNames(), builder))
                    .executes(ctx -> join(ctx, StringArgumentType.getString(ctx, "name"), false))
                    .then(Commands.literal("confirm").executes(ctx -> join(ctx, StringArgumentType.getString(ctx, "name"), true)))))
            .then(Commands.literal("leave").executes(GuildCommands::leave))
            .then(Commands.literal("kick")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(memberNames(ctx), builder))
                    .executes(ctx -> kick(ctx, StringArgumentType.getString(ctx, "player")))))
            .then(Commands.literal("charter")
                .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> charter(ctx, StringArgumentType.getString(ctx, "text")))))
            .then(Commands.literal("fee")
                .then(Commands.argument("amount", StringArgumentType.word()).executes(ctx -> fee(ctx, StringArgumentType.getString(ctx, "amount")))))
            .then(Commands.literal("commission")
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 50)).executes(ctx -> commission(ctx, IntegerArgumentType.getInteger(ctx, "percent")))))
            .then(Commands.literal("transfer")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(memberNames(ctx), builder))
                    .executes(ctx -> transfer(ctx, StringArgumentType.getString(ctx, "player")))))
            .then(Commands.literal("withdraw")
                .then(Commands.argument("amount", StringArgumentType.word()).executes(ctx -> withdraw(ctx, StringArgumentType.getString(ctx, "amount")))))
            .then(Commands.literal("depot").executes(ctx -> depot(ctx, null)))
            .then(Commands.literal("bind").executes(ctx -> bind(ctx, null)))
            .then(Commands.literal("info")
                .executes(ctx -> info(ctx, null))
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(guildNames(), builder))
                    .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list").executes(GuildCommands::list))
            .then(Commands.literal("admin")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> adminCreate(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("portal")
                    .then(Commands.argument("guild", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(guildNames(), builder))
                        .then(Commands.argument("installed", BoolArgumentType.bool())
                            .executes(ctx -> portal(ctx, StringArgumentType.getString(ctx, "guild"), BoolArgumentType.getBool(ctx, "installed"))))))
                .then(Commands.literal("bind")
                    .then(Commands.argument("guild", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(guildNames(), builder))
                        .executes(ctx -> bind(ctx, StringArgumentType.getString(ctx, "guild")))))
                .then(Commands.literal("depot")
                    .then(Commands.argument("guild", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(guildNames(), builder))
                        .executes(ctx -> depot(ctx, StringArgumentType.getString(ctx, "guild")))))));
    }

    private static Iterable<String> guildNames() {
        return Guilds.data().guilds.stream().map(guild -> guild.name).toList();
    }

    private static Iterable<String> memberNames(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        Guild guild = player == null ? null : Guilds.data().guildOf(player.getUUID());
        return guild == null ? java.util.List.of() : guild.members.stream().map(Guilds::nameOf).toList();
    }

    private static int found(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        GuildData data = Guilds.data();
        GuildsConfig config = GuildsConfig.get();
        String problem = checkName(data, name);
        if (problem != null) {
            return fail(ctx, problem);
        }
        if (data.guildOf(player.getUUID()) != null) {
            return fail(ctx, "You already belong to a guild.");
        }
        if (GuildOpac.inOtherParty(server, player.getUUID(), null)) {
            return fail(ctx, "Leave your Open Parties and Claims party first.");
        }
        Spot depot = Depots.ownedNearby(player);
        if (depot == null) {
            return fail(ctx, "Stand at a depot you own, or look at it, so it can become the guild depot.");
        }
        if (data.guilds.stream().anyMatch(guild -> depot.equals(guild.depot))) {
            return fail(ctx, "That depot already belongs to a guild.");
        }
        long cost = config.foundingCostPice;
        if (!Wallet.canAfford(player, cost)) {
            return fail(ctx, "Founding a guild costs " + Currency.format(cost) + ".");
        }
        UUID party = GuildOpac.create(server, player);
        if (party == null) {
            return fail(ctx, "The guild's Open Parties and Claims party could not be created.");
        }

        Guild guild = new Guild(UUID.randomUUID(), name, false, player.getUUID(), config.contractCommissionPercent);
        guild.depot = depot;
        guild.opacParty = party;
        if (cost > 0) {
            Tax.Settlement settlement = Tax.settleTaxedOffline(player, cost, TaxKind.PAY);
            guild.treasury += settlement.net();
            History.log(player, new HistoryEntry(Billing.today(server), HistoryCategory.PAYMENT, "", 0,
                settlement.gross(), settlement.tax(), -settlement.gross(), "Founding " + name));
        }
        data.guilds.add(guild);
        data.setDirty();
        return ok(ctx, "You founded " + name + ".");
    }

    private static int join(CommandContext<CommandSourceStack> ctx, String name, boolean confirm) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        GuildData data = Guilds.data();
        Guild guild = data.byName(name);
        if (guild == null) {
            return fail(ctx, "No guild is called " + name + ".");
        }
        if (data.guildOf(player.getUUID()) != null) {
            return fail(ctx, "You already belong to a guild. Leave it first.");
        }
        if (!confirm) {
            String charter = guild.charter.isEmpty() ? "(no charter set)" : guild.charter;
            ctx.getSource().sendSuccess(() -> Component.literal(
                guild.name + " charter: " + charter + "\nJoin fee: " + Currency.format(guild.joinFee)
                    + "\nType /guild join \"" + guild.name + "\" confirm to accept the charter and pay."), false);
            return 1;
        }
        if (GuildOpac.inOtherParty(server, player.getUUID(), guild.opacParty)) {
            return fail(ctx, "Leave your Open Parties and Claims party first.");
        }
        if (guild.joinFee > 0) {
            if (!Wallet.canAfford(player, guild.joinFee)) {
                return fail(ctx, "The join fee is " + Currency.format(guild.joinFee) + ".");
            }
            Tax.Settlement settlement = Tax.settleTaxedOffline(player, guild.joinFee, TaxKind.PAY);
            guild.treasury += settlement.net();
            History.log(player, new HistoryEntry(Billing.today(server), HistoryCategory.PAYMENT, "", 0,
                settlement.gross(), settlement.tax(), -settlement.gross(), "Joining " + guild.name));
        }
        guild.members.add(player.getUUID());
        GuildOpac.add(server, guild, player.getUUID(), player.getGameProfile().name());
        data.setDirty();
        return ok(ctx, "You joined " + guild.name + ".");
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        GuildData data = Guilds.data();
        Guild guild = data.guildOf(player.getUUID());
        if (guild == null) {
            return fail(ctx, "You are not in a guild.");
        }
        if (player.getUUID().equals(guild.master)) {
            return fail(ctx, "Transfer the guild to someone else before leaving.");
        }
        guild.members.remove(player.getUUID());
        GuildOpac.remove(ctx.getSource().getServer(), guild, player.getUUID());
        data.setDirty();
        return ok(ctx, "You left " + guild.name + ".");
    }

    private static int kick(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Guild guild = managed(player);
        if (guild == null) {
            return fail(ctx, "Only a guildmaster can do that.");
        }
        UUID target = memberByName(guild, name);
        if (target == null) {
            return fail(ctx, name + " is not in " + guild.name + ".");
        }
        if (target.equals(guild.master)) {
            return fail(ctx, "The guildmaster cannot be kicked.");
        }
        guild.members.remove(target);
        GuildOpac.remove(ctx.getSource().getServer(), guild, target);
        Guilds.data().setDirty();
        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayer(target);
        if (online != null) {
            online.sendSystemMessage(Component.literal("You were removed from " + guild.name + "."));
        }
        return ok(ctx, "Removed " + name + " from " + guild.name + ".");
    }

    private static int charter(CommandContext<CommandSourceStack> ctx, String text) throws CommandSyntaxException {
        Guild guild = managed(ctx.getSource().getPlayerOrException());
        if (guild == null) {
            return fail(ctx, "Only a guildmaster can do that.");
        }
        if (text.length() > GuildsConfig.get().maxCharterLength) {
            return fail(ctx, "The charter is limited to " + GuildsConfig.get().maxCharterLength + " characters.");
        }
        guild.charter = text;
        Guilds.data().setDirty();
        return ok(ctx, "Charter updated.");
    }

    private static int fee(CommandContext<CommandSourceStack> ctx, String amount) throws CommandSyntaxException {
        Guild guild = managed(ctx.getSource().getPlayerOrException());
        if (guild == null) {
            return fail(ctx, "Only a guildmaster can do that.");
        }
        long pice = Currency.parse(amount);
        if (pice < 0) {
            return fail(ctx, "Write the fee like 5 or 5.25.");
        }
        guild.joinFee = pice;
        Guilds.data().setDirty();
        return ok(ctx, "Join fee set to " + Currency.format(pice) + ".");
    }

    private static int commission(CommandContext<CommandSourceStack> ctx, int percent) throws CommandSyntaxException {
        Guild guild = managed(ctx.getSource().getPlayerOrException());
        if (guild == null) {
            return fail(ctx, "Only a guildmaster can do that.");
        }
        guild.commissionPercent = percent;
        Guilds.data().setDirty();
        return ok(ctx, "Contract commission set to " + percent + "%.");
    }

    private static int transfer(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        Guild guild = Guilds.data().guildOf(player.getUUID());
        if (guild == null || !player.getUUID().equals(guild.master)) {
            return fail(ctx, "Only the guildmaster can transfer the guild.");
        }
        UUID target = memberByName(guild, name);
        ServerPlayer next = target == null ? null : server.getPlayerList().getPlayer(target);
        if (next == null || target.equals(player.getUUID())) {
            return fail(ctx, "Name another guild member who is online.");
        }
        UUID party = GuildOpac.transfer(server, guild, next);
        if (party == null) {
            return fail(ctx, "The Open Parties and Claims party could not pass to " + name + ".");
        }
        guild.opacParty = party;
        guild.master = target;
        Guilds.data().setDirty();
        return ok(ctx, name + " is now the guildmaster of " + guild.name + ".");
    }

    private static int withdraw(CommandContext<CommandSourceStack> ctx, String amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Guild guild = managed(player);
        if (guild == null) {
            return fail(ctx, "Only a guildmaster can do that.");
        }
        long pice = Currency.parse(amount);
        if (pice <= 0 || pice > guild.treasury) {
            return fail(ctx, "The treasury holds " + Currency.format(guild.treasury) + ".");
        }
        guild.treasury -= pice;
        Wallet.deposit(player, pice);
        History.log(player, new HistoryEntry(Billing.today(ctx.getSource().getServer()), HistoryCategory.PAYMENT, "", 0,
            pice, 0L, pice, guild.name + " treasury"));
        Guilds.data().setDirty();
        return ok(ctx, "Withdrew " + Currency.format(pice) + ". The treasury holds " + Currency.format(guild.treasury) + ".");
    }

    private static int depot(CommandContext<CommandSourceStack> ctx, String guildName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Guild guild = guildName == null ? managed(player) : Guilds.data().byName(guildName);
        if (guild == null) {
            return fail(ctx, guildName == null ? "Only a guildmaster can do that." : "No guild is called " + guildName + ".");
        }
        Spot spot = Depots.ownedNearby(player);
        if (spot == null) {
            return fail(ctx, "Stand at a depot you own, or look at it.");
        }
        guild.depot = spot;
        Guilds.data().setDirty();
        return ok(ctx, guild.name + "'s depot is now at " + spot.pos().toShortString() + ".");
    }

    private static int bind(CommandContext<CommandSourceStack> ctx, String guildName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Guild guild = guildName == null ? managed(player) : Guilds.data().byName(guildName);
        if (guild == null) {
            return fail(ctx, guildName == null ? "Only a guildmaster can do that." : "No guild is called " + guildName + ".");
        }
        HitResult hit = player.pick(LOOK_REACH, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK
            || !(player.level().getBlockEntity(((BlockHitResult) hit).getBlockPos()) instanceof QuestBoardBlockEntity board)) {
            return fail(ctx, "Look at a quest board.");
        }
        board.bind(guild.id);
        return ok(ctx, "This quest board now lists the quests and contracts of " + guild.name + ".");
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        GuildData data = Guilds.data();
        ServerPlayer player = ctx.getSource().getPlayer();
        Guild guild = name != null ? data.byName(name) : player == null ? null : data.guildOf(player.getUUID());
        if (guild == null) {
            return fail(ctx, name != null ? "No guild is called " + name + "." : "You are not in a guild.");
        }
        boolean member = player != null && guild.has(player.getUUID());
        StringBuilder text = new StringBuilder(guild.name + (guild.isPublic ? " (public)" : " (private)"));
        text.append("\nGuildmaster: ").append(guild.master == null ? "the admins" : Guilds.nameOf(guild.master));
        text.append("\nMembers: ").append(guild.members.size());
        text.append("\nJoin fee: ").append(Currency.format(guild.joinFee));
        text.append("\nContract commission: ").append(guild.commissionPercent).append("%");
        text.append("\nPortal: ").append(guild.portalInstalled ? "installed" : "not installed");
        text.append("\nOpen postings: ").append(data.bountiesOf(guild.id).stream().filter(Bounty::open).count());
        if (member) {
            text.append("\nTreasury: ").append(Currency.format(guild.treasury));
        }
        text.append("\nCharter: ").append(guild.charter.isEmpty() ? "(none)" : guild.charter);
        ctx.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        GuildData data = Guilds.data();
        if (data.guilds.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("There are no guilds yet."), false);
            return 1;
        }
        StringBuilder text = new StringBuilder("Guilds:");
        for (Guild guild : data.guilds) {
            text.append("\n").append(guild.name).append(guild.isPublic ? " (public)" : "")
                .append(" - ").append(guild.members.size()).append(" members, fee ").append(Currency.format(guild.joinFee));
        }
        ctx.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int adminCreate(CommandContext<CommandSourceStack> ctx, String name) {
        GuildData data = Guilds.data();
        String problem = checkName(data, name);
        if (problem != null) {
            return fail(ctx, problem);
        }
        if (data.guilds.stream().anyMatch(guild -> guild.isPublic)) {
            return fail(ctx, "The public guild already exists.");
        }
        Guild guild = new Guild(UUID.randomUUID(), name, true, null, GuildsConfig.get().contractCommissionPercent);
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            guild.depot = Depots.ownedNearby(player);
        }
        data.guilds.add(guild);
        data.setDirty();
        return ok(ctx, "Created the public guild " + name + ".");
    }

    private static int portal(CommandContext<CommandSourceStack> ctx, String name, boolean installed) {
        Guild guild = Guilds.data().byName(name);
        if (guild == null) {
            return fail(ctx, "No guild is called " + name + ".");
        }
        guild.portalInstalled = installed;
        Guilds.data().setDirty();
        return ok(ctx, guild.name + " portal installed: " + installed + ".");
    }

    // The guild the player runs: their own as guildmaster, or the public guild for an admin.
    private static Guild managed(ServerPlayer player) {
        GuildData data = Guilds.data();
        Guild own = data.guildOf(player.getUUID());
        if (own != null && player.getUUID().equals(own.master)) {
            return own;
        }
        if (Guilds.isAdmin(player)) {
            return data.guilds.stream().filter(guild -> guild.isPublic).findFirst().orElse(null);
        }
        return null;
    }

    private static UUID memberByName(Guild guild, String name) {
        for (UUID member : guild.members) {
            if (Guilds.nameOf(member).equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    private static String checkName(GuildData data, String name) {
        int max = GuildsConfig.get().maxNameLength;
        if (name.length() < 3 || name.length() > max || !name.matches("[A-Za-z0-9 '_-]+")) {
            return "A guild name is 3 to " + max + " letters, digits, spaces, apostrophes, dashes or underscores.";
        }
        if (data.byName(name) != null) {
            return "A guild with that name exists.";
        }
        return null;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }
}
