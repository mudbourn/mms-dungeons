package info.mudbourn.mmsdungeons.corpse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Every saved corpse, one record per death: the items taken, where the marker stands and when they fell, kept apart from any chunk.
public final class CorpseData extends SavedData {

    public static final Codec<CorpseData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Corpse.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(data -> data.records),
        Codec.unboundedMap(Codec.STRING, Corpse.LEGACY_CODEC).optionalFieldOf("corpses", Map.of()).forGetter(data -> Map.of())
    ).apply(instance, CorpseData::new));

    public static final SavedDataType<CorpseData> TYPE = new SavedDataType<>(
        MmsDungeons.id("corpses"),
        CorpseData::new,
        CODEC,
        DataFixTypes.LEVEL);

    private final List<Corpse> records = new ArrayList<>();

    public CorpseData() {
    }

    private CorpseData(List<Corpse> loaded, Map<String, Corpse> legacy) {
        records.addAll(loaded);
        for (Map.Entry<String, Corpse> entry : legacy.entrySet()) {
            try {
                UUID owner = UUID.fromString(entry.getKey());
                Corpse corpse = entry.getValue();
                corpse.owner = owner;
                records.add(corpse);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // One death's items, marker entity, dimension, spot, time, and the look of its body.
    public static final class Corpse {

        public static final Codec<Corpse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(corpse -> corpse.id),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(corpse -> corpse.owner),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(corpse -> corpse.items),
            UUIDUtil.CODEC.optionalFieldOf("marker").forGetter(corpse -> Optional.ofNullable(corpse.marker)),
            Identifier.CODEC.fieldOf("dimension").forGetter(corpse -> corpse.dimension),
            BlockPos.CODEC.optionalFieldOf("pos").forGetter(corpse -> Optional.ofNullable(corpse.pos)),
            Codec.LONG.optionalFieldOf("time", 0L).forGetter(corpse -> corpse.time),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("worn", List.of()).forGetter(corpse -> corpse.worn),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(corpse -> corpse.scale)
        ).apply(instance, (id, owner, items, marker, dimension, pos, time, worn, scale) ->
            new Corpse(id, owner, items, marker.orElse(null), dimension, pos.orElse(null), time, worn, scale)));

        public static final Codec<Corpse> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(corpse -> corpse.items),
            UUIDUtil.CODEC.optionalFieldOf("marker").forGetter(corpse -> Optional.ofNullable(corpse.marker)),
            Identifier.CODEC.fieldOf("dimension").forGetter(corpse -> corpse.dimension)
        ).apply(instance, (items, marker, dimension) ->
            new Corpse(UUID.randomUUID(), null, items, marker.orElse(null), dimension, null, 0L, List.of(), 1.0F)));

        public final UUID id;
        public UUID owner;
        public final List<ItemStack> items;
        public UUID marker;
        public Identifier dimension;
        public BlockPos pos;
        public long time;
        // What the body wears for show, head to feet, and its size.
        public final List<ItemStack> worn;
        public final float scale;

        private Corpse(UUID id, UUID owner, List<ItemStack> items, UUID marker, Identifier dimension, BlockPos pos, long time, List<ItemStack> worn, float scale) {
            this.id = id;
            this.owner = owner;
            this.items = new ArrayList<>(items);
            this.marker = marker;
            this.dimension = dimension;
            this.pos = pos;
            this.time = time;
            this.worn = List.copyOf(worn);
            this.scale = scale;
        }
    }

    public Corpse add(UUID owner, List<ItemStack> items, Identifier dimension, BlockPos pos, long time, List<ItemStack> worn, float scale) {
        Corpse corpse = new Corpse(UUID.randomUUID(), owner, items, null, dimension, pos, time, worn, scale);
        records.add(corpse);
        setDirty();
        return corpse;
    }

    // The owner's corpses, newest first.
    public List<Corpse> of(UUID owner) {
        List<Corpse> found = new ArrayList<>();
        for (Corpse corpse : records) {
            if (owner.equals(corpse.owner)) {
                found.add(corpse);
            }
        }
        found.sort((a, b) -> Long.compare(b.time, a.time));
        return found;
    }

    public Corpse byId(UUID id) {
        for (Corpse corpse : records) {
            if (corpse.id.equals(id)) {
                return corpse;
            }
        }
        return null;
    }

    public Corpse byMarker(UUID marker) {
        for (Corpse corpse : records) {
            if (marker.equals(corpse.marker)) {
                return corpse;
            }
        }
        return null;
    }

    public void remove(Corpse corpse) {
        if (records.remove(corpse)) {
            setDirty();
        }
    }

    public void touch() {
        setDirty();
    }
}
