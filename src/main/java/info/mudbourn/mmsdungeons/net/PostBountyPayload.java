package info.mudbourn.mmsdungeons.net;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.guild.BountyType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

// Client to server: post a quest or contract at the open board.
public record PostBountyPayload(String title, String description, ItemStack proof, int count, long reward,
                                BountyType type) implements CustomPacketPayload {

    public static final Type<PostBountyPayload> TYPE = new Type<>(MmsDungeons.id("post_bounty"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostBountyPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PostBountyPayload::title,
        ByteBufCodecs.STRING_UTF8,
        PostBountyPayload::description,
        ItemStack.OPTIONAL_STREAM_CODEC,
        PostBountyPayload::proof,
        ByteBufCodecs.VAR_INT,
        PostBountyPayload::count,
        ByteBufCodecs.VAR_LONG,
        PostBountyPayload::reward,
        ByteBufCodecs.idMapper(index -> BountyType.values()[index], BountyType::ordinal),
        PostBountyPayload::type,
        PostBountyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
