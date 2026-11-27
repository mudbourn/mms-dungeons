package info.mudbourn.mmsdungeons.mobs.factions;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.mobs.factions.entity.FactionMember;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

// Where the human factions spawn, how far they keep from claims, and how a spawned founder gathers its group.
public final class FactionSpawns {

    // The group a founder spawns with, and its leader when the faction has one.
    public record Crew(@Nullable UUID leader) implements SpawnGroupData {
    }

    private static final int CLAIM_CHUNKS = 15;
    private static final int CLAIM_CACHE_TICKS = 600;
    private static final int CLAIM_CACHE_MAX = 4096;
    private static final Map<ResourceKey<Level>, Map<Long, Long>> CLAIM_CACHE = new HashMap<>();
    private static final int OVERWORLD_ODDS = 4;
    private static final double CROWD_RANGE = 48.0;
    private static final int CROWD_LIMIT = 8;
    private static final double DEEP_SPACING = 64.0;
    private static final int PLACE_ATTEMPTS = 8;
    private static final int PLACE_SPREAD = 3;

    private FactionSpawns() {
    }

    public static void register() {
        Predicate<BiomeSelectionContext> outsideEnd = BiomeSelectors.foundInTheEnd().negate();
        Predicate<BiomeSelectionContext> dungeon = context -> context.getBiomeKey().identifier().getNamespace().equals("mms_dungeons");
        addSpawn(outsideEnd, HumanFactions.BANDIT, 4);
        addSpawn(outsideEnd, HumanFactions.MERCENARY, 2);
        addSpawn(outsideEnd, HumanFactions.DARK_GUILDSMAN, 1);
        addSpawn(dungeon, HumanFactions.BANDIT, 6);
        addSpawn(dungeon, HumanFactions.MERCENARY, 5);
        addSpawn(dungeon, HumanFactions.DARK_GUILDSMAN, 4);
        for (EntityType<FactionMember> type : HumanFactions.types()) {
            SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FactionSpawns::checkSpawn);
        }
    }

    private static void addSpawn(Predicate<BiomeSelectionContext> biomes, EntityType<FactionMember> type, int weight) {
        BiomeModifications.addSpawn(biomes, MobCategory.MONSTER, type, weight, 1, 1);
    }

    private static boolean checkSpawn(
        EntityType<? extends FactionMember> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        boolean placed = isWorldSpawn(reason) || reason == EntitySpawnReason.STRUCTURE;
        if (placed && DungeonDimension.isSanctuary(level.getLevel(), pos)) {
            return false;
        }
        if (!isWorldSpawn(reason)) {
            return true;
        }
        ServerLevel server = level.getLevel();
        if (server.dimension() == Level.END) {
            return false;
        }
        if (server.dimension() == Level.OVERWORLD && random.nextInt(OVERWORLD_ODDS) != 0) {
            return false;
        }
        // In the Deep a new group starts well clear of every other.
        if (DungeonDimension.isDeep(server, pos) && !level.getEntitiesOfClass(FactionMember.class, new AABB(pos).inflate(DEEP_SPACING)).isEmpty()) {
            return false;
        }
        // Counts the whole group this would bring, not only its founder.
        if (level.getEntitiesOfClass(FactionMember.class, new AABB(pos).inflate(CROWD_RANGE)).size() + HumanFactions.factionOf(type).maxGroup() > CROWD_LIMIT) {
            return false;
        }
        return !nearClaim(server, pos);
    }

    // Whether any Open Parties and Claims claim, guild claims included, lies within 15 chunks. Spawn attempts bunch up around players, so an answer is kept for half a minute per chunk.
    private static boolean nearClaim(ServerLevel level, BlockPos pos) {
        long key = ChunkPos.pack(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        Map<Long, Long> cache = CLAIM_CACHE.computeIfAbsent(level.dimension(), dimension -> new HashMap<>());
        long now = level.getGameTime();
        Long cached = cache.get(key);
        if (cached != null && now - (cached >> 1) < CLAIM_CACHE_TICKS) {
            return (cached & 1L) != 0L;
        }
        if (cache.size() > CLAIM_CACHE_MAX) {
            cache.clear();
        }
        boolean near = scanClaims(level, pos);
        cache.put(key, now << 1 | (near ? 1L : 0L));
        return near;
    }

    private static boolean scanClaims(ServerLevel level, BlockPos pos) {
        IServerClaimsManagerAPI claims = OpenPACServerAPI.get(level.getServer()).getServerClaimsManager();
        Identifier dimension = level.dimension().identifier();
        int centerX = SectionPos.blockToSectionCoord(pos.getX());
        int centerZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (int dx = -CLAIM_CHUNKS; dx <= CLAIM_CHUNKS; dx++) {
            for (int dz = -CLAIM_CHUNKS; dz <= CLAIM_CHUNKS; dz++) {
                if (claims.get(dimension, centerX + dx, centerZ + dz) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWorldSpawn(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
    }

    public static boolean formsCrew(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.STRUCTURE;
    }

    // Spawns the rest of the founder's group on loaded ground around it and returns the group.
    public static Crew formCrew(FactionMember founder, ServerLevelAccessor accessor, RandomSource random) {
        Crew crew = new Crew(founder.isLeader() ? founder.getUUID() : null);
        if (!(accessor instanceof ServerLevel level)) {
            return crew;
        }
        Faction faction = founder.faction();
        int others = faction.minGroup() - 1 + random.nextInt(faction.maxGroup() - faction.minGroup() + 1);
        EntityType<FactionMember> type = HumanFactions.typeOf(faction);
        for (int i = 0; i < others; i++) {
            BlockPos spot = findSpot(level, founder.blockPosition(), random);
            if (spot == null) {
                continue;
            }
            FactionMember member = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (member == null) {
                continue;
            }
            member.snapTo(Vec3.atBottomCenterOf(spot), random.nextFloat() * 360.0f, 0.0f);
            member.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.MOB_SUMMONED, crew);
            level.addFreshEntityWithPassengers(member);
        }
        return crew;
    }

    private static @Nullable BlockPos findSpot(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < PLACE_ATTEMPTS; attempt++) {
            BlockPos base = origin.offset(
                random.nextInt(PLACE_SPREAD * 2 + 1) - PLACE_SPREAD,
                0,
                random.nextInt(PLACE_SPREAD * 2 + 1) - PLACE_SPREAD
            );
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos feet = base.above(dy);
                if (standable(level, feet)) {
                    return feet;
                }
            }
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        BlockState floor = stateNow(level, feet.below());
        return open(level, feet) && open(level, feet.above()) && floor != null && floor.isFaceSturdy(level, feet.below(), Direction.UP);
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = stateNow(level, pos);
        return state != null && state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    // The block at pos, or null when its chunk is not loaded, so a spawn never waits on a chunk.
    private static @Nullable BlockState stateNow(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
