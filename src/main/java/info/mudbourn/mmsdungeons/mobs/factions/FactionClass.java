package info.mudbourn.mmsdungeons.mobs.factions;

import net.minecraft.util.RandomSource;

// The six classes every faction group mixes.
public enum FactionClass {
    KNIGHT("knight", 4),
    PALADIN("paladin", 2),
    ARCHER("archer", 3),
    MAGE("mage", 2),
    CLERIC("cleric", 1),
    PRIEST("priest", 1);

    private final String id;
    private final int weight;

    FactionClass(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    public String id() {
        return this.id;
    }

    public boolean melee() {
        return this == KNIGHT || this == PALADIN;
    }

    public boolean caster() {
        return this == MAGE || this == CLERIC || this == PRIEST;
    }

    public static FactionClass byId(String id) {
        for (FactionClass value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return KNIGHT;
    }

    public static FactionClass pick(RandomSource random) {
        int total = 0;
        for (FactionClass value : values()) {
            total += value.weight;
        }
        int roll = random.nextInt(total);
        for (FactionClass value : values()) {
            roll -= value.weight;
            if (roll < 0) {
                return value;
            }
        }
        return KNIGHT;
    }
}
