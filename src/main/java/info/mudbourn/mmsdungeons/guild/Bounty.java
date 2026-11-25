package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmseconomy.deal.Spot;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

// A quest or contract: what is wanted, the escrowed reward, who posted it and who holds it.
public final class Bounty {

    // The proof item template and how many of it must be handed in.
    public record Proof(ItemStack stack, int count) {

        public static final Codec<Proof> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(Proof::stack),
            Codec.INT.fieldOf("count").forGetter(Proof::count)
        ).apply(instance, Proof::new));
    }

    public static final Codec<Bounty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(bounty -> bounty.id),
        BountyKind.CODEC.fieldOf("kind").forGetter(bounty -> bounty.kind),
        UUIDUtil.CODEC.fieldOf("guild").forGetter(bounty -> bounty.guild),
        UUIDUtil.CODEC.fieldOf("poster").forGetter(bounty -> bounty.poster),
        Codec.STRING.fieldOf("title").forGetter(bounty -> bounty.title),
        Codec.STRING.fieldOf("description").forGetter(bounty -> bounty.description),
        Proof.CODEC.fieldOf("proof").forGetter(bounty -> bounty.proof),
        Codec.LONG.fieldOf("reward").forGetter(bounty -> bounty.reward),
        BountyType.CODEC.fieldOf("type").forGetter(bounty -> bounty.type),
        BountyStatus.CODEC.fieldOf("status").forGetter(bounty -> bounty.status),
        UUIDUtil.CODEC.optionalFieldOf("taker").forGetter(bounty -> Optional.ofNullable(bounty.taker)),
        Codec.LONG.fieldOf("createdDay").forGetter(bounty -> bounty.createdDay),
        Codec.LONG.fieldOf("expiryDay").forGetter(bounty -> bounty.expiryDay),
        Codec.LONG.fieldOf("finishedDay").forGetter(bounty -> bounty.finishedDay),
        Spot.CODEC.optionalFieldOf("posterDepot").forGetter(bounty -> Optional.ofNullable(bounty.posterDepot))
    ).apply(instance, Bounty::new));

    // The codec for clients: it leaves out the poster depot.
    public static final Codec<Bounty> VIEW_CODEC = CODEC.xmap(Function.identity(), Bounty::withoutDepot);

    public final UUID id;
    public final BountyKind kind;
    public final UUID guild;
    public final UUID poster;
    public final String title;
    public final String description;
    public final Proof proof;
    public final long reward;
    public final BountyType type;
    public BountyStatus status;
    public UUID taker;
    public final long createdDay;
    public final long expiryDay;
    public long finishedDay;
    public final Spot posterDepot;

    public Bounty(
        UUID guild,
        BountyKind kind,
        UUID poster,
        String title,
        String description,
        Proof proof,
        long reward,
        BountyType type,
        long createdDay,
        long expiryDay,
        Spot posterDepot
    ) {
        this.id = UUID.randomUUID();
        this.kind = kind;
        this.guild = guild;
        this.poster = poster;
        this.title = title;
        this.description = description;
        this.proof = proof;
        this.reward = reward;
        this.type = type;
        this.status = BountyStatus.AVAILABLE;
        this.createdDay = createdDay;
        this.expiryDay = expiryDay;
        this.posterDepot = posterDepot;
    }

    private Bounty(
        UUID id,
        BountyKind kind,
        UUID guild,
        UUID poster,
        String title,
        String description,
        Proof proof,
        long reward,
        BountyType type,
        BountyStatus status,
        Optional<UUID> taker,
        long createdDay,
        long expiryDay,
        long finishedDay,
        Optional<Spot> posterDepot
    ) {
        this.id = id;
        this.kind = kind;
        this.guild = guild;
        this.poster = poster;
        this.title = title;
        this.description = description;
        this.proof = proof;
        this.reward = reward;
        this.type = type;
        this.status = status;
        this.taker = taker.orElse(null);
        this.createdDay = createdDay;
        this.expiryDay = expiryDay;
        this.finishedDay = finishedDay;
        this.posterDepot = posterDepot.orElse(null);
    }

    private Bounty withoutDepot() {
        return new Bounty(
            id,
            kind,
            guild,
            poster,
            title,
            description,
            proof,
            reward,
            type,
            status,
            Optional.ofNullable(taker),
            createdDay,
            expiryDay,
            finishedDay,
            Optional.empty()
        );
    }

    public boolean open() {
        return status == BountyStatus.AVAILABLE || status == BountyStatus.IN_PROGRESS;
    }
}
