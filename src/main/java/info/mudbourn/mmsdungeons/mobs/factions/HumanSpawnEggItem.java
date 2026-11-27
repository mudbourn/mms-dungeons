package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

// One egg for every human: it hatches a bandit, a mercenary or a dark guildsman at random.
public class HumanSpawnEggItem extends Item {

    public HumanSpawnEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        ItemStack stack = context.getItemInHand();
        List<EntityType<FactionMember>> types = HumanFactions.types();
        EntityType<FactionMember> type = types.get(level.getRandom().nextInt(types.size()));
        boolean lift = !clicked.equals(pos) && face == Direction.UP;
        if (type.spawn(level, stack, context.getPlayer(), pos, EntitySpawnReason.SPAWN_ITEM_USE, true, lift) != null) {
            stack.consume(1, context.getPlayer());
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.SUCCESS;
    }
}
