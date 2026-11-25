package info.mudbourn.mmsdungeons.mobs.orpiment.block;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Places a scaffold block in mid-air up to five blocks ahead.
public class EctoplasmScaffoldItem extends BlockItem {

    private static final double REACH = 5.0;

    public EctoplasmScaffoldItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(REACH));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos target = BlockPos.containing(hit.getLocation());
        if (hit.getType() != HitResult.Type.MISS || !level.getBlockState(target).isAir()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.setBlock(target, this.getBlock().defaultBlockState(), 3);
            level.playSound(null, target, OrpimentSounds.ECTOPLASM_SCAFFOLD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
