package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: open the locator tuning screen with what the dungeon can generate and what is tuned.
public record OpenTuningPayload(
    List<LocatorPoi> presets,
    List<LocatorPoi> structures,
    List<LocatorPoi> biomes,
    List<LocatorPick> picks,
    int region
) implements CustomPacketPayload {

    public static final Type<OpenTuningPayload> TYPE = new Type<>(MmsDungeons.id("open_tuning"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTuningPayload> CODEC = StreamCodec.composite(
        LocatorPoi.CODEC.apply(ByteBufCodecs.list()),
        OpenTuningPayload::presets,
        LocatorPoi.CODEC.apply(ByteBufCodecs.list()),
        OpenTuningPayload::structures,
        LocatorPoi.CODEC.apply(ByteBufCodecs.list()),
        OpenTuningPayload::biomes,
        LocatorPick.CODEC.apply(ByteBufCodecs.list(LocatorPick.MAX)),
        OpenTuningPayload::picks,
        ByteBufCodecs.VAR_INT,
        OpenTuningPayload::region,
        OpenTuningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
