package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.net.CycleLocatorPayload;
import info.mudbourn.mmsdungeons.net.LocatorPick;
import info.mudbourn.mmsdungeons.net.LocatorPoi;
import info.mudbourn.mmsdungeons.net.OpenTuningPayload;
import info.mudbourn.mmsdungeons.net.TuneLocatorPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

// Sends the tuning list, applies a tuning, and cycles the focus.
final class LocatorNetworking {

    private LocatorNetworking() {
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenTuningPayload.TYPE, OpenTuningPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TuneLocatorPayload.TYPE, TuneLocatorPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleLocatorPayload.TYPE, CycleLocatorPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TuneLocatorPayload.TYPE, (payload, context) ->
            context.server().execute(() -> tune(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(CycleLocatorPayload.TYPE, (payload, context) ->
            context.server().execute(() -> LocatorItem.cycle(context.player())));
    }

    static void open(ServerPlayer player) {
        ServerLevel dungeon = player.level().getServer().getLevel(DungeonDimension.LEVEL);
        if (dungeon == null) {
            dungeon = player.level();
        }
        List<Holder<Biome>> biomes = new ArrayList<>(dungeon.getChunkSource().getGenerator().getBiomeSource().possibleBiomes());
        Registry<Structure> registry = dungeon.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<LocatorPoi> presets = new ArrayList<>();
        for (LocatorTarget target : Locators.TARGETS) {
            presets.add(new LocatorPoi(target.id(), LocatorRegions.mask(dungeon, target)));
        }
        List<LocatorPoi> structures = new ArrayList<>();
        for (Holder.Reference<Structure> holder : registry.listElements().toList()) {
            if (biomes.stream().anyMatch(biome -> holder.value().biomes().contains(biome))) {
                structures.add(new LocatorPoi(holder.key().identifier(), LocatorRegions.structure(dungeon, holder)));
            }
        }
        List<LocatorPoi> biomeEntries = new ArrayList<>();
        for (Holder<Biome> biome : biomes) {
            ServerLevel level = dungeon;
            biome.unwrapKey().ifPresent(key -> biomeEntries.add(new LocatorPoi(key.identifier(), LocatorRegions.biome(level, biome))));
        }
        structures.sort(Comparator.comparing(LocatorPoi::id));
        biomeEntries.sort(Comparator.comparing(LocatorPoi::id));
        List<LocatorPick> picks = new ArrayList<>();
        ItemStack stack = held(player);
        if (stack != null) {
            for (LocatorTarget target : LocatorItem.selection(stack)) {
                picks.add(new LocatorPick(target.kind(), target.id()));
            }
        }
        ServerPlayNetworking.send(player, new OpenTuningPayload(presets, structures, biomeEntries, picks, LocatorRegions.current(player)));
    }

    private static void tune(ServerPlayer player, TuneLocatorPayload payload) {
        ItemStack stack = held(player);
        if (stack == null) {
            return;
        }
        Set<LocatorPick> accepted = new LinkedHashSet<>();
        for (LocatorPick pick : payload.picks()) {
            if (accepted.size() < LocatorPick.MAX && exists(player, pick)) {
                accepted.add(pick);
            }
        }
        List<LocatorTarget> targets = new ArrayList<>();
        Set<LocatorPick> current = new HashSet<>();
        for (LocatorTarget target : LocatorItem.selection(stack)) {
            current.add(new LocatorPick(target.kind(), target.id()));
        }
        if (accepted.equals(current)) {
            return;
        }
        for (LocatorPick pick : accepted) {
            targets.add(LocatorTarget.single(pick.kind(), pick.id()));
        }
        LocatorItem.select(player, stack, targets);
        Component message = switch (targets.size()) {
            case 0 -> Component.translatable("locator.mms_dungeons.no_target");
            case 1 -> Component.translatable("locator.mms_dungeons.selected", targets.get(0).label());
            default -> Component.translatable("locator.mms_dungeons.selected_many", targets.size());
        };
        player.sendSystemMessage(message, true);
    }

    private static boolean exists(ServerPlayer player, LocatorPick pick) {
        return switch (pick.kind()) {
            case PRESET -> Locators.preset(pick.id()) != null;
            case STRUCTURE -> player.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).containsKey(pick.id());
            case BIOME -> player.level().registryAccess().lookupOrThrow(Registries.BIOME).containsKey(pick.id());
        };
    }

    private static ItemStack held(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Locators.LOCATOR)) {
                return stack;
            }
        }
        return null;
    }
}
