package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmseconomy.deal.Spot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

// One guild: its people, charter, depot, money and Open Parties and Claims party.
public final class Guild {

    public static final Codec<Guild> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(guild -> guild.id),
        Codec.STRING.fieldOf("name").forGetter(guild -> guild.name),
        Codec.BOOL.fieldOf("public").forGetter(guild -> guild.isPublic),
        UUIDUtil.CODEC.optionalFieldOf("master").forGetter(guild -> Optional.ofNullable(guild.master)),
        UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(guild -> guild.members),
        Codec.STRING.fieldOf("charter").forGetter(guild -> guild.charter),
        Codec.LONG.fieldOf("joinFee").forGetter(guild -> guild.joinFee),
        Spot.CODEC.optionalFieldOf("depot").forGetter(guild -> Optional.ofNullable(guild.depot)),
        Codec.LONG.fieldOf("treasury").forGetter(guild -> guild.treasury),
        Codec.BOOL.fieldOf("portalInstalled").forGetter(guild -> guild.portalInstalled),
        UUIDUtil.CODEC.optionalFieldOf("opacParty").forGetter(guild -> Optional.ofNullable(guild.opacParty)),
        Codec.INT.fieldOf("commissionPercent").forGetter(guild -> guild.commissionPercent)
    ).apply(instance, Guild::new));

    public final UUID id;
    public String name;
    public final boolean isPublic;
    public UUID master;
    public final List<UUID> members;
    public String charter;
    public long joinFee;
    public Spot depot;
    public long treasury;
    public boolean portalInstalled;
    public UUID opacParty;
    public int commissionPercent;

    public Guild(UUID id, String name, boolean isPublic, UUID master, int commissionPercent) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.master = master;
        this.members = new ArrayList<>();
        this.charter = "";
        this.commissionPercent = commissionPercent;
        if (master != null) {
            members.add(master);
        }
    }

    private Guild(UUID id, String name, boolean isPublic, Optional<UUID> master, List<UUID> members, String charter,
                  long joinFee, Optional<Spot> depot, long treasury, boolean portalInstalled,
                  Optional<UUID> opacParty, int commissionPercent) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.master = master.orElse(null);
        this.members = new ArrayList<>(members);
        this.charter = charter;
        this.joinFee = joinFee;
        this.depot = depot.orElse(null);
        this.treasury = treasury;
        this.portalInstalled = portalInstalled;
        this.opacParty = opacParty.orElse(null);
        this.commissionPercent = commissionPercent;
    }

    public boolean has(UUID player) {
        return members.contains(player);
    }
}
