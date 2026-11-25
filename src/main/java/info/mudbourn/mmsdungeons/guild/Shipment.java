package info.mudbourn.mmsdungeons.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmseconomy.deal.Spot;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

// Proof items taken from a guild depot, held until the next day and then inserted into the poster's depot.
public final class Shipment {

    public static final Codec<Shipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.CODEC.fieldOf("recipient").forGetter(shipment -> shipment.recipient),
        Spot.CODEC.fieldOf("depot").forGetter(shipment -> shipment.depot),
        ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(shipment -> shipment.stack),
        Codec.INT.fieldOf("count").forGetter(shipment -> shipment.count),
        Codec.LONG.fieldOf("arrivalDay").forGetter(shipment -> shipment.arrivalDay)
    ).apply(instance, Shipment::new));

    public final UUID recipient;
    public Spot depot;
    public final ItemStack stack;
    public int count;
    public final long arrivalDay;

    public Shipment(UUID recipient, Spot depot, ItemStack stack, int count, long arrivalDay) {
        this.recipient = recipient;
        this.depot = depot;
        this.stack = stack;
        this.count = count;
        this.arrivalDay = arrivalDay;
    }
}
