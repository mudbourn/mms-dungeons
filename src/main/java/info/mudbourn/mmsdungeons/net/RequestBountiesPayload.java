package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client to server: send the postings of a guild, or the player's own when the guild is empty.
public record RequestBountiesPayload(Optional<UUID> guildId) implements CustomPacketPayload {

    public static final Type<RequestBountiesPayload> TYPE = new Type<>(MmsDungeons.id("request_bounties"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBountiesPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        RequestBountiesPayload::guildId,
        RequestBountiesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
