package info.mudbourn.mmsdungeons.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Pairs each surface portal with the exit portal it built in the dungeon, both ways, keyed by each portal's anchor block.
public final class PortalLinks extends SavedData {

    private record Link(GlobalPos surface, BlockPos exit) {

        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("surface").forGetter(Link::surface),
            BlockPos.CODEC.fieldOf("exit").forGetter(Link::exit)
        ).apply(instance, Link::new));
    }

    public static final Codec<PortalLinks> CODEC = Link.CODEC.listOf()
        .xmap(PortalLinks::new, links -> List.copyOf(links.bySurface.values()));

    public static final SavedDataType<PortalLinks> TYPE = new SavedDataType<>(
        MmsDungeons.id("portal_links"),
        PortalLinks::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    private final Map<GlobalPos, Link> bySurface = new HashMap<>();
    private final Map<BlockPos, Link> byExit = new HashMap<>();

    public PortalLinks() {
    }

    private PortalLinks(List<Link> links) {
        for (Link link : links) {
            bySurface.put(link.surface(), link);
            byExit.put(link.exit(), link);
        }
    }

    public static PortalLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public BlockPos exitFor(GlobalPos surface) {
        Link link = bySurface.get(surface);
        return link == null ? null : link.exit();
    }

    public GlobalPos surfaceFor(BlockPos exit) {
        Link link = byExit.get(exit);
        return link == null ? null : link.surface();
    }

    public void link(GlobalPos surface, BlockPos exit) {
        Link link = new Link(surface, exit);
        Link old = bySurface.put(surface, link);
        if (old != null) {
            byExit.remove(old.exit());
        }
        byExit.put(exit, link);
        setDirty();
    }
}
