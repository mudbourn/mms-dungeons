package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmseconomy.deal.Spot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

// One guild: its people, charter, depot, money, portal, banished players and Open Parties and Claims party.
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
        GlobalPos.CODEC.optionalFieldOf("portal").forGetter(guild -> Optional.ofNullable(guild.portal)),
        UUIDUtil.CODEC.optionalFieldOf("opacParty").forGetter(guild -> Optional.ofNullable(guild.opacParty)),
        Codec.INT.fieldOf("commissionPercent").forGetter(guild -> guild.commissionPercent),
        UUIDUtil.CODEC.listOf().optionalFieldOf("banished", List.of()).forGetter(guild -> guild.banished),
        UUIDUtil.CODEC.listOf().optionalFieldOf("moveOut", List.of()).forGetter(guild -> guild.moveOut),
        Codec.INT.optionalFieldOf("claimSub", -1).forGetter(guild -> guild.claimSub)
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
    public GlobalPos portal;
    public UUID opacParty;
    public int commissionPercent;
    public final List<UUID> banished;
    public final List<UUID> moveOut;
    public int claimSub;

    public Guild(UUID id, String name, boolean isPublic, UUID master, int commissionPercent) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.master = master;
        this.members = new ArrayList<>();
        this.banished = new ArrayList<>();
        this.moveOut = new ArrayList<>();
        this.claimSub = -1;
        this.charter = "";
        this.commissionPercent = commissionPercent;
        if (master != null) {
            members.add(master);
        }
    }

    private Guild(UUID id, String name, boolean isPublic, Optional<UUID> master, List<UUID> members, String charter,
                  long joinFee, Optional<Spot> depot, long treasury, Optional<GlobalPos> portal,
                  Optional<UUID> opacParty, int commissionPercent, List<UUID> banished, List<UUID> moveOut,
                  int claimSub) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.master = master.orElse(null);
        this.members = new ArrayList<>(members);
        this.charter = charter;
        this.joinFee = joinFee;
        this.depot = depot.orElse(null);
        this.treasury = treasury;
        this.portal = portal.orElse(null);
        this.opacParty = opacParty.orElse(null);
        this.commissionPercent = commissionPercent;
        this.banished = new ArrayList<>(banished);
        this.moveOut = new ArrayList<>(moveOut);
        this.claimSub = claimSub;
    }

    public boolean has(UUID player) {
        return members.contains(player);
    }
}
