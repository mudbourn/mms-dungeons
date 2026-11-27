package info.mudbourn.mmsdungeons.mobs.kobolds;

import info.mudbourn.mmsdungeons.MmsDungeons;
import info.mudbourn.mmsdungeons.mobs.kobolds.entity.AbstractKobold;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class KoboldEvents {

    private static final ResourceKey<Enchantment> PROSPECTOR = ResourceKey.create(Registries.ENCHANTMENT, MmsDungeons.id("prospector"));
    private static final double EMERALD_CHANCE = 0.05;
    private static final double EMERALD_CHANCE_LUCKY = 0.25;
    private static final List<ResourceKey<Structure>> DENS = List.of(
        ResourceKey.create(Registries.STRUCTURE, MmsDungeons.id("kobold_den")),
        ResourceKey.create(Registries.STRUCTURE, MmsDungeons.id("kobold_den_pirate"))
    );
    // How far the alarm carries through a den when a player loots it.
    private static final double DEFEND_RADIUS = 48.0;

    private KoboldEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KoboldEvents::onDeath);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level instanceof ServerLevel server) {
                defendLoot(server, player, hit.getBlockPos(), level.getBlockEntity(hit.getBlockPos()));
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel server) {
                defendLoot(server, player, pos, blockEntity);
            }
        });
    }

    // Opening or breaking a container inside a kobold den sets every kobold nearby on the looter.
    private static void defendLoot(ServerLevel level, Player player, BlockPos pos, BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container) || player.isCreative() || player.isSpectator() || !inDen(level, pos)) {
            return;
        }
        for (AbstractKobold kobold : level.getEntitiesOfClass(AbstractKobold.class, player.getBoundingBox().inflate(DEFEND_RADIUS))) {
            kobold.setLastHurtByMob(player);
            kobold.setTarget(player);
        }
    }

    private static boolean inDen(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, holder -> DENS.stream().anyMatch(holder::is)).isValid();
    }

    // Prospector gives a small chance of emeralds, one per level, from enemies the wielder kills.
    private static void onDeath(LivingEntity victim, DamageSource source) {
        boolean enemy = victim instanceof Enemy;
        if (!enemy || !(source.getEntity() instanceof LivingEntity killer) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(PROSPECTOR),
            killer.getMainHandItem()
        );
        double chance = killer.hasEffect(MobEffects.LUCK) ? EMERALD_CHANCE_LUCKY : EMERALD_CHANCE;
        if (enchantLevel > 0 && level.getRandom().nextDouble() <= chance) {
            for (int i = 0; i < enchantLevel; i++) {
                level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.EMERALD)));
            }
        }
    }
}
