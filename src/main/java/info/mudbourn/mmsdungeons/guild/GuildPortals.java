package info.mudbourn.mmsdungeons.guild;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// Decides who may pass a guild's portal, and tells operators which guilds still need one.
public final class GuildPortals {

    private GuildPortals() {
    }

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GuildData data = Guilds.data();
            if (data != null && Guilds.isAdmin(handler.player)) {
                data.guilds.stream().filter(GuildPortals::needsPortal).forEach(guild -> tell(handler.player, guild));
            }
        });
    }

    // Tells every online operator that a guild needs a portal.
    static void announceNeeded(MinecraftServer server, Guild guild) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Guilds.isAdmin(player)) {
                tell(player, guild);
            }
        }
    }

    private static boolean needsPortal(Guild guild) {
        return !guild.isPublic && guild.portal == null;
    }

    private static void tell(ServerPlayer operator, Guild guild) {
        operator.sendSystemMessage(Component.literal(
            guild.name + " needs a dungeon portal. Stand in or look at its portal and run /guild admin portal \""
                + guild.name + "\" set."));
    }

    public static Guild ownerOf(GlobalPos portal) {
        GuildData data = Guilds.data();
        if (data == null) {
            return null;
        }
        return data.guilds.stream().filter(guild -> portal.equals(guild.portal)).findFirst().orElse(null);
    }

    // Whether the player may pass the portal, or the reason they may not.
    public static Component refusal(ServerPlayer player, GlobalPos portal) {
        Guild owner = ownerOf(portal);
        if (owner == null) {
            return Guilds.isAdmin(player) ? null : Component.literal("No guild owns this portal yet.");
        }
        if (owner.has(player.getUUID()) || partyHasOnlineMember(player, owner)) {
            return null;
        }
        return Component.literal("This portal belongs to " + owner.name
            + ". Only its members, and parties with one of its members online, may pass.");
    }

    private static boolean partyHasOnlineMember(ServerPlayer player, Guild guild) {
        MinecraftServer server = player.level().getServer();
        for (UUID member : Guilds.partyMembers(player.getUUID())) {
            if (guild.has(member) && server.getPlayerList().getPlayer(member) != null) {
                return true;
            }
        }
        return false;
    }
}
