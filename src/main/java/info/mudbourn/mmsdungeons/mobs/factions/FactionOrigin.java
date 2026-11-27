package info.mudbourn.mmsdungeons.mobs.factions;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

// The Origins origin a faction member may be born with, and which powers the mob version of it has.
public enum FactionOrigin {
    NONE("none", false, false, null, List.of()),
    ARACHNID("arachnid", false, false, null, List.of(FactionClass.ARCHER, FactionClass.KNIGHT)),
    AVIAN("avian", true, false, ParticleTypes.CLOUD, List.of(FactionClass.ARCHER, FactionClass.MAGE)),
    BLAZEBORN("blazeborn", false, true, ParticleTypes.FLAME, List.of(FactionClass.KNIGHT, FactionClass.PALADIN)),
    ENDERIAN("enderian", false, true, ParticleTypes.PORTAL, List.of(FactionClass.MAGE)),
    BEASTFOLK("beastfolk", true, false, null, List.of(FactionClass.ARCHER, FactionClass.KNIGHT, FactionClass.KNIGHT)),
    FISHMAN("fishman", false, false, ParticleTypes.BUBBLE, List.of(FactionClass.PRIEST, FactionClass.MAGE, FactionClass.KNIGHT)),
    PHANTOM("phantom", false, false, ParticleTypes.SMOKE, List.of(FactionClass.MAGE, FactionClass.ARCHER)),
    SHULK("shulk", false, false, null, List.of(FactionClass.PALADIN, FactionClass.KNIGHT, FactionClass.CLERIC)),
    FLORAN("floran", false, false, ParticleTypes.SPORE_BLOSSOM_AIR, List.of(FactionClass.PRIEST, FactionClass.ARCHER, FactionClass.MAGE, FactionClass.KNIGHT)),
    TRUFFLE("truffle", false, false, ParticleTypes.MYCELIUM, List.of(FactionClass.MAGE, FactionClass.CLERIC, FactionClass.KNIGHT)),
    PIGLIN("piglin", false, false, null, List.of(FactionClass.KNIGHT, FactionClass.PALADIN)),
    FAIRY("fairy", true, false, null, List.of(FactionClass.CLERIC, FactionClass.CLERIC, FactionClass.CLERIC, FactionClass.MAGE));

    private static final float ORIGIN_CHANCE = 0.65f;
    private static final int FAVORED_WEIGHT = 3;

    private final String id;
    private final boolean noFallDamage;
    private final boolean hurtByWater;
    private final @Nullable ParticleOptions aura;
    private final List<FactionClass> roles;

    FactionOrigin(String id, boolean noFallDamage, boolean hurtByWater, @Nullable ParticleOptions aura, List<FactionClass> roles) {
        this.id = id;
        this.noFallDamage = noFallDamage;
        this.hurtByWater = hurtByWater;
        this.aura = aura;
        this.roles = roles;
    }

    public String id() {
        return this.id;
    }

    public boolean noFallDamage() {
        return this.noFallDamage;
    }

    public boolean hurtByWater() {
        return this.hurtByWater;
    }

    public @Nullable ParticleOptions aura() {
        return this.aura;
    }

    // The namespace of the origin in the MMS Origins pack: the fairy, floran, truffle and piglin are MMS Origins' own, the fishman is Origins', the rest come from Origins Tweaks.
    public String namespace() {
        return switch (this) {
            case FAIRY, FLORAN, TRUFFLE, PIGLIN -> "mms_origins";
            case FISHMAN, NONE -> "origins";
            default -> "originstweaks";
        };
    }

    // The translation key of the origin's name, as MMS Origins names it.
    public String nameKey() {
        return "origin." + namespace() + "." + this.id + ".name";
    }

    // The classes this origin is born into: fighters to the front, fragile or land-weak origins to range and support. A repeated class is picked more often.
    public List<FactionClass> roles() {
        return this.roles;
    }

    public FactionClass pickRole(RandomSource random) {
        return this.roles.get(random.nextInt(this.roles.size()));
    }

    public static FactionOrigin byId(String id) {
        for (FactionOrigin origin : values()) {
            if (origin.id.equals(id)) {
                return origin;
            }
        }
        return NONE;
    }

    // Rolls the faction's origin chance, then any origin that fits the class when it is already set, with the faction's own favorites three times as likely.
    public static FactionOrigin roll(Faction faction, @Nullable FactionClass fixedClass, RandomSource random) {
        if (random.nextFloat() >= ORIGIN_CHANCE) {
            return NONE;
        }
        List<FactionOrigin> favored = switch (faction) {
            case BANDIT -> List.of(ARACHNID, BEASTFOLK, AVIAN, FISHMAN, FAIRY, FLORAN, TRUFFLE);
            case MERCENARY -> List.of(BLAZEBORN, AVIAN, BEASTFOLK, SHULK, FAIRY, PIGLIN);
            case GUILDSMAN -> List.of(ENDERIAN, PHANTOM, BLAZEBORN, SHULK, FISHMAN, FLORAN, PIGLIN);
        };
        List<FactionOrigin> pool = new ArrayList<>();
        for (FactionOrigin origin : values()) {
            if (origin == NONE || fixedClass != null && !origin.roles.contains(fixedClass)) {
                continue;
            }
            int weight = favored.contains(origin) ? FAVORED_WEIGHT : 1;
            for (int i = 0; i < weight; i++) {
                pool.add(origin);
            }
        }
        return pool.isEmpty() ? NONE : pool.get(random.nextInt(pool.size()));
    }
}
