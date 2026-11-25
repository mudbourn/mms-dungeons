package info.mudbourn.mmsdungeons.mobs.orpiment.block.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentParticles;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.BurstSpawnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BurstSpawnerBlockEntity extends BlockEntity {

    private static final double DETECTION_RANGE = 4.0;
    private static final int SPAWN_RANGE = 4;
    private static final int MAX_ATTEMPTS = 16;

    private String entityTypeId = "";
    private int burstCount = 4;
    private boolean hasFired;

    public BurstSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(OrpimentBlocks.BURST_SPAWNER_ENTITY, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BurstSpawnerBlockEntity spawner) {
        if (spawner.hasFired || spawner.entityTypeId.isEmpty()) {
            return;
        }
        boolean survivalNearby = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(DETECTION_RANGE))
            .stream()
            .anyMatch(player -> !player.getAbilities().instabuild && !player.isSpectator());
        if (!survivalNearby) {
            return;
        }
        Identifier id = Identifier.tryParse(spawner.entityTypeId);
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            return;
        }
        spawner.hasFired = true;
        spawner.setChanged();
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, OrpimentSounds.BURST_SPAWNER_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.0f);
        for (int i = 0; i < spawner.burstCount; i++) {
            spawner.spawnOne(level, pos, type);
        }
        level.setBlock(pos, state.setValue(BurstSpawnerBlock.FIRED, false), 3);
    }

    private void spawnOne(ServerLevel level, BlockPos pos, EntityType<?> type) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - random.nextDouble()) * SPAWN_RANGE;
            double y = pos.getY() + random.nextInt(3) - 1;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - random.nextDouble()) * SPAWN_RANGE;
            if (!level.noCollision(type.getSpawnAABB(x, y, z))) {
                continue;
            }
            Entity entity = type.create(level, EntitySpawnReason.SPAWNER);
            if (entity == null) {
                continue;
            }
            entity.snapTo(x, y, z, random.nextFloat() * 360.0f, 0.0f);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
            }
            if (level.tryAddFreshEntityWithPassengers(entity)) {
                for (int p = 0; p < 5 + random.nextInt(6); p++) {
                    level.sendParticles(
                        OrpimentParticles.SPARK,
                        x + (random.nextDouble() - 0.5) * 1.5,
                        y + 0.5 + (random.nextDouble() - 0.5) * 1.5,
                        z + (random.nextDouble() - 0.5) * 1.5,
                        1,
                        (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.5,
                        0.1
                    );
                }
                level.levelEvent(2004, entity.blockPosition(), 0);
                return;
            }
        }
    }

    public void setEntityTypeId(String id) {
        this.entityTypeId = id;
        this.hasFired = false;
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.setBlock(this.worldPosition, state.setValue(BurstSpawnerBlock.FIRED, true), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.entityTypeId = input.getStringOr("EntityTypeId", "");
        this.burstCount = input.getIntOr("BurstCount", 4);
        this.hasFired = input.getBooleanOr("HasFired", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("EntityTypeId", this.entityTypeId);
        output.putInt("BurstCount", this.burstCount);
        output.putBoolean("HasFired", this.hasFired);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
