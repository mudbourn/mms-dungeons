package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

// Client to server: take, hand in, abandon or delete one posting.
public record BountyActionPayload(Action action, UUID bounty) implements CustomPacketPayload {

    public enum Action {
        TAKE,
        HAND_IN,
        ABANDON,
        DELETE
    }

    public static final Type<BountyActionPayload> TYPE = new Type<>(MmsDungeons.id("bounty_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyActionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.idMapper(
            ByIdMap.continuous(Action::ordinal, Action.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
            Action::ordinal
        ),
        BountyActionPayload::action,
        UUIDUtil.STREAM_CODEC,
        BountyActionPayload::bounty,
        BountyActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
