package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.locator.LocatorTarget;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;

// One tuned entry on the wire: its kind and id.
public record LocatorPick(LocatorTarget.Kind kind, Identifier id) {

    public static final int MAX = 512;

    public static final StreamCodec<ByteBuf, LocatorPick> CODEC = StreamCodec.composite(
        ByteBufCodecs.idMapper(
            ByIdMap.continuous(LocatorTarget.Kind::ordinal, LocatorTarget.Kind.values(), ByIdMap.OutOfBoundsStrategy.CLAMP),
            LocatorTarget.Kind::ordinal
        ),
        LocatorPick::kind,
        Identifier.STREAM_CODEC,
        LocatorPick::id,
        LocatorPick::new);
}
