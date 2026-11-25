package info.mudbourn.mmsdungeons.guild;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.parties.party.member.PartyMemberRank;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

// Keeps a guild's Open Parties and Claims party in step with its members and guildmaster.
final class GuildOpac {

    private GuildOpac() {
    }

    private static IPartyManagerAPI parties(MinecraftServer server) {
        return OpenPACServerAPI.get(server).getPartyManager();
    }

    // True when the player belongs to an Open Parties and Claims party other than the guild's.
    static boolean inOtherParty(MinecraftServer server, UUID player, UUID guildParty) {
        IServerPartyAPI party = parties(server).getPartyByMember(player);
        return party != null && !party.getId().equals(guildParty);
    }

    // Creates the party with the player as owner and returns its id.
    static UUID create(MinecraftServer server, ServerPlayer owner) {
        IServerPartyAPI party = parties(server).createPartyForOwner(owner);
        return party == null ? null : party.getId();
    }

    static void add(MinecraftServer server, Guild guild, UUID player, String name) {
        IServerPartyAPI party = party(server, guild);
        if (party != null && party.getMemberInfo(player) == null) {
            party.addMember(player, PartyMemberRank.MEMBER, name);
        }
    }

    static void remove(MinecraftServer server, Guild guild, UUID player) {
        IServerPartyAPI party = party(server, guild);
        if (party != null && party.getMemberInfo(player) != null) {
            party.removeMember(player);
        }
    }

    // Rebuilds the party under a new owner and returns its id, or null when the new owner cannot own one.
    static UUID transfer(MinecraftServer server, Guild guild, ServerPlayer newOwner) {
        IServerPartyAPI old = party(server, guild);
        if (old != null) {
            parties(server).removePartyById(old.getId());
        }
        IServerPartyAPI created = parties(server).createPartyForOwner(newOwner);
        if (created == null) {
            return null;
        }
        for (UUID member : guild.members) {
            if (!member.equals(newOwner.getUUID())) {
                created.addMember(member, PartyMemberRank.MEMBER, Guilds.nameOf(member));
            }
        }
        return created.getId();
    }

    private static IServerPartyAPI party(MinecraftServer server, Guild guild) {
        return guild.opacParty == null ? null : parties(server).getPartyById(guild.opacParty);
    }
}
