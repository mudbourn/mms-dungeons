package info.mudbourn.mmsdungeons.party;

import info.mudbourn.mmsdungeons.net.PartyMember;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The client's copy of its own party and of the teammates currently under attack.
public final class PartyView {

    private static volatile List<PartyMember> members = List.of();
    private static volatile Set<UUID> ids = Set.of();
    private static final Map<UUID, Long> ALERTS = new HashMap<>();

    private PartyView() {
    }

    public static void update(List<PartyMember> synced) {
        members = List.copyOf(synced);
        ids = synced.stream().map(PartyMember::id).collect(Collectors.toUnmodifiableSet());
    }

    public static void clear() {
        members = List.of();
        ids = Set.of();
        ALERTS.clear();
    }

    public static List<PartyMember> members() {
        return members;
    }

    public static boolean isMember(UUID id) {
        return ids.contains(id);
    }

    public static PartyMember find(UUID id) {
        for (PartyMember member : members) {
            if (member.id().equals(id)) {
                return member;
            }
        }
        return null;
    }

    public static void alert(UUID victim, long untilMillis) {
        ALERTS.put(victim, untilMillis);
    }

    // The most recent live alert as victim id to expiry, or null; expired alerts are dropped.
    public static Map.Entry<UUID, Long> latestAlert(long nowMillis) {
        ALERTS.values().removeIf(until -> until < nowMillis);
        Map.Entry<UUID, Long> best = null;
        for (Map.Entry<UUID, Long> entry : ALERTS.entrySet()) {
            if (best == null || entry.getValue() > best.getValue()) {
                best = entry;
            }
        }
        return best;
    }
}
