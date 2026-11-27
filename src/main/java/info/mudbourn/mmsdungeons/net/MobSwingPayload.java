package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: a mob started a Better Combat swing with the given animation, length in ticks, upswing share and hand.
public record MobSwingPayload(int entityId, String animation, int ticks, float upswing, byte hand) implements CustomPacketPayload {

    public static final Type<MobSwingPayload> TYPE = new Type<>(MmsDungeons.id("mob_swing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobSwingPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MobSwingPayload::entityId,
        ByteBufCodecs.STRING_UTF8,
        MobSwingPayload::animation,
        ByteBufCodecs.VAR_INT,
        MobSwingPayload::ticks,
        ByteBufCodecs.FLOAT,
        MobSwingPayload::upswing,
        ByteBufCodecs.BYTE,
        MobSwingPayload::hand,
        MobSwingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
