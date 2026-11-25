package info.mudbourn.mmsdungeons.party;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Every party and the last known name of each member, saved with the world.
public final class PartyData extends SavedData {

    public static final int MAX_SIZE = 5;

    public static final Codec<PartyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Party.CODEC.listOf().fieldOf("parties").forGetter(data -> new ArrayList<>(data.parties)),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("names").forGetter(data -> data.names)
    ).apply(instance, PartyData::new));

    public static final SavedDataType<PartyData> TYPE = new SavedDataType<>(
        MmsDungeons.id("parties"),
        PartyData::new,
        CODEC,
        DataFixTypes.LEVEL);

    private final List<Party> parties = new ArrayList<>();
    private final Map<String, String> names = new HashMap<>();
    private final Map<UUID, Party> byMember = new HashMap<>();

    public PartyData() {
    }

    private PartyData(List<Party> loaded, Map<String, String> loadedNames) {
        parties.addAll(loaded);
        names.putAll(loadedNames);
        for (Party party : parties) {
            for (UUID member : party.members) {
                byMember.put(member, party);
            }
        }
    }

    // A group of up to five players with one leader.
    public static final class Party {

        public static final Codec<Party> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("leader").forGetter(party -> party.leader),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(party -> party.members)
        ).apply(instance, Party::new));

        public UUID leader;
        public final List<UUID> members;

        private Party(UUID leader, List<UUID> members) {
            this.leader = leader;
            this.members = new ArrayList<>(members);
        }

        public boolean isFull() {
            return members.size() >= MAX_SIZE;
        }
    }

    public Party partyOf(UUID player) {
        return byMember.get(player);
    }

    public boolean sameParty(UUID a, UUID b) {
        Party party = byMember.get(a);
        return party != null && party == byMember.get(b);
    }

    public Party create(UUID leader) {
        Party party = new Party(leader, List.of(leader));
        parties.add(party);
        byMember.put(leader, party);
        setDirty();
        return party;
    }

    public void add(Party party, UUID player) {
        party.members.add(player);
        byMember.put(player, party);
        setDirty();
    }

    // Removes a player, hands leadership on, and disbands a party left with one member.
    public void remove(UUID player) {
        Party party = byMember.remove(player);
        if (party == null) {
            return;
        }
        party.members.remove(player);
        if (party.members.size() <= 1) {
            for (UUID member : party.members) {
                byMember.remove(member);
            }
            parties.remove(party);
        } else if (party.leader.equals(player)) {
            party.leader = party.members.getFirst();
        }
        setDirty();
    }

    public void rememberName(UUID player, String name) {
        if (!name.equals(names.put(player.toString(), name))) {
            setDirty();
        }
    }

    public String nameOf(UUID player) {
        return names.getOrDefault(player.toString(), player.toString());
    }

    public UUID findByName(Party party, String name) {
        for (UUID member : party.members) {
            if (nameOf(member).equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }
}
