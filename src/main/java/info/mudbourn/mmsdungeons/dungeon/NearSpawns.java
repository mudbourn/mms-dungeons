package info.mudbourn.mmsdungeons.dungeon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Brings dungeon monsters to roomy spots near each player that can walk to them, out of sight: full strength in ruins, half in caves, plus drowned and guardians in ruin pools.
public final class NearSpawns {

    private static final int INTERVAL_TICKS = 40;
    private static final int ATTEMPTS = 6;
    private static final int MIN_DISTANCE = 12;
    private static final int MAX_DISTANCE = 28;
    private static final int VERTICAL_RANGE = 12;
    // Ruin halls are tight and their floors sit close together.
    private static final int RUIN_MIN_DISTANCE = 8;
    private static final int RUIN_MAX_DISTANCE = 20;
    private static final int RUIN_VERTICAL_RANGE = 3;
    // Most cells the walk search visits per player.
    private static final int SEARCH_LIMIT = 6000;
    // Radius around a player in which monsters count against the local cap.
    private static final double CAP_RADIUS = 40.0;
    private static final int CAP_AT_ROOF = 4;
    private static final int CAP_AT_FLOOR = 10;
    // Open caves run every other cycle with half the local cap.
    private static final int CAVE_DIVISOR = 2;
    private static final int ROOF_Y = 157;
    private static final int FLOOR_Y = -128;
    // How far out to look for unseen monsters to move closer.
    private static final double GATHER_RADIUS = 96.0;
    // How far a player can be to count as watching.
    private static final double WATCH_RADIUS = 64.0;
    private static final TagKey<Biome> ALEXS_CAVES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("alexscaves", "alexs_caves_biomes"));
    private static final String CAVE_CREATURE = "cave_creature";
    // Most Alex's Caves mobs around a player before more stop coming.
    private static final int ENDEMIC_CAP = 10;
    // Water mobs need a pool this full around their spot.
    private static final int POOL_REACH = 2;
    private static final int POOL_MIN_WATER = 30;
    private static final int SWIMMER_CAP = 3;
    // Drowned are three times as common as guardians.
    private static final int DROWNED_WEIGHT = 3;
    private static final Direction[] FLAT = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private NearSpawns() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(NearSpawns::tick);
    }

    private static void tick(ServerLevel level) {
        if (!DungeonDimension.isDungeon(level) || level.getGameTime() % INTERVAL_TICKS != 0) {
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
            return;
        }
        boolean caveCycle = level.getGameTime() / INTERVAL_TICKS % CAVE_DIVISOR == 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            boolean inRuins = inRuins(level, player);
            int cap = inRuins ? capAt(player.getBlockY()) : capAt(player.getBlockY()) / CAVE_DIVISOR;
            if (!(inRuins || caveCycle) || localMonsters(level, player) >= cap) {
                continue;
            }
            List<BlockPos> spots = reachableSpots(level, player, inRuins);
            if (!spots.isEmpty() && inAlexsCaves(level, player.blockPosition())) {
                spawnEndemic(level, player, spots, level.getRandom());
            } else if (!spots.isEmpty()) {
                gatherOne(level, player, spots, level.getRandom());
                spawnOne(level, spots, level.getRandom(), MobCategory.MONSTER);
            }
            if (inRuins) {
                spawnSwimmer(level, player, level.getRandom());
            }
        }
    }

    private static boolean inRuins(ServerLevel level, ServerPlayer player) {
        return level.structureManager()
            .getStructureWithPieceAt(player.blockPosition(), holder -> holder.unwrapKey()
                .map(key -> DddPlacement.isPlacedDdd(key.identifier()))
                .orElse(false))
            .isValid();
    }

    private static int localMonsters(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(Monster.class, new AABB(player.blockPosition()).inflate(CAP_RADIUS)).size();
    }

    // The local cap rises evenly from the roof to the floor.
    private static int capAt(int y) {
        float depth = Mth.clamp((float) (ROOF_Y - y) / (ROOF_Y - FLOOR_Y), 0.0f, 1.0f);
        return Math.round(Mth.lerp(depth, CAP_AT_ROOF, CAP_AT_FLOOR));
    }

    // Roomy spots in range that a mob can walk to the player from.
    private static List<BlockPos> reachableSpots(ServerLevel level, ServerPlayer player, boolean inRuins) {
        int minDistance = inRuins ? RUIN_MIN_DISTANCE : MIN_DISTANCE;
        int maxDistance = inRuins ? RUIN_MAX_DISTANCE : MAX_DISTANCE;
        int verticalRange = inRuins ? RUIN_VERTICAL_RANGE : VERTICAL_RANGE;
        List<BlockPos> spots = new ArrayList<>();
        BlockPos start = standingStart(level, player.blockPosition());
        if (start == null) {
            return spots;
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < SEARCH_LIMIT) {
            BlockPos pos = queue.poll();
            if (pos.distSqr(start) >= (double) minDistance * minDistance && roomy(level, pos)) {
                spots.add(pos);
            }
            for (Direction direction : FLAT) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = pos.relative(direction).above(dy);
                    boolean inRange = Math.abs(next.getY() - start.getY()) <= verticalRange
                        && next.distSqr(start) <= (double) maxDistance * maxDistance;
                    if (inRange && !seen.contains(next) && canStep(level, pos, next, dy)) {
                        seen.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return spots;
    }

    // The player's feet, or the first standable cell a few blocks below while they jump or fall.
    private static BlockPos standingStart(ServerLevel level, BlockPos feet) {
        for (int i = 0; i < 4; i++) {
            if (standable(level, feet.below(i))) {
                return feet.below(i);
            }
        }
        return null;
    }

    // A one-block step either way needs headroom over the lower cell.
    private static boolean canStep(ServerLevel level, BlockPos from, BlockPos to, int dy) {
        if (!level.isLoaded(to) || !standable(level, to)) {
            return false;
        }
        if (dy > 0) {
            return open(level, from.above(2));
        }
        return dy == 0 || open(level, to.above(2));
    }

    // Two open blocks over a solid floor.
    private static boolean standable(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        return open(level, feet) && open(level, feet.above())
            && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    // Three blocks tall and two wide.
    private static boolean roomy(ServerLevel level, BlockPos feet) {
        if (!open(level, feet.above(2))) {
            return false;
        }
        for (Direction direction : FLAT) {
            BlockPos side = feet.relative(direction);
            if (standable(level, side) && open(level, side.above(2))) {
                return true;
            }
        }
        return false;
    }

    // Unloaded counts as closed so the search never loads chunks.
    private static boolean open(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    // Whether any player nearby has a clear line to the spot's feet or head.
    private static boolean watched(ServerLevel level, BlockPos spot) {
        Vec3 feet = Vec3.atBottomCenterOf(spot);
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.position().distanceToSqr(feet) > WATCH_RADIUS * WATCH_RADIUS) {
                continue;
            }
            if (sees(level, player, feet.add(0.0, 0.5, 0.0)) || sees(level, player, feet.add(0.0, 1.5, 0.0))) {
                return true;
            }
        }
        return false;
    }

    private static boolean sees(ServerLevel level, ServerPlayer player, Vec3 target) {
        ClipContext clip = new ClipContext(player.getEyePosition(), target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);
        return level.clip(clip).getType() == HitResult.Type.MISS;
    }

    // Moves one far, idle monster that no player can see into a hidden spot near this player.
    private static void gatherOne(ServerLevel level, ServerPlayer player, List<BlockPos> spots, RandomSource random) {
        AABB area = new AABB(player.blockPosition()).inflate(GATHER_RADIUS);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area, monster -> movable(level, player, monster))) {
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                BlockPos spot = spots.get(random.nextInt(spots.size()));
                if (inAlexsCaves(level, spot)) {
                    continue;
                }
                Vec3 target = Vec3.atBottomCenterOf(spot);
                if (!watched(level, spot) && level.noCollision(monster, monster.getBoundingBox().move(target.subtract(monster.position())))) {
                    monster.teleportTo(target.x, target.y, target.z);
                    monster.getNavigation().stop();
                    return;
                }
            }
        }
    }

    // Natural, idle and out of sight, so moving it is never noticed.
    private static boolean movable(ServerLevel level, ServerPlayer player, Monster monster) {
        if (!monster.isAlive() || monster.isPersistenceRequired() || !monster.removeWhenFarAway(0.0)) {
            return false;
        }
        if (monster.isPassenger() || monster.isVehicle() || monster.isLeashed() || monster.getTarget() != null) {
            return false;
        }
        return monster.distanceTo(player) > CAP_RADIUS && !watched(level, monster.blockPosition());
    }

    // Whether the spot lies in an Alex's Caves biome.
    private static boolean inAlexsCaves(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ALEXS_CAVES);
    }

    // Spawns the biome's own Alex's Caves mobs near the player instead of dungeon monsters.
    private static void spawnEndemic(ServerLevel level, ServerPlayer player, List<BlockPos> spots, RandomSource random) {
        MobCategory category = caveCreature();
        if (category == null) {
            return;
        }
        AABB area = new AABB(player.blockPosition()).inflate(CAP_RADIUS);
        if (level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory() == category).size() >= ENDEMIC_CAP) {
            return;
        }
        List<BlockPos> local = spots.stream().filter(spot -> inAlexsCaves(level, spot)).toList();
        if (!local.isEmpty()) {
            spawnOne(level, local, random, random.nextBoolean() ? category : MobCategory.MONSTER);
        }
    }

    // Alex's Caves' own spawn category, or null when the mod is absent.
    private static MobCategory caveCreature() {
        for (MobCategory category : MobCategory.values()) {
            if (category.getName().contains(CAVE_CREATURE)) {
                return category;
            }
        }
        return null;
    }

    private static void spawnOne(ServerLevel level, List<BlockPos> spots, RandomSource random, MobCategory category) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            BlockPos spot = spots.get(random.nextInt(spots.size()));
            if (!watched(level, spot) && spawnAt(level, spot, random, category)) {
                return;
            }
        }
    }

    // Drops a drowned or guardian into a big, unseen pool in the ruin near this player.
    private static void spawnSwimmer(ServerLevel level, ServerPlayer player, RandomSource random) {
        AABB area = new AABB(player.blockPosition()).inflate(CAP_RADIUS);
        int swimmers = level.getEntitiesOfClass(Monster.class, area,
            monster -> monster.getType() == EntityTypes.DROWNED || monster.getType() == EntityTypes.GUARDIAN).size();
        if (swimmers >= SWIMMER_CAP) {
            return;
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int distance = Mth.nextInt(random, RUIN_MIN_DISTANCE, RUIN_MAX_DISTANCE);
            float angle = random.nextFloat() * Mth.TWO_PI;
            BlockPos pos = player.blockPosition().offset(
                Math.round(Mth.cos(angle) * distance),
                Mth.nextInt(random, -RUIN_VERTICAL_RANGE, RUIN_VERTICAL_RANGE),
                Math.round(Mth.sin(angle) * distance));
            if (level.isLoaded(pos) && bigPool(level, pos) && !watched(level, pos)) {
                EntityType<? extends Mob> type = random.nextInt(DROWNED_WEIGHT + 1) == 0 ? EntityTypes.GUARDIAN : EntityTypes.DROWNED;
                Mob mob = type.create(level, EntitySpawnReason.NATURAL);
                if (mob == null) {
                    return;
                }
                mob.snapTo(Vec3.atBottomCenterOf(pos), random.nextFloat() * 360.0f, 0.0f);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
                level.addFreshEntityWithPassengers(mob);
                return;
            }
        }
    }

    // Water here and above, with enough water around to count as a pool rather than a puddle.
    private static boolean bigPool(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return false;
        }
        int water = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-POOL_REACH, -1, -POOL_REACH), pos.offset(POOL_REACH, 1, POOL_REACH))) {
            if (level.isLoaded(near) && level.getFluidState(near).is(FluidTags.WATER)) {
                water++;
            }
        }
        return water >= POOL_MIN_WATER;
    }

    private static boolean spawnAt(ServerLevel level, BlockPos pos, RandomSource random, MobCategory category) {
        Optional<MobSpawnSettings.SpawnerData> entry = level.getChunkSource().getGenerator()
            .getMobsAt(level, level.structureManager(), category, pos)
            .getRandom(random);
        if (entry.isEmpty()) {
            return false;
        }
        EntityType<?> type = entry.get().type();
        if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)) {
            return false;
        }
        if (!(type.create(level, EntitySpawnReason.NATURAL) instanceof Mob mob)) {
            return false;
        }
        mob.snapTo(Vec3.atBottomCenterOf(pos), random.nextFloat() * 360.0f, 0.0f);
        if (!mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) || !mob.checkSpawnObstruction(level)) {
            mob.discard();
            return false;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        level.addFreshEntityWithPassengers(mob);
        return true;
    }
}
