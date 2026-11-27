package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Every guild, quest and contract, and the shipments in transit, saved with the world.
public final class GuildData extends SavedData {

    public static final Codec<GuildData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Guild.CODEC.listOf().fieldOf("guilds").forGetter(data -> data.guilds),
        Bounty.CODEC.listOf().fieldOf("bounties").forGetter(data -> data.bounties),
        Shipment.CODEC.listOf().fieldOf("shipments").forGetter(data -> data.shipments),
        Codec.LONG.fieldOf("lastDay").forGetter(data -> data.lastDay)
    ).apply(instance, GuildData::new));

    public static final SavedDataType<GuildData> TYPE = new SavedDataType<>(
        MmsDungeons.id("guilds"),
        GuildData::new,
        CODEC,
        DataFixTypes.LEVEL);

    public final List<Guild> guilds = new ArrayList<>();
    public final List<Bounty> bounties = new ArrayList<>();
    public final List<Shipment> shipments = new ArrayList<>();
    public long lastDay = -1L;

    public GuildData() {
    }

    private GuildData(List<Guild> guilds, List<Bounty> bounties, List<Shipment> shipments, long lastDay) {
        this.guilds.addAll(guilds);
        this.bounties.addAll(bounties);
        this.shipments.addAll(shipments);
        this.lastDay = lastDay;
    }

    public Guild byId(UUID id) {
        for (Guild guild : guilds) {
            if (guild.id.equals(id)) {
                return guild;
            }
        }
        return null;
    }

    public Guild byName(String name) {
        String key = Guild.lookupKey(name);
        for (Guild guild : guilds) {
            if (Guild.lookupKey(guild.name).equals(key)) {
                return guild;
            }
        }
        return null;
    }

    public Guild guildOf(UUID player) {
        for (Guild guild : guilds) {
            if (guild.has(player)) {
                return guild;
            }
        }
        return null;
    }

    public Bounty bounty(UUID id) {
        for (Bounty bounty : bounties) {
            if (bounty.id.equals(id)) {
                return bounty;
            }
        }
        return null;
    }

    public List<Bounty> bountiesOf(UUID guild) {
        return bounties.stream().filter(bounty -> bounty.guild.equals(guild)).toList();
    }
}
