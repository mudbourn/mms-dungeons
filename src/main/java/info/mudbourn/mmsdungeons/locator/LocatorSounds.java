package info.mudbourn.mmsdungeons.locator;

import info.mudbourn.mmsdungeons.MmsDungeons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// The locator's sound events and per-player playback.
final class LocatorSounds {

    static SoundEvent SEARCHING;
    static SoundEvent CONNECT;
    static SoundEvent PING;
    static SoundEvent TOGGLE;
    static SoundEvent STATIC;

    private LocatorSounds() {
    }

    static void register() {
        SEARCHING = register("locator.searching");
        CONNECT = register("locator.connect");
        PING = register("locator.ping");
        TOGGLE = register("locator.toggle");
        STATIC = register("locator.static");
    }

    static void play(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundEntityPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
            SoundSource.PLAYERS,
            player,
            1.0f,
            1.0f,
            player.getRandom().nextLong()));
    }

    static void stop(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundStopSoundPacket(sound.location(), SoundSource.PLAYERS));
    }

    private static SoundEvent register(String name) {
        Identifier id = MmsDungeons.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
