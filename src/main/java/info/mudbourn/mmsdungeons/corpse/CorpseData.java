package info.mudbourn.mmsdungeons.corpse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsdungeons.MmsDungeons;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// Each player's saved corpse: the items taken at death and where the marker stands, kept apart from any chunk.
public final class CorpseData extends SavedData {

    public static final Codec<CorpseData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(Codec.STRING, Corpse.CODEC).fieldOf("corpses").forGetter(data -> data.corpses)
    ).apply(instance, CorpseData::new));

    public static final SavedDataType<CorpseData> TYPE = new SavedDataType<>(
        MmsDungeons.id("corpses"),
        CorpseData::new,
        CODEC,
        DataFixTypes.LEVEL);

    private final Map<String, Corpse> corpses = new HashMap<>();

    public CorpseData() {
    }

    private CorpseData(Map<String, Corpse> loaded) {
        corpses.putAll(loaded);
    }

    // The items, marker entity and dimension of one player's corpse.
    public static final class Corpse {

        public static final Codec<Corpse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(corpse -> corpse.items),
            UUIDUtil.CODEC.optionalFieldOf("marker").forGetter(corpse -> Optional.ofNullable(corpse.marker)),
            Identifier.CODEC.fieldOf("dimension").forGetter(corpse -> corpse.dimension)
        ).apply(instance, (items, marker, dimension) -> new Corpse(items, marker.orElse(null), dimension)));

        public final List<ItemStack> items;
        public UUID marker;
        public Identifier dimension;

        private Corpse(List<ItemStack> items, UUID marker, Identifier dimension) {
            this.items = new ArrayList<>(items);
            this.marker = marker;
            this.dimension = dimension;
        }
    }

    public Corpse get(UUID owner) {
        return corpses.get(owner.toString());
    }

    public Corpse getOrCreate(UUID owner, Identifier dimension) {
        setDirty();
        return corpses.computeIfAbsent(owner.toString(), key -> new Corpse(List.of(), null, dimension));
    }

    public Corpse take(UUID owner) {
        Corpse corpse = corpses.remove(owner.toString());
        if (corpse != null) {
            setDirty();
        }
        return corpse;
    }

    // Whether a marker entity still belongs to a live record.
    public boolean ownsMarker(UUID owner, UUID marker) {
        Corpse corpse = corpses.get(owner.toString());
        return corpse != null && marker.equals(corpse.marker);
    }
}
