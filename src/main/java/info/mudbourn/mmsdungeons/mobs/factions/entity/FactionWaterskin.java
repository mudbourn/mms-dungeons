package info.mudbourn.mmsdungeons.mobs.factions.entity;

import com.github.thedeathlycow.scorchful.item.WaterSkinItem;
import com.github.thedeathlycow.scorchful.item.component.DrinkContainer;
import com.github.thedeathlycow.scorchful.registry.SItems;
import com.github.thedeathlycow.scorchful.registry.SSoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Reads and fills Scorchful waterskins for faction humans.
public final class FactionWaterskin {

    private FactionWaterskin() {
    }

    public static ItemStack filled() {
        ItemStack skin = SItems.WATER_SKIN.getDefaultInstance();
        DrinkContainer.fillCompletely(skin);
        return skin;
    }

    public static boolean hasDrink(ItemStack skin) {
        return WaterSkinItem.hasDrink(skin);
    }

    // Adds drinks to the skin and plays the fill sound, topping it up to full at most.
    public static void fill(ServerLevel level, LivingEntity filler, ItemStack skin, int drinks) {
        DrinkContainer.addDrinks(skin, drinks);
        level.playSound(null, filler.blockPosition(), SSoundEvents.ITEM_WATER_SKIN_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    public static void fillCompletely(ServerLevel level, LivingEntity filler, ItemStack skin) {
        DrinkContainer.fillCompletely(skin);
        level.playSound(null, filler.blockPosition(), SSoundEvents.ITEM_WATER_SKIN_FILL, SoundSource.NEUTRAL, 1.0f, 1.0f);
    }
}
