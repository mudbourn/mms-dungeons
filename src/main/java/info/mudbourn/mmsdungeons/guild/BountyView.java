package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// A quest or contract as one player sees it: names, hold state, the share preview and the days left.
public record BountyView(
    Bounty bounty,
    String guildName,
    String posterName,
    String takerName,
    boolean holding,
    int partySize,
    long yourShare,
    long commission,
    int have,
    long daysLeft
) {

    public static final Codec<BountyView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Bounty.VIEW_CODEC.fieldOf("bounty").forGetter(BountyView::bounty),
        Codec.STRING.fieldOf("guildName").forGetter(BountyView::guildName),
        Codec.STRING.fieldOf("posterName").forGetter(BountyView::posterName),
        Codec.STRING.fieldOf("takerName").forGetter(BountyView::takerName),
        Codec.BOOL.fieldOf("holding").forGetter(BountyView::holding),
        Codec.INT.fieldOf("partySize").forGetter(BountyView::partySize),
        Codec.LONG.fieldOf("yourShare").forGetter(BountyView::yourShare),
        Codec.LONG.fieldOf("commission").forGetter(BountyView::commission),
        Codec.INT.fieldOf("have").forGetter(BountyView::have),
        Codec.LONG.fieldOf("daysLeft").forGetter(BountyView::daysLeft)
    ).apply(instance, BountyView::new));
}
