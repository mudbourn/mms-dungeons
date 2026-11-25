package info.mudbourn.mmsdungeons.mobs.kobolds.entity;

import info.mudbourn.mmsdungeons.mobs.kobolds.KoboldMobs;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

// The kobold a Zombold used to be, which sets its look and what curing turns it back into.
public enum ZomboType {
    PIRATE_CAPTAIN("pirate_captain", () -> KoboldMobs.KOBOLD_CAPTAIN),
    PIRATE("pirate", () -> KoboldMobs.KOBOLD_PIRATE),
    WARRIOR("warrior", () -> KoboldMobs.KOBOLD_WARRIOR),
    ENGINEER("engineer", () -> KoboldMobs.KOBOLD_ENGINEER),
    ENCHANTER("enchanter", () -> KoboldMobs.KOBOLD_ENCHANTER),
    BASE("base", () -> KoboldMobs.KOBOLD);

    private final String id;
    private final Supplier<EntityType<? extends AbstractKobold>> cured;

    ZomboType(String id, Supplier<EntityType<? extends AbstractKobold>> cured) {
        this.id = id;
        this.cured = cured;
    }

    public String id() {
        return this.id;
    }

    public EntityType<? extends AbstractKobold> cured() {
        return this.cured.get();
    }

    public boolean isPirate() {
        return this == PIRATE || this == PIRATE_CAPTAIN;
    }

    public static ZomboType byId(String id) {
        for (ZomboType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return BASE;
    }

    public static ZomboType of(EntityType<?> kobold) {
        for (ZomboType type : values()) {
            if (type != BASE && type.cured() == kobold) {
                return type;
            }
        }
        return BASE;
    }
}
