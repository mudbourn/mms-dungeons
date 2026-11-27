package info.mudbourn.mmsdungeons.dungeon;

import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

// Where Deadly Deadly Dungeon structures may start in the dungeon, and what their pieces leave out.
public final class DddPlacement {

    private static final String DDD_NAMESPACE = "ddd";
    // mms-dungeons places Deadly Deadly Dungeon pieces through its own structures, named ddd_*.
    private static final String PLACED_PREFIX = "ddd_";
    private static final TagKey<Biome> ALEXS_CAVES = TagKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath("alexscaves", "alexs_caves_biomes")
    );
    private static final int AC_BUFFER = 250;
    private static final int SAMPLE_STEP = 50;
    private static final int[] SAMPLE_HEIGHTS = {-32, 0, 32};
    // Swaps mob spawners for void air, which RemovedBlocks then settles, as a piece is placed. Trial spawners stay.
    public static final StructureProcessor NO_SPAWNERS = new RuleProcessor(List.of(
        new ProcessorRule(new BlockMatchTest(Blocks.SPAWNER), AlwaysTrueTest.INSTANCE, Blocks.VOID_AIR.defaultBlockState())
    ));

    private DddPlacement() {
    }

    public static boolean isDddTemplate(Identifier id) {
        return id.getNamespace().equals(DDD_NAMESPACE);
    }

    public static boolean isPlacedDdd(Identifier id) {
        boolean placedByUs = id.getNamespace().equals(MmsDungeons.MOD_ID) && id.getPath().startsWith(PLACED_PREFIX);
        return placedByUs || id.getNamespace().equals(DDD_NAMESPACE);
    }

    // Whether an Alex's Caves biome lies within AC_BUFFER blocks of a start, checked on a grid around it.
    public static boolean nearAlexsCaves(Structure.GenerationContext context, BlockPos start) {
        for (int dx = -AC_BUFFER; dx <= AC_BUFFER; dx += SAMPLE_STEP) {
            for (int dz = -AC_BUFFER; dz <= AC_BUFFER; dz += SAMPLE_STEP) {
                if (dx * dx + dz * dz > AC_BUFFER * AC_BUFFER) {
                    continue;
                }
                for (int dy : SAMPLE_HEIGHTS) {
                    boolean alexsCaves = context.biomeResolver().getNoiseBiome(
                        QuartPos.fromBlock(start.getX() + dx),
                        QuartPos.fromBlock(start.getY() + dy),
                        QuartPos.fromBlock(start.getZ() + dz)
                    ).is(ALEXS_CAVES);
                    if (alexsCaves) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
