package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client to server: left click with a switched-on locator in the main hand, cycling its focus.
public record CycleLocatorPayload() implements CustomPacketPayload {

    public static final Type<CycleLocatorPayload> TYPE = new Type<>(MmsDungeons.id("cycle_locator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CycleLocatorPayload> CODEC = StreamCodec.unit(new CycleLocatorPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
