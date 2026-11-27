package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: every guild, and the player's own with what they may manage, for the guild menu.
public record GuildMenuPayload(List<Row> guilds, int mine, boolean manager, boolean admin, String treasury,
                               List<String> members, List<String> banished) implements CustomPacketPayload {

    public static final Type<GuildMenuPayload> TYPE = new Type<>(MmsDungeons.id("guild_menu"));

    // One guild as the list shows it; the command name is what the menu sends back.
    public record Row(String name, String commandName, boolean isPublic, String master, int members, String fee,
                      int commission, String charter) {

        static final StreamCodec<FriendlyByteBuf, Row> CODEC = StreamCodec.of(
            (buf, row) -> {
                buf.writeUtf(row.name);
                buf.writeUtf(row.commandName);
                buf.writeBoolean(row.isPublic);
                buf.writeUtf(row.master);
                buf.writeVarInt(row.members);
                buf.writeUtf(row.fee);
                buf.writeVarInt(row.commission);
                buf.writeUtf(row.charter);
            },
            buf -> new Row(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readVarInt(),
                buf.readUtf(), buf.readVarInt(), buf.readUtf()));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, GuildMenuPayload> CODEC = StreamCodec.composite(
        Row.CODEC.apply(ByteBufCodecs.list()),
        GuildMenuPayload::guilds,
        ByteBufCodecs.VAR_INT,
        GuildMenuPayload::mine,
        ByteBufCodecs.BOOL,
        GuildMenuPayload::manager,
        ByteBufCodecs.BOOL,
        GuildMenuPayload::admin,
        ByteBufCodecs.STRING_UTF8,
        GuildMenuPayload::treasury,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        GuildMenuPayload::members,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        GuildMenuPayload::banished,
        GuildMenuPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
