package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmsdungeons.config.GuildsConfig;
import info.mudbourn.mmseconomy.economy.Billing;
import info.mudbourn.mmseconomy.economy.Currency;
import info.mudbourn.mmseconomy.economy.PendingPayouts;
import info.mudbourn.mmseconomy.economy.Tax;
import info.mudbourn.mmseconomy.economy.TaxKind;
import info.mudbourn.mmseconomy.economy.Treasury;
import info.mudbourn.mmseconomy.economy.Wallet;
import info.mudbourn.mmseconomy.history.HistoryEntry;
import info.mudbourn.mmseconomy.history.History;
import info.mudbourn.mmseconomy.history.HistoryCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// Splits an escrowed reward between the guild and the party, with the guild taxes and the server tax.
final class Payout {

    // One member's cut: the even share, the guild taxes, the server tax and what reaches the wallet.
    record Share(UUID member, Guild ownGuild, long base, long toPosting, long toOwnGuild, long serverTax, long paid) {
    }

    // The guild commission, the leftover Pice and every member's cut.
    record Plan(long commission, long remainder, List<Share> shares) {
    }

    private Payout() {
    }

    // Computes the split for a reward without moving any money.
    static Plan plan(GuildData data, Bounty bounty, Guild posting, List<UUID> members) {
        GuildsConfig config = GuildsConfig.get();
        long commission = bounty.kind == BountyKind.CONTRACT ? percentOf(bounty.reward, posting.commissionPercent) : 0L;
        long pool = bounty.reward - commission;
        long each = members.isEmpty() ? 0L : pool / members.size();
        long remainder = pool - each * members.size();
        List<Share> shares = new ArrayList<>();
        for (UUID member : members) {
            Guild own = data.guildOf(member);
            long toPosting = 0L;
            long toOwn = 0L;
            if (own == null) {
                toPosting = percentOf(each, config.noGuildPostingPercent);
            } else if (!own.id.equals(posting.id)) {
                toPosting = percentOf(each, config.otherGuildPostingPercent);
                toOwn = percentOf(each - toPosting, config.otherGuildOwnPercent);
            }
            long net = each - toPosting - toOwn;
            long serverTax = Tax.on(net, TaxKind.PAY);
            shares.add(new Share(member, own, each, toPosting, toOwn, serverTax, net - serverTax));
        }
        return new Plan(commission, remainder, shares);
    }

    // Moves the money of a plan: guild treasuries, the server treasury and each member's wallet or pending payout.
    static void settle(MinecraftServer server, GuildData data, Bounty bounty, Guild posting, List<UUID> members) {
        Plan plan = plan(data, bounty, posting, members);
        posting.treasury += plan.commission() + plan.remainder();
        long day = Billing.today(server);
        String label = posting.name + ": " + bounty.title;
        for (Share share : plan.shares()) {
            posting.treasury += share.toPosting();
            if (share.ownGuild() != null) {
                share.ownGuild().treasury += share.toOwnGuild();
            }
            Treasury.get(server).credit(share.serverTax());
            long deducted = share.base() - share.paid();
            HistoryEntry entry = new HistoryEntry(day, HistoryCategory.PAYMENT, "", 0, share.base(), deducted, share.paid(), label);
            ServerPlayer online = server.getPlayerList().getPlayer(share.member());
            if (online != null) {
                Wallet.deposit(online, share.paid());
                History.log(online, entry);
                online.sendSystemMessage(Component.literal(
                    bounty.title + " paid " + Currency.format(share.paid()) + " (share " + Currency.format(share.base()) + ")."));
            } else {
                PendingPayouts.get(server).record(share.member(), share.paid(), entry);
            }
        }
        data.setDirty();
    }

    // A floored percentage of an amount.
    static long percentOf(long amount, int percent) {
        if (amount <= 0 || percent <= 0) {
            return 0L;
        }
        return amount / 100 * percent + amount % 100 * percent / 100;
    }
}
