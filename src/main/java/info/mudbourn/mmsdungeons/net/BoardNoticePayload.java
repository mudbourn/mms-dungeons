package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: the result of a quest board action, shown even while a board screen is open.
public record BoardNoticePayload(String message, boolean failed) implements CustomPacketPayload {

    public static final Type<BoardNoticePayload> TYPE = new Type<>(MmsDungeons.id("board_notice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardNoticePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        BoardNoticePayload::message,
        ByteBufCodecs.BOOL,
        BoardNoticePayload::failed,
        BoardNoticePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
