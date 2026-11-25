package info.mudbourn.mmsdungeons.client.guild;

import info.mudbourn.mmsdungeons.guild.BountyView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Hands the postings the server sends to whichever screen is open.
public final class BountySync {

    public interface Listener {
        void onBounties(Optional<UUID> guild, List<BountyView> views);
    }

    private static Listener listener;

    private BountySync() {
    }

    public static void set(Listener next) {
        listener = next;
    }

    public static void clear(Listener old) {
        if (listener == old) {
            listener = null;
        }
    }

    static void push(Optional<UUID> guild, List<BountyView> views) {
        if (listener != null) {
            listener.onBounties(guild, views);
        }
    }
}
