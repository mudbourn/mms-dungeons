package info.mudbourn.mmsdungeons.relations;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

// How one kind of creature sees another.
public enum Relation {
    HOSTILE,
    NEUTRAL,
    ALLIED;

    public static @Nullable Relation parse(String name) {
        for (Relation relation : values()) {
            if (relation.name().equals(name.toUpperCase(Locale.ROOT))) {
                return relation;
            }
        }
        return null;
    }
}
