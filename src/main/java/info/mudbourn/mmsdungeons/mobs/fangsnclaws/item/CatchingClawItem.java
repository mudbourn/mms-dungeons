package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.CatchingClawHook;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

// A grappling claw: throw it to hook a target, use it again to reel the target in toward your aim.
public class CatchingClawItem extends Item {

    private static final String HOOK_KEY = "HookUUID";
    private static final int RETRIEVE_COST = 1;
    private static final int HOOKED_COST = 3;

    private final boolean netherite;

    public CatchingClawItem(Properties properties, boolean netherite) {
        super(properties);
        this.netherite = netherite;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.FISHING_BOBBER_THROW,
            SoundSource.NEUTRAL,
            0.5f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f)
        );
        if (level instanceof ServerLevel server) {
            Entity existing = hookId(stack) == null ? null : server.getEntity(hookId(stack));
            if (existing instanceof CatchingClawHook hook && hook.isAlive()) {
                int cost = hook.isHooked() ? HOOKED_COST : RETRIEVE_COST;
                hook.retrieve(player);
                stack.remove(DataComponents.CUSTOM_DATA);
                stack.hurtAndBreak(cost, player, hand);
                return InteractionResult.SUCCESS;
            }
            CatchingClawHook hook = new CatchingClawHook(player, server, this.netherite);
            float velocity = this.netherite ? 1.8f : 1.5f;
            float inaccuracy = this.netherite ? 0.8f : 1.0f;
            hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
            server.addFreshEntity(hook);
            stack.set(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY.update(tag -> tag.store(HOOK_KEY, UUIDUtil.CODEC, hook.getUUID()))
            );
        }
        return InteractionResult.SUCCESS;
    }

    private static UUID hookId(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .read(HOOK_KEY, UUIDUtil.CODEC)
            .orElse(null);
    }
}
