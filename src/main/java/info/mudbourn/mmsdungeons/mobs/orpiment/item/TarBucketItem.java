package info.mudbourn.mmsdungeons.mobs.orpiment.item;

import info.mudbourn.mmsdungeons.mobs.orpiment.block.TarCauldronBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

public class TarBucketItem extends BucketItem {

    public TarBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.CAULDRON)) {
            return TarCauldronBlock.fill(
                context.getLevel(),
                context.getClickedPos(),
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand()
            );
        }
        return super.useOn(context);
    }
}
