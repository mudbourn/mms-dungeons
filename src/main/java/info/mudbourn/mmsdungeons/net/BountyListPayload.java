package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.guild.BountyView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: the postings of one guild, or the player's own when the guild is empty.
public record BountyListPayload(Optional<UUID> guildId, List<BountyView> bounties) implements CustomPacketPayload {

    public static final Type<BountyListPayload> TYPE = new Type<>(MmsDungeons.id("bounty_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyListPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        BountyListPayload::guildId,
        ByteBufCodecs.fromCodecWithRegistries(BountyView.CODEC.listOf()),
        BountyListPayload::bounties,
        BountyListPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
