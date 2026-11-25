package info.mudbourn.mmsdungeons.guild;

import info.mudbourn.mmseconomy.deal.Spot;
import info.mudbourn.mmseconomy.market.Depot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

// Moves handed-in proof items from a guild depot to the poster's depot on the next in-game day.
final class Shipping {

    private Shipping() {
    }

    // Takes the proof items out of the guild depot and holds them for the next day.
    static void queue(MinecraftServer server, GuildData data, Guild guild, Bounty bounty) {
        if (bounty.posterDepot == null) {
            return;
        }
        List<Container> barrels = Depots.barrels(server, guild.depot);
        int taken = Depot.take(barrels, bounty.proof.stack(), bounty.proof.count());
        if (taken > 0) {
            data.shipments.add(new Shipment(
                bounty.poster,
                bounty.posterDepot,
                bounty.proof.stack(),
                taken,
                bounty.finishedDay + 1
            ));
        }
    }

    // Inserts every due shipment into its depot or another depot of the recipient, and keeps the rest for the next day.
    static void deliver(MinecraftServer server, GuildData data, long today) {
        for (Shipment shipment : new ArrayList<>(data.shipments)) {
            if (shipment.arrivalDay > today) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(shipment.recipient);
            List<Container> barrels = Depots.barrels(server, shipment.depot);
            if (barrels.isEmpty()) {
                Spot found = Depots.ownedAny(server, shipment.recipient);
                if (found == null) {
                    if (owner != null) {
                        owner.sendSystemMessage(Component.literal(
                            shipment.count + " x " + shipment.stack.getHoverName().getString() + " waits for a depot you own."));
                    }
                    continue;
                }
                shipment.depot = found;
                barrels = Depots.barrels(server, found);
            }
            int inserted = Depot.insert(barrels, shipment.stack, shipment.count);
            shipment.count -= inserted;
            if (owner != null && inserted > 0) {
                owner.sendSystemMessage(Component.literal(
                    inserted + " x " + shipment.stack.getHoverName().getString() + " arrived at your depot."));
            }
            if (shipment.count <= 0) {
                data.shipments.remove(shipment);
            }
        }
    }
}
