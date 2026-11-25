package info.mudbourn.mmsdungeons.mobs.orpiment.block.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.CrawEggBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Counts down each stage of the egg, twice as fast on lignite.
public class CrawEggBlockEntity extends BlockEntity {

    public static final int STAGE_TICKS = 8000;

    private int remaining = STAGE_TICKS;

    public CrawEggBlockEntity(BlockPos pos, BlockState state) {
        super(OrpimentBlocks.CRAW_EGG_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrawEggBlockEntity egg) {
        egg.remaining -= level.getBlockState(pos.below()).is(OrpimentBlocks.LIGNITE) ? 2 : 1;
        if (egg.remaining > 0) {
            egg.setChanged();
            return;
        }
        egg.remaining = STAGE_TICKS;
        int hatch = state.getValue(CrawEggBlock.HATCH);
        if (hatch < 2) {
            level.setBlock(pos, state.setValue(CrawEggBlock.HATCH, hatch + 1), 3);
            level.playSound(null, pos, OrpimentSounds.CRAW_EGG_CRACK, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (level instanceof ServerLevel server && OrpimentMobs.CRAW.spawn(server, pos, EntitySpawnReason.MOB_SUMMONED) instanceof AgeableMob craw) {
            craw.setAge(-24000);
        }
        level.playSound(null, pos, OrpimentSounds.CRAW_EGG_HATCH, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.remaining = input.getIntOr("Remaining", STAGE_TICKS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Remaining", this.remaining);
    }
}
