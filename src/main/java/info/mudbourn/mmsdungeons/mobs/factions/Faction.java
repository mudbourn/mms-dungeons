package info.mudbourn.mmsdungeons.mobs.factions;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// The three human factions, weakest first, with the skins each draws from.
public enum Faction {
    BANDIT("bandit", skins("bandit", 9), 3, 2, 4, false),
    MERCENARY("mercenary", concat(skins("bandit", 9), skins("dark_guildsman", 8)), 3, 3, 5, true),
    GUILDSMAN("dark_guildsman", skins("dark_guildsman", 8), 3, 3, 5, true);

    private final String id;
    private final List<String> skins;
    private final int tiers;
    private final int minGroup;
    private final int maxGroup;
    private final boolean led;

    Faction(String id, List<String> skins, int tiers, int minGroup, int maxGroup, boolean led) {
        this.id = id;
        this.skins = skins;
        this.tiers = tiers;
        this.minGroup = minGroup;
        this.maxGroup = maxGroup;
        this.led = led;
    }

    public String id() {
        return this.id;
    }

    public List<String> skins() {
        return this.skins;
    }

    // Ordinary gear tiers, not counting the lunar guildsmen.
    public int tiers() {
        return this.tiers;
    }

    public int minGroup() {
        return this.minGroup;
    }

    public int maxGroup() {
        return this.maxGroup;
    }

    // Whether a group has a leader who picks its targets.
    public boolean led() {
        return this.led;
    }

    private static List<String> skins(String prefix, int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> prefix + "_" + i).toList();
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
