package info.mudbourn.mmsdungeons.mobs.orpiment;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.item.ItemStack;

public final class OrpimentDataSerializers {

    public static final EntityDataSerializer<List<ItemStack>> ITEMS = EntityDataSerializer.forValueType(ItemStack.OPTIONAL_LIST_STREAM_CODEC);

    private OrpimentDataSerializers() {
    }

    public static void register() {
        FabricEntityDataRegistry.register(MmsDungeons.id("orpiment_items"), ITEMS);
    }
}
