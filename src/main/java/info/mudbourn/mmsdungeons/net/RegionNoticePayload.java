package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: the player entered a dungeon region.
public record RegionNoticePayload(String title, int color) implements CustomPacketPayload {

    public static final Type<RegionNoticePayload> TYPE = new Type<>(MmsDungeons.id("region_notice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RegionNoticePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        RegionNoticePayload::title,
        ByteBufCodecs.INT,
        RegionNoticePayload::color,
        RegionNoticePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
