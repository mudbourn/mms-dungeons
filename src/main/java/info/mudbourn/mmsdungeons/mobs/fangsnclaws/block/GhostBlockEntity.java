package info.mudbourn.mmsdungeons.mobs.fangsnclaws.block;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.FangsNClawsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Holds the block a Ghost Block looks like.
public class GhostBlockEntity extends BlockEntity {

    private static final String MIMIC = "Mimic";

    private @Nullable BlockState mimic;

    public GhostBlockEntity(BlockPos pos, BlockState state) {
        super(FangsNClawsBlocks.GHOST_BLOCK_ENTITY, pos, state);
    }

    public @Nullable BlockState getMimic() {
        return this.mimic;
    }

    public void setMimic(@Nullable BlockState mimic) {
        this.mimic = mimic;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.mimic = input.read(MIMIC, BlockState.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable(MIMIC, BlockState.CODEC, this.mimic);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
