package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: open the quest board screen for a guild.
public record OpenBoardPayload(UUID guildId, String guildName, boolean postsQuests) implements CustomPacketPayload {

    public static final Type<OpenBoardPayload> TYPE = new Type<>(MmsDungeons.id("open_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        OpenBoardPayload::guildId,
        ByteBufCodecs.STRING_UTF8,
        OpenBoardPayload::guildName,
        ByteBufCodecs.BOOL,
        OpenBoardPayload::postsQuests,
        OpenBoardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
