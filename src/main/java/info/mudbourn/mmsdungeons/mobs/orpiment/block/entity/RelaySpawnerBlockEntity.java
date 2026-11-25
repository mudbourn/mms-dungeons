package info.mudbourn.mmsdungeons.mobs.orpiment.block.entity;

import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentBlocks;
import info.mudbourn.mmsdungeons.mobs.orpiment.OrpimentSounds;
import info.mudbourn.mmsdungeons.mobs.orpiment.block.RelaySpawnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class RelaySpawnerBlockEntity extends BlockEntity {

    private static final int SEARCH_INTERVAL = 40;
    private static final int SEARCH_RADIUS = 16;

    private @Nullable BlockPos linked;
    private String entityTypeId = "";
    private int spawnDelay = 20;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;
    private int searchTimer;

    public RelaySpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(OrpimentBlocks.RELAY_SPAWNER_ENTITY, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, RelaySpawnerBlockEntity relay) {
        if (!state.getValue(RelaySpawnerBlock.LINKED)) {
            if (++relay.searchTimer >= SEARCH_INTERVAL) {
                relay.searchTimer = 0;
                relay.tryLink(level, pos, state);
            }
            return;
        }
        if (relay.linked == null || !(level.getBlockEntity(relay.linked) instanceof SpawnerBlockEntity)) {
            relay.deactivate(level, pos, state);
            return;
        }
        if (relay.entityTypeId.isEmpty() || !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, relay.requiredPlayerRange)) {
            return;
        }
        if (relay.spawnDelay > 0) {
            relay.spawnDelay--;
            return;
        }
        relay.spawn(level, pos);
    }

    private void spawn(ServerLevel level, BlockPos pos) {
        Identifier id = Identifier.tryParse(this.entityTypeId);
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            this.resetDelay(level);
            return;
        }
        int nearby = level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(this.spawnRange), entity -> entity.getType() == type).size();
        if (nearby >= this.maxNearbyEntities) {
            this.resetDelay(level);
            return;
        }
        RandomSource random = level.getRandom();
        boolean spawned = false;
        for (int i = 0; i < this.spawnCount; i++) {
            double x = pos.getX() + (random.nextDouble() - random.nextDouble()) * this.spawnRange + 0.5;
            double y = pos.getY() + random.nextInt(3) - 1;
            double z = pos.getZ() + (random.nextDouble() - random.nextDouble()) * this.spawnRange + 0.5;
            BlockPos spawnPos = BlockPos.containing(x, y, z);
            if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.SPAWNER, spawnPos, random) || !level.noCollision(type.getSpawnAABB(x, y, z))) {
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
                level.levelEvent(2004, entity.blockPosition(), 0);
                if (entity instanceof Mob mob) {
                    mob.spawnAnim();
                }
                spawned = true;
            }
        }
        if (spawned) {
            this.resetDelay(level);
        }
    }

    private void tryLink(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS), pos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
            if (level.isLoaded(candidate) && level.getBlockState(candidate).is(Blocks.SPAWNER) && candidate.distSqr(pos) < bestDistance) {
                bestDistance = candidate.distSqr(pos);
                best = candidate.immutable();
            }
        }
        if (best == null || !(level.getBlockEntity(best) instanceof SpawnerBlockEntity spawner)) {
            return;
        }
        Entity display = spawner.getSpawner().getOrCreateDisplayEntity(level, best);
        if (display == null) {
            return;
        }
        this.linked = best;
        this.entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(display.getType()).toString();
        this.mirrorTiming(level, spawner);
        level.setBlock(pos, state.setValue(RelaySpawnerBlock.LINKED, true), 3);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, OrpimentSounds.RELAY_SPAWNER_CONNECT, SoundSource.BLOCKS, 1.0f, 1.0f);
        this.setChanged();
    }

    private void mirrorTiming(ServerLevel level, SpawnerBlockEntity spawner) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        spawner.getSpawner().save(output);
        CompoundTag tag = output.buildResult();
        this.minSpawnDelay = tag.getShortOr("MinSpawnDelay", (short) this.minSpawnDelay);
        this.maxSpawnDelay = tag.getShortOr("MaxSpawnDelay", (short) this.maxSpawnDelay);
        this.spawnCount = tag.getShortOr("SpawnCount", (short) this.spawnCount);
        this.maxNearbyEntities = tag.getShortOr("MaxNearbyEntities", (short) this.maxNearbyEntities);
        this.requiredPlayerRange = tag.getShortOr("RequiredPlayerRange", (short) this.requiredPlayerRange);
        this.spawnRange = tag.getShortOr("SpawnRange", (short) this.spawnRange);
    }

    private void deactivate(ServerLevel level, BlockPos pos, BlockState state) {
        this.linked = null;
        this.entityTypeId = "";
        this.spawnDelay = 20;
        level.setBlock(pos, state.setValue(RelaySpawnerBlock.LINKED, false), 3);
        this.setChanged();
    }

    private void resetDelay(ServerLevel level) {
        this.spawnDelay = this.maxSpawnDelay <= this.minSpawnDelay
            ? this.minSpawnDelay
            : this.minSpawnDelay + level.getRandom().nextInt(this.maxSpawnDelay - this.minSpawnDelay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.entityTypeId = input.getStringOr("EntityTypeId", "");
        this.spawnDelay = input.getIntOr("SpawnDelay", 20);
        this.minSpawnDelay = input.getIntOr("MinSpawnDelay", 200);
        this.maxSpawnDelay = input.getIntOr("MaxSpawnDelay", 800);
        this.spawnCount = input.getIntOr("SpawnCount", 4);
        this.maxNearbyEntities = input.getIntOr("MaxNearbyEntities", 6);
        this.requiredPlayerRange = input.getIntOr("RequiredPlayerRange", 16);
        this.spawnRange = input.getIntOr("SpawnRange", 4);
        this.linked = input.getInt("LinkedX").map(x -> new BlockPos(x, input.getIntOr("LinkedY", 0), input.getIntOr("LinkedZ", 0))).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("EntityTypeId", this.entityTypeId);
        output.putInt("SpawnDelay", this.spawnDelay);
        output.putInt("MinSpawnDelay", this.minSpawnDelay);
        output.putInt("MaxSpawnDelay", this.maxSpawnDelay);
        output.putInt("SpawnCount", this.spawnCount);
        output.putInt("MaxNearbyEntities", this.maxNearbyEntities);
        output.putInt("RequiredPlayerRange", this.requiredPlayerRange);
        output.putInt("SpawnRange", this.spawnRange);
        if (this.linked != null) {
            output.putInt("LinkedX", this.linked.getX());
            output.putInt("LinkedY", this.linked.getY());
            output.putInt("LinkedZ", this.linked.getZ());
        }
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
