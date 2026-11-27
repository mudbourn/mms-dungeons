package info.mudbourn.mmsdungeons.mobs.fangsnclaws;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

// The Fangs 'n Claws enchantments that items read in code.
public final class FangsNClawsEnchantments {

    public static final ResourceKey<Enchantment> CRITICAL_BACKSTAB = key("critical_backstab");
    public static final ResourceKey<Enchantment> QUICK_KILLER = key("quick_killer");
    public static final ResourceKey<Enchantment> ITEM_CATCHER = key("item_catcher");
    public static final ResourceKey<Enchantment> SCRATCH = key("scratch");

    private FangsNClawsEnchantments() {
    }

    // The level of an enchantment on a stack, or 0.
    public static int level(ItemStack stack, Level level, ResourceKey<Enchantment> enchantment) {
        return level.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .get(enchantment)
            .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
            .orElse(0);
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, MmsDungeons.id(name));
    }
}
