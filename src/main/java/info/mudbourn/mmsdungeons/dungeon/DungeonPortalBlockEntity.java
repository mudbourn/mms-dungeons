package info.mudbourn.mmsdungeons.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// Gives the dungeon portal the end portal's starfield, drawn on every face that does not touch more portal.
public class DungeonPortalBlockEntity extends TheEndPortalBlockEntity {

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(DungeonBlocks.DUNGEON_PORTAL_ENTITY, pos, state);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        if (this.level == null) {
            return true;
        }
        return !this.level.getBlockState(this.worldPosition.relative(direction)).is(DungeonBlocks.DUNGEON_PORTAL);
    }
}
