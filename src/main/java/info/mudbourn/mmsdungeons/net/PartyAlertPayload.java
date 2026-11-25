package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: a teammate was just attacked.
public record PartyAlertPayload(UUID victim) implements CustomPacketPayload {

    public static final Type<PartyAlertPayload> TYPE = new Type<>(MmsDungeons.id("party_alert"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyAlertPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        PartyAlertPayload::victim,
        PartyAlertPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
