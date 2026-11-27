package info.mudbourn.mmsdungeons.mobs.fangsnclaws.item;

import info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity.ThrownTomahawk;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

// An axe that is thrown on use and comes back to its owner.
public class TomahawkItem extends Item implements ProjectileItem {

    private static final int COOLDOWN_TICKS = 100;
    private static final float THROW_VELOCITY = 1.6f;

    public TomahawkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            ThrownTomahawk tomahawk = new ThrownTomahawk(server, player, stack.copyWithCount(1));
            tomahawk.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, THROW_VELOCITY, 1.0f);
            server.addFreshEntity(tomahawk);
            server.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            if (player.hasInfiniteMaterials()) {
                tomahawk.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            } else {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ThrownTomahawk tomahawk = new ThrownTomahawk(
            level,
            position.x(),
            position.y(),
            position.z(),
            stack.copyWithCount(1)
        );
        tomahawk.pickup = AbstractArrow.Pickup.ALLOWED;
        return tomahawk;
    }
}
