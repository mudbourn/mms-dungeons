package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsdungeons.mobs.factions.FactionOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// Puts out a burning member as a player would: into water close by, or a water bucket poured at its feet and scooped back up.
final class FactionDouse {

    private static final int WATER_SEARCH = 8;
    private static final int RETHINK_TICKS = 10;
    private static final int SCOOP_TICKS = 12;

    private final FactionMember member;
    private @Nullable BlockPos poured;
    private int scoopIn;

    FactionDouse(FactionMember member) {
        this.member = member;
    }

    void tick(ServerLevel level) {
        if (this.poured != null && --this.scoopIn <= 0) {
            this.scoop(level);
        }
        if (!this.burning() || this.member.tickCount % RETHINK_TICKS != 0) {
            return;
        }
        if (this.poured == null && this.pour(level)) {
            return;
        }
        BlockPos water = this.nearestWater(level);
        if (water != null) {
            this.member.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.3);
        }
    }

    private boolean burning() {
        return this.member.isOnFire()
            && !this.member.fireImmune()
            && !this.member.hasEffect(MobEffects.FIRE_RESISTANCE)
            && this.member.origin() != FactionOrigin.BLAZEBORN
            && !this.member.isInWaterOrRain()
            && !this.member.downed();
    }

    // Pours a carried water bucket where it stands, leaving the bucket in hand to scoop the water back.
    private boolean pour(ServerLevel level) {
        BlockPos feet = this.member.blockPosition();
        BlockState there = level.getBlockState(feet);
        if (!(there.isAir() || there.is(Blocks.FIRE)) || !this.member.drawSupply(stack -> stack.is(Items.WATER_BUCKET))) {
            return false;
        }
        if (!(this.member.getMainHandItem().getItem() instanceof BucketItem bucket)
            || !bucket.emptyContents(this.member, level, feet, null)
            || !level.getFluidState(feet).is(FluidTags.WATER)) {
            this.member.stowTool();
            return false;
        }
        this.member.swing(InteractionHand.MAIN_HAND, this.member.getMainHandItem().getAttackAnimation(), true);
        this.member.clearFire();
        this.poured = feet;
        this.scoopIn = SCOOP_TICKS;
        return true;
    }

    // Takes the poured water back into the bucket, keeping the bucket's other data, and puts it away.
    private void scoop(ServerLevel level) {
        BlockPos at = this.poured;
        this.poured = null;
        ItemStack held = this.member.getMainHandItem();
        if (level.getFluidState(at).isSource() && level.getFluidState(at).is(FluidTags.WATER) && held.is(Items.WATER_BUCKET)) {
            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, at, SoundEvents.BUCKET_FILL, this.member.getSoundSource(), 1.0f, 1.0f);
        } else if (held.is(Items.WATER_BUCKET)) {
            this.member.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BUCKET));
        }
        this.member.stowTool();
    }

    private @Nullable BlockPos nearestWater(ServerLevel level) {
        BlockPos center = this.member.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-WATER_SEARCH, -3, -WATER_SEARCH), center.offset(WATER_SEARCH, 2, WATER_SEARCH))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                double distance = pos.distSqr(center);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }
}
