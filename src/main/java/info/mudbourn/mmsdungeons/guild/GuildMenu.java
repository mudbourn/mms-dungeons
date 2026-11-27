package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmsdungeons.net.BoardNoticePayload;
import info.mudbourn.mmsdungeons.net.GuildMenuActionPayload;
import info.mudbourn.mmsdungeons.net.GuildMenuPayload;
import info.mudbourn.mmseconomy.economy.Currency;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// The guild menu: runs its buttons as the player's own /guild commands, so every rule stays in GuildCommands.
final class GuildMenu {

    private GuildMenu() {
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(GuildMenuPayload.TYPE, GuildMenuPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuildMenuActionPayload.TYPE, GuildMenuActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GuildMenuActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String command = payload.command().strip();
            if (!command.isEmpty() && !command.startsWith("admin")) {
                run(player, command);
            }
            send(player);
        });
    }

    // Runs the command with the player's own permissions and hands its replies back as one notice.
    private static void run(ServerPlayer player, String command) {
        List<String> lines = new ArrayList<>();
        boolean[] failed = {false};
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                lines.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                failed[0] = true;
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        player.level().getServer().getCommands()
            .performPrefixedCommand(player.createCommandSourceStack().withSource(capture), "guild " + command);
        if (!lines.isEmpty()) {
            ServerPlayNetworking.send(player, new BoardNoticePayload(String.join("\n", lines), failed[0]));
        }
    }

    static void send(ServerPlayer player) {
        GuildData data = Guilds.data();
        UUID id = player.getUUID();
        Guild own = data.guildOf(id);
        boolean admin = Guilds.isAdmin(player);
        List<GuildMenuPayload.Row> rows = new ArrayList<>();
        for (Guild guild : data.guilds) {
            rows.add(new GuildMenuPayload.Row(
                guild.name,
                guild.commandName(),
                guild.isPublic,
                guild.master == null ? "the admins" : Guilds.nameOf(guild.master),
                guild.members.size(),
                Currency.format(guild.joinFee),
                guild.commissionPercent,
                guild.charter));
        }
        Guild managed = own != null ? own : admin ? data.guilds.stream().filter(guild -> guild.isPublic).findFirst().orElse(null) : null;
        boolean manager = managed != null && (id.equals(managed.master) || managed.isPublic && admin);
        List<String> members = new ArrayList<>();
        List<String> banished = new ArrayList<>();
        if (managed != null) {
            managed.members.forEach(member -> members.add(Guilds.nameOf(member)));
            managed.banished.forEach(exile -> banished.add(Guilds.nameOf(exile)));
        }
        ServerPlayNetworking.send(player, new GuildMenuPayload(
            rows,
            managed == null ? -1 : data.guilds.indexOf(managed),
            manager,
            admin,
            managed == null ? "" : Currency.format(managed.treasury),
            members,
            banished));
    }
}
