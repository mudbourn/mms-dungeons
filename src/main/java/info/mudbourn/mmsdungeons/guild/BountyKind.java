package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;

// Whether a posting came from the guild (a quest) or from an outside player (a contract).
public enum BountyKind {
    QUEST,
    CONTRACT;

    public static final Codec<BountyKind> CODEC = Codec.STRING.xmap(BountyKind::valueOf, BountyKind::name);
}
