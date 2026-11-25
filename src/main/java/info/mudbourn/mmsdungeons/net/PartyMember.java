package info.mudbourn.mmsdungeons.net;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

// One party member's synced stats and position, as the client draws them.
public record PartyMember(
    UUID id,
    String name,
    float health,
    float maxHealth,
    int food,
    int armor,
    double x,
    double y,
    double z,
    Identifier dimension
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyMember> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        PartyMember::id,
        ByteBufCodecs.STRING_UTF8,
        PartyMember::name,
        ByteBufCodecs.FLOAT,
        PartyMember::health,
        ByteBufCodecs.FLOAT,
        PartyMember::maxHealth,
        ByteBufCodecs.VAR_INT,
        PartyMember::food,
        ByteBufCodecs.VAR_INT,
        PartyMember::armor,
        ByteBufCodecs.DOUBLE,
        PartyMember::x,
        ByteBufCodecs.DOUBLE,
        PartyMember::y,
        ByteBufCodecs.DOUBLE,
        PartyMember::z,
        Identifier.STREAM_CODEC,
        PartyMember::dimension,
        PartyMember::new);
}
