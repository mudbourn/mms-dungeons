package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client to server: set the held locator's tuned entries.
public record TuneLocatorPayload(List<LocatorPick> picks) implements CustomPacketPayload {

    public static final Type<TuneLocatorPayload> TYPE = new Type<>(MmsDungeons.id("tune_locator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TuneLocatorPayload> CODEC = StreamCodec.composite(
        LocatorPick.CODEC.apply(ByteBufCodecs.list(LocatorPick.MAX)),
        TuneLocatorPayload::picks,
        TuneLocatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
