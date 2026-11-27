package info.mudbourn.mmsdungeons.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

// One thing the locator can find, with a bit mask of the dungeon regions it occurs in.
public record LocatorPoi(Identifier id, int regions) {

    public static final StreamCodec<ByteBuf, LocatorPoi> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        LocatorPoi::id,
        ByteBufCodecs.VAR_INT,
        LocatorPoi::regions,
        LocatorPoi::new);
}
