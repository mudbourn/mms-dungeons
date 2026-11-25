package info.mudbourn.mmsdungeons.party;

import info.mudbourn.mmsdungeons.config.FeaturesConfig;
import info.mudbourn.mmsdungeons.net.PartyAlertPayload;
import info.mudbourn.mmsdungeons.net.PartyMember;
import info.mudbourn.mmsdungeons.net.PartySyncPayload;
import info.mudbourn.mmsdungeons.net.PlayerSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Pushes teammate stats to party members a few times a second and relays attack alerts.
public final class PartySync {

    private static final Set<UUID> SYNCED = new HashSet<>();
    private static final Map<UUID, Long> LAST_ALERT = new HashMap<>();

    private PartySync() {
    }

    public static void forget(ServerPlayer player) {
        SYNCED.remove(player.getUUID());
        LAST_ALERT.remove(player.getUUID());
    }

    static void tick(MinecraftServer server) {
        PartyData data = Parties.data();
        if (data == null || server.getTickCount() % FeaturesConfig.get().syncIntervalTicks != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PartyData.Party party = data.partyOf(player.getUUID());
            if (party == null) {
                if (SYNCED.remove(player.getUUID())) {
                    ServerPlayNetworking.send(player, new PartySyncPayload(List.of()));
                }
                continue;
            }
            List<PartyMember> members = new ArrayList<>();
            for (UUID id : party.members) {
                ServerPlayer member = server.getPlayerList().getPlayer(id);
                if (member != null) {
                    members.add(describe(member));
                }
            }
            SYNCED.add(player.getUUID());
            ServerPlayNetworking.send(player, new PartySyncPayload(members));
        }
    }

    static void alert(ServerPlayer victim) {
        PartyData data = Parties.data();
        if (data == null) {
            return;
        }
        PartyData.Party party = data.partyOf(victim.getUUID());
        if (party == null) {
            return;
        }
        long now = victim.level().getGameTime();
        Long last = LAST_ALERT.get(victim.getUUID());
        if (last != null && now - last < FeaturesConfig.get().alertCooldownTicks) {
            return;
        }
        LAST_ALERT.put(victim.getUUID(), now);
        MinecraftServer server = Parties.serverOf(victim);
        for (UUID id : party.members) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null && member != victim) {
                ServerPlayNetworking.send(member, new PartyAlertPayload(victim.getUUID()));
                PlayerSounds.play(member, SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        }
    }

    private static PartyMember describe(ServerPlayer player) {
        return new PartyMember(
            player.getUUID(),
            player.getGameProfile().name(),
            player.getHealth(),
            (float) player.getAttributeValue(Attributes.MAX_HEALTH),
            player.getFoodData().getFoodLevel(),
            player.getArmorValue(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.level().dimension().identifier());
    }
}
