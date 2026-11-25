package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;

// Where a quest or contract stands.
public enum BountyStatus {
    AVAILABLE("Available"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    EXPIRED("Expired");

    public static final Codec<BountyStatus> CODEC = Codec.STRING.xmap(BountyStatus::valueOf, BountyStatus::name);

    private final String label;

    BountyStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
