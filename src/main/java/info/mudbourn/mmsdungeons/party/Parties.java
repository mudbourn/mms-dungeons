package info.mudbourn.mmsdungeons.party;

import info.mudbourn.mmsdungeons.dungeon.DungeonDimension;
import info.mudbourn.mmsdungeons.net.PartyAlertPayload;
import info.mudbourn.mmsdungeons.net.PartySyncPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.internals.target.EntityRelations;

// Wires parties: storage, commands, the HUD sync, friendly fire, attack alerts and the Spell Engine team matcher.
public final class Parties {

    private static PartyData data;

    private Parties() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(PartySyncPayload.TYPE, PartySyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PartyAlertPayload.TYPE, PartyAlertPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> data = server.overworld().getDataStorage().computeIfAbsent(PartyData.TYPE));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> data = null);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PartyCommands.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (data != null) {
                data.rememberName(handler.player.getUUID(), handler.player.getGameProfile().name());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PartySync.forget(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(PartySync::tick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !blocksFriendlyFire(entity, source.getEntity()));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (damage > 0.0F && entity instanceof ServerPlayer victim && source.getEntity() != null && source.getEntity() != victim) {
                PartySync.alert(victim);
            }
        });

        EntityRelations.registerTeamMatcher("mms_dungeons_party", (attacker, target) -> {
            if (attacker == target || !(attacker instanceof Player) || !(target instanceof Player)) {
                return null;
            }
            if (!sameParty(attacker, target)) {
                return null;
            }
            return new EntityRelations.TeamRelation(true, DungeonDimension.isPericulum(target.level(), target.blockPosition()));
        });
    }

    public static PartyData data() {
        return data;
    }

    public static MinecraftServer serverOf(ServerPlayer player) {
        return player.level().getServer();
    }

    // Whether two players share a party, using the saved parties on the server and the synced copy on a client.
    public static boolean sameParty(Entity a, Entity b) {
        if (a.level().isClientSide()) {
            return PartyView.isMember(a.getUUID()) && PartyView.isMember(b.getUUID());
        }
        return data != null && data.sameParty(a.getUUID(), b.getUUID());
    }

    private static boolean blocksFriendlyFire(Entity victim, Entity attacker) {
        return victim instanceof ServerPlayer
            && attacker instanceof ServerPlayer
            && victim != attacker
            && sameParty(victim, attacker)
            && !DungeonDimension.isPericulum(victim.level(), victim.blockPosition());
    }
}
