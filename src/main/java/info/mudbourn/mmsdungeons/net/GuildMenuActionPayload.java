package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client to server: a /guild command from the guild menu, without the leading "guild", or empty to only refresh it.
public record GuildMenuActionPayload(String command) implements CustomPacketPayload {

    public static final Type<GuildMenuActionPayload> TYPE = new Type<>(MmsDungeons.id("guild_menu_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuildMenuActionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        GuildMenuActionPayload::command,
        GuildMenuActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
