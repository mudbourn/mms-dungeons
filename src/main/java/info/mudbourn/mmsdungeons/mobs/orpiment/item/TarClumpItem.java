package info.mudbourn.mmsdungeons.mobs.orpiment.item;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentMobs;
import info.mudbourn.mmsdungeons.mobs.orpiment.entity.TarClumpProjectile;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

// Throws a clump of tar that leaves its target Tarred.
public class TarClumpItem extends Item implements ProjectileItem {

    private static final float POWER = 0.7f;
    private static final int COOLDOWN = 40;

    public TarClumpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.SNOWBALL_THROW,
            SoundSource.PLAYERS,
            1.0f,
            1.0f / (level.getRandom().nextFloat() * 0.5f + 1.0f) + POWER / 2.0f
        );
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation(
                (world, owner, item) -> new TarClumpProjectile(OrpimentMobs.TAR_CLUMP_PROJECTILE, owner, world, item),
                server,
                stack,
                player,
                0.0f,
                POWER * 2.0f,
                0.0f
            );
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return new TarClumpProjectile(OrpimentMobs.TAR_CLUMP_PROJECTILE, position.x(), position.y(), position.z(), level, stack.copyWithCount(1));
    }
}
