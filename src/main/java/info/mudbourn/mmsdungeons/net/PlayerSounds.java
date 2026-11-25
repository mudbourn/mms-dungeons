package info.mudbourn.mmsdungeons.net;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Plays a sound to one player at their own position so distance never mutes it.
public final class PlayerSounds {

    private PlayerSounds() {
    }

    public static void play(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
            sound,
            source,
            player.getX(),
            player.getY(),
            player.getZ(),
            volume,
            pitch,
            player.level().getRandom().nextLong()));
    }
}
