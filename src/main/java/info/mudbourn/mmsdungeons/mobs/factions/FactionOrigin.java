package info.mudbourn.mmsdungeons.mobs.factions;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

// The Origins origin a faction member may be born with; it carries the origin's real powers, so origins exist only with Origins and MMS Origins installed.
public enum FactionOrigin {
    NONE("none", List.of()),
    ARACHNID("arachnid", List.of(FactionClass.ARCHER, FactionClass.KNIGHT)),
    AVIAN("avian", List.of(FactionClass.ARCHER, FactionClass.KNIGHT, FactionClass.MAGE)),
    BLAZEBORN("blazeborn", List.of(FactionClass.KNIGHT, FactionClass.PALADIN)),
    ENDERIAN("enderian", List.of(FactionClass.MAGE)),
    BEASTFOLK("beastfolk", List.of(FactionClass.ARCHER, FactionClass.KNIGHT, FactionClass.KNIGHT)),
    FISHMAN("fishman", List.of(FactionClass.PRIEST, FactionClass.MAGE, FactionClass.KNIGHT)),
    PHANTOM("phantom", List.of(FactionClass.MAGE, FactionClass.ARCHER)),
    SHULK("shulk", List.of(FactionClass.PALADIN, FactionClass.KNIGHT, FactionClass.CLERIC)),
    FLORAN("floran", List.of(FactionClass.PRIEST, FactionClass.ARCHER, FactionClass.MAGE, FactionClass.KNIGHT)),
    TRUFFLE("truffle", List.of(FactionClass.MAGE, FactionClass.CLERIC, FactionClass.KNIGHT)),
    PIGLIN("piglin", List.of(FactionClass.KNIGHT, FactionClass.PALADIN)),
    FAIRY("fairy", List.of(FactionClass.CLERIC, FactionClass.CLERIC, FactionClass.CLERIC, FactionClass.MAGE));

    public static final boolean POWERS = FabricLoader.getInstance().isModLoaded("origins") && FabricLoader.getInstance().isModLoaded("mms_origins");
    private static final float ORIGIN_CHANCE = 0.65f;
    private static final int FAVORED_WEIGHT = 3;

    private final String id;
    private final List<FactionClass> roles;

    FactionOrigin(String id, List<FactionClass> roles) {
        this.id = id;
        this.roles = roles;
    }

    public String id() {
        return this.id;
    }

    // The namespace of the origin: every origin is MMS Origins'.
    public String namespace() {
        return "mms_origins";
    }

    public Identifier originId() {
        return Identifier.fromNamespaceAndPath(namespace(), this.id);
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
        if (!POWERS || random.nextFloat() >= ORIGIN_CHANCE) {
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
