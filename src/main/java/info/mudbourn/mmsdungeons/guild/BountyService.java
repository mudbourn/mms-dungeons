package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmsdungeons.config.GuildsConfig;
import info.mudbourn.mmsdungeons.net.PostBountyPayload;
import info.mudbourn.mmseconomy.deal.Spot;
import info.mudbourn.mmseconomy.economy.Billing;
import info.mudbourn.mmseconomy.economy.Currency;
import info.mudbourn.mmseconomy.economy.Wallet;
import info.mudbourn.mmseconomy.history.History;
import info.mudbourn.mmseconomy.history.HistoryCategory;
import info.mudbourn.mmseconomy.history.HistoryEntry;
import info.mudbourn.mmseconomy.market.Depot;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The rules for posting, taking, handing in, abandoning, deleting and expiring quests and contracts.
final class BountyService {

    private static final Logger LOG = LoggerFactory.getLogger("mms_dungeons");
    private static final int MAX_TITLE = 40;
    private static final int MAX_DESCRIPTION = 200;

    private BountyService() {
    }

    // Posts a quest from the guild treasury or a contract from the player's wallet; returns an error or null.
    static String post(ServerPlayer player, Guild guild, PostBountyPayload request) {
        MinecraftServer server = player.level().getServer();
        GuildData data = Guilds.data();
        GuildsConfig config = GuildsConfig.get();
        String title = request.title().strip();
        String description = request.description().strip();
        ItemStack template = request.proof().copyWithCount(1);
        if (title.isEmpty() || title.length() > MAX_TITLE) {
            return "Give the posting a title of 1 to " + MAX_TITLE + " characters.";
        }
        if (description.length() > MAX_DESCRIPTION) {
            return "The description is limited to " + MAX_DESCRIPTION + " characters.";
        }
        if (template.isEmpty() || request.count() < 1 || request.count() > config.maxProofCount) {
            return "Choose a proof item and a count from 1 to " + config.maxProofCount + ".";
        }
        if (request.reward() < 1) {
            return "Set a reward above zero.";
        }
        if (data.bountiesOf(guild.id).stream().filter(Bounty::open).count() >= config.bountiesPerGuild) {
            return "This guild has too many open postings.";
        }
        if (Depots.barrels(server, guild.depot).isEmpty()) {
            return guild.name + " has no working depot, so it cannot take postings yet.";
        }

        UUID id = player.getUUID();
        boolean asGuild = id.equals(guild.master) || guild.isPublic && Guilds.isAdmin(player);
        if (!asGuild && guild.has(id)) {
            return "Only the guildmaster posts quests for the guild.";
        }

        long today = Billing.today(server);
        Bounty.Proof proof = new Bounty.Proof(template, request.count());
        Bounty bounty;
        if (asGuild) {
            if (guild.treasury < request.reward()) {
                return "The guild treasury holds only " + Currency.format(guild.treasury) + ".";
            }
            guild.treasury -= request.reward();
            bounty = new Bounty(
                guild.id,
                BountyKind.QUEST,
                id,
                title,
                description,
                proof,
                request.reward(),
                request.tag(),
                today,
                today + config.questExpiryDays,
                null
            );
        } else {
            Spot depot = Depots.ownedNearby(player);
            if (depot == null) {
                return "Stand at a depot you own, or look at it, so the proof items can be shipped to you.";
            }
            if (!Wallet.withdraw(player, request.reward())) {
                return "You cannot afford " + Currency.format(request.reward()) + ".";
            }
            History.log(player, new HistoryEntry(
                today,
                HistoryCategory.PAYMENT,
                "",
                0,
                request.reward(),
                0L,
                -request.reward(),
                guild.name + ": " + title
            ));
            bounty = new Bounty(
                guild.id,
                BountyKind.CONTRACT,
                id,
                title,
                description,
                proof,
                request.reward(),
                request.tag(),
                today,
                today + config.contractExpiryDays,
                depot
            );
        }
        data.bounties.add(bounty);
        data.setDirty();
        return null;
    }

    // Takes a posting for the player's whole party; returns an error or null.
    static String take(ServerPlayer player, UUID bountyId) {
        GuildData data = Guilds.data();
        Bounty bounty = data.bounty(bountyId);
        if (bounty == null || bounty.status != BountyStatus.AVAILABLE) {
            return "That posting is no longer available.";
        }
        List<UUID> party = Guilds.partyMembers(player.getUUID());
        if (bounty.kind == BountyKind.CONTRACT && party.contains(bounty.poster)) {
            return "Your party cannot take its own member's posting.";
        }
        long held = data.bounties.stream()
            .filter(other -> other.status == BountyStatus.IN_PROGRESS && party.contains(other.taker))
            .count();
        if (held >= GuildsConfig.get().bountiesPerParty) {
            return "Your party already holds " + held + " postings.";
        }
        bounty.status = BountyStatus.IN_PROGRESS;
        bounty.taker = player.getUUID();
        data.setDirty();
        return null;
    }

    // Lets the holding party give a posting back; returns an error or null.
    static String abandon(ServerPlayer player, UUID bountyId) {
        GuildData data = Guilds.data();
        Bounty bounty = data.bounty(bountyId);
        if (bounty == null || bounty.status != BountyStatus.IN_PROGRESS) {
            return "That posting is not in progress.";
        }
        if (!Guilds.partyMembers(bounty.taker).contains(player.getUUID())) {
            return "Your party does not hold that posting.";
        }
        bounty.status = BountyStatus.AVAILABLE;
        bounty.taker = null;
        data.setDirty();
        return null;
    }

