package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmseconomy.deal.Spot;
import info.mudbourn.mmseconomy.deal.WholesaleManager;
import info.mudbourn.mmseconomy.market.Depot;
import info.mudbourn.mmseconomy.market.Market;
import info.mudbourn.mmseconomy.market.ShopListing;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

// Finds and reads mms-economy depots for guilds, quests and contracts.
final class Depots {

    private static final double LOOK_REACH = 6.0;

    private Depots() {
    }

    // The depot the player looks at or owns nearby, or null.
    static Spot ownedNearby(ServerPlayer player) {
        ServerLevel world = player.level();
        BlockPos looked = Depot.lookedAtDepot(player, LOOK_REACH);
        if (looked != null && Market.get(world.getServer()).ownsAnyAt(looked, player.getUUID())) {
            return new Spot(looked, world.dimension().identifier().toString());
        }
        return WholesaleManager.ownDepot(player);
    }

    // Any complete depot the player has a shop listing at, or null.
    static Spot ownedAny(MinecraftServer server, UUID owner) {
        for (ShopListing listing : Market.get(server).all()) {
            Spot spot = new Spot(listing.depot(), listing.dimension());
            if (listing.owner().equals(owner) && !barrels(server, spot).isEmpty()) {
                return spot;
            }
        }
        return null;
    }

    // The four barrels of a depot, or an empty list when the depot is missing or incomplete.
    static List<Container> barrels(MinecraftServer server, Spot spot) {
        if (spot == null) {
            return List.of();
        }
        Identifier id = Identifier.tryParse(spot.dimension());
        if (id == null) {
            return List.of();
        }
        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        return world == null ? List.of() : Depot.barrels(world, spot.pos());
    }

    // How many more of the stack the depot has room for.
    static int room(List<Container> barrels, ItemStack template) {
        int room = 0;
        int limit = template.getMaxStackSize();
        for (Container barrel : barrels) {
            int cap = Math.min(limit, barrel.getMaxStackSize());
            for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
                ItemStack stack = barrel.getItem(slot);
                if (stack.isEmpty()) {
                    room += cap;
                } else if (ItemStack.isSameItemSameComponents(stack, template)) {
                    room += Math.max(0, cap - stack.getCount());
                }
            }
        }
        return room;
    }
}
