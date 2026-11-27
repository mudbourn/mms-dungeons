package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmseconomy.economy.Billing;
import info.mudbourn.mmsdungeons.party.PartyData;
import info.mudbourn.mmsdungeons.party.Parties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

// Wires guilds, the quest board and the quest and contract rules into the server.
public final class Guilds {

    private static final int DAY_CHECK_TICKS = 20;

    private static GuildData data;

    private Guilds() {
    }

    public static void register() {
        GuildBlocks.register();
        GuildNetworking.register();
        GuildMenu.register();
        GuildClaims.register();
        GuildPortals.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
            data = server.overworld().getDataStorage().computeIfAbsent(GuildData.TYPE));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            data = null;
            GuildNetworking.forgetAll();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            GuildCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (data != null && server.getTickCount() % DAY_CHECK_TICKS == 0 && data.lastDay != Billing.today(server)) {
                BountyService.dayChange(server, data);
            }
        });
    }

    public static GuildData data() {
        return data;
    }

    // Everyone in the player's party, or just the player when they have none.
    public static List<UUID> partyMembers(UUID player) {
        PartyData parties = Parties.data();
        PartyData.Party party = parties == null ? null : parties.partyOf(player);
        return party == null ? List.of(player) : new ArrayList<>(party.members);
    }

    public static String nameOf(UUID player) {
        PartyData parties = Parties.data();
        return parties == null ? player.toString() : parties.nameOf(player);
    }

    public static boolean isAdmin(ServerPlayer player) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }
}
