package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;

// The informational tag shown on a quest or contract.
public enum BountyType {
    SUBJUGATION("Subjugation"),
    FORAGING("Foraging"),
    GEAR_HUNT("Gear hunt"),
    MINING("Mining");

    public static final Codec<BountyType> CODEC = Codec.STRING.xmap(BountyType::valueOf, BountyType::name);

    private final String label;

    BountyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
