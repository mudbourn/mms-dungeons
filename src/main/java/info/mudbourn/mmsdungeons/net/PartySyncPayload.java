package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: every member of the receiver's party, empty when they have none.
public record PartySyncPayload(List<PartyMember> members) implements CustomPacketPayload {

    public static final Type<PartySyncPayload> TYPE = new Type<>(MmsDungeons.id("party_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartySyncPayload> CODEC = StreamCodec.composite(
        PartyMember.CODEC.apply(ByteBufCodecs.list()),
        PartySyncPayload::members,
        PartySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
