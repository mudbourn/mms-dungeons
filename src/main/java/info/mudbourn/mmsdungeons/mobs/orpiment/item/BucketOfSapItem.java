package info.mudbourn.mmsdungeons.mobs.orpiment.item;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// A bucket holding a Sap, released on the block it is used on.
public class BucketOfSapItem extends Item {

    public BucketOfSapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos pos = hit.getBlockPos().above();
            OrpimentMobs.SAP.spawn(server, stack, player, pos, EntitySpawnReason.BUCKET, false, false);
            level.playSound(null, pos, OrpimentSounds.SAP_EMPTY, SoundSource.NEUTRAL, 1.0f, 1.0f);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
        }
        return InteractionResult.SUCCESS;
    }
}
