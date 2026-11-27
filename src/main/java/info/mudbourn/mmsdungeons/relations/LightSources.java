package info.mudbourn.mmsdungeons.relations;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Tells whether a creature carries a light in a hand or, for players, in the Trinkets belt slot.
public final class LightSources {

    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, MmsDungeons.id("light_sources"));

    private static final boolean TRINKETS = FabricLoader.getInstance().isModLoaded("trinkets");
    private static final Map<Item, Boolean> EMITTERS = new ConcurrentHashMap<>();

    private LightSources() {
    }

    public static boolean carries(LivingEntity entity) {
        if (isLight(entity.getMainHandItem()) || isLight(entity.getOffhandItem())) {
            return true;
        }
        return TRINKETS && entity instanceof Player && BeltLight.carries(entity);
    }

    public static boolean isLight(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(TAG) || EMITTERS.computeIfAbsent(stack.getItem(), LightSources::emits));
    }

    private static boolean emits(Item item) {
        return item instanceof BlockItem block && block.getBlock().defaultBlockState().getLightEmission() > 0;
    }
}
