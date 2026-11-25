package info.mudbourn.mmsdungeons.guild;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Remembers which guild a quest board belongs to.
public class QuestBoardBlockEntity extends BlockEntity {

    private UUID guild;

    public QuestBoardBlockEntity(BlockPos pos, BlockState state) {
        super(GuildBlocks.QUEST_BOARD_ENTITY, pos, state);
    }

    public UUID guild() {
        return guild;
    }

    public void bind(UUID guildId) {
        this.guild = guildId;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (guild != null) {
            output.store("guild", UUIDUtil.CODEC, guild);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        guild = input.read("guild", UUIDUtil.CODEC).orElse(null);
    }
}