    // Removes an unheld posting and returns its escrow; returns an error or null.
    static String delete(ServerPlayer player, UUID bountyId) {
        MinecraftServer server = player.level().getServer();
        GuildData data = Guilds.data();
        Bounty bounty = data.bounty(bountyId);
        if (bounty == null || !bounty.open()) {
            return "That posting is already finished.";
        }
        Guild guild = data.byId(bounty.guild);
        boolean poster = player.getUUID().equals(bounty.poster);
        boolean guildStaff = bounty.kind == BountyKind.QUEST
            && guild != null
            && (player.getUUID().equals(guild.master) || guild.isPublic && Guilds.isAdmin(player));
        if (!poster && !guildStaff) {
            return "Only the poster can delete this.";
        }
        if (bounty.status == BountyStatus.IN_PROGRESS) {
            return "A party holds this posting. It can be deleted once they abandon it.";
        }
        refund(server, data, bounty, "Deleted");
        data.bounties.remove(bounty);
        data.setDirty();
        return null;
    }

    // Takes the proof items from the player into the guild depot, then pays the party; returns an error or null.
    static String handIn(ServerPlayer player, UUID bountyId) {
        MinecraftServer server = player.level().getServer();
        GuildData data = Guilds.data();
        Bounty bounty = data.bounty(bountyId);
        if (bounty == null || bounty.status != BountyStatus.IN_PROGRESS) {
            return "That posting is not in progress.";
        }
        List<UUID> party = Guilds.partyMembers(bounty.taker);
        if (!party.contains(player.getUUID())) {
            return "Your party does not hold that posting.";
        }
        Guild guild = data.byId(bounty.guild);
        if (guild == null) {
            return "The guild no longer exists.";
        }
        List<Container> barrels = Depots.barrels(server, guild.depot);
        if (barrels.isEmpty()) {
            return guild.name + "'s depot is missing or incomplete.";
        }
        ItemStack template = bounty.proof.stack();
        int need = bounty.proof.count();
        int have = count(player, template);
        if (have < need) {
            return "You carry " + have + " of the " + need + " required.";
        }
        if (Depots.room(barrels, template) < need) {
            return guild.name + "'s depot is full.";
        }

        int inserted = Depot.insert(barrels, template, need);
        if (inserted < need) {
            Depot.take(barrels, template, inserted);
            return guild.name + "'s depot is full.";
        }
        remove(player, template, need);

        bounty.status = BountyStatus.COMPLETED;
        bounty.finishedDay = Billing.today(server);
        Payout.settle(server, data, bounty, guild, party);
        if (bounty.kind == BountyKind.CONTRACT) {
            Shipping.queue(server, data, guild, bounty);
        }
        data.setDirty();
        return null;
    }

    // Expires late postings, prunes old finished ones and delivers the shipments that are due.
    static void dayChange(MinecraftServer server, GuildData data) {
        long today = Billing.today(server);
        for (Bounty bounty : new ArrayList<>(data.bounties)) {
            if (bounty.open() && bounty.expiryDay <= today) {
                refund(server, data, bounty, "Expired");
                bounty.status = BountyStatus.EXPIRED;
                bounty.taker = null;
                bounty.finishedDay = today;
            } else if (!bounty.open() && bounty.finishedDay + GuildsConfig.get().finishedRetentionDays <= today) {
                data.bounties.remove(bounty);
            }
        }
        Shipping.deliver(server, data, today);
        data.lastDay = today;
        data.setDirty();
    }

    // Builds how a posting looks to one viewer.
    static BountyView view(ServerPlayer viewer, GuildData data, Bounty bounty, long today) {
        Guild guild = data.byId(bounty.guild);
        boolean inProgress = bounty.status == BountyStatus.IN_PROGRESS;
        List<UUID> members = inProgress ? Guilds.partyMembers(bounty.taker) : Guilds.partyMembers(viewer.getUUID());
        long share = 0L;
        long commission = 0L;
        if (guild != null) {
            Payout.Plan plan = Payout.plan(data, bounty, guild, members);
            commission = plan.commission();
            for (Payout.Share each : plan.shares()) {
                if (each.member().equals(viewer.getUUID())) {
                    share = each.paid();
                }
            }
        }
        return new BountyView(
            bounty,
            guild == null ? "" : guild.name,
            Guilds.nameOf(bounty.poster),
            bounty.taker == null ? "" : Guilds.nameOf(bounty.taker),
            inProgress && members.contains(viewer.getUUID()),
            members.size(),
            share,
            commission,
            count(viewer, bounty.proof.stack()),
            Math.max(0L, bounty.expiryDay - today));
    }

    private static void refund(MinecraftServer server, GuildData data, Bounty bounty, String reason) {
        Guild guild = data.byId(bounty.guild);
        if (bounty.kind == BountyKind.QUEST) {
            if (guild == null) {
                LOG.warn("Quest {} lost guild {}; its {} Pice reward was not refunded.", bounty.id, bounty.guild, bounty.reward);
                return;
            }
            guild.treasury += bounty.reward;
            return;
        }
        long day = Billing.today(server);
        String label = (guild == null ? "" : guild.name + ": ") + bounty.title + " (" + reason + ")";
        HistoryEntry entry = new HistoryEntry(
            day,
            HistoryCategory.PAYMENT,
            "",
            0,
            bounty.reward,
            0L,
            bounty.reward,
            label
        );
        String returned = Currency.format(bounty.reward);
        String message = bounty.title + " " + reason.toLowerCase() + ". " + returned + " returned to your wallet.";
        Payout.credit(server, bounty.poster, bounty.reward, entry, message);
    }

    static int count(ServerPlayer player, ItemStack template) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void remove(ServerPlayer player, ItemStack template, int amount) {
        int left = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
    }
}
