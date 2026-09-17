package me.vladosik.csu.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public enum Sounds {
    SUCCESS("entity.player.levelup", 1f),
    FAIL("entity.iron_golem.repair", 1f),
    SHUFFLE("ui.cartography_table.take_result", 1f)
    ;

    private final SoundEvent event;
    private final float pitch;

    Sounds(String id, float pitch) {
        event = BuiltInRegistries.SOUND_EVENT.get(Identifier.withDefaultNamespace(id)).orElseThrow().value();
        this.pitch = pitch;
    }

    public void playOnPlayer() { playOnPlayer(60f); }
    public void playOnPlayer(float volume) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(event, SoundSource.UI, volume, pitch, player, 0L));
    }

    public void playOnUI() { playOnUI(60f); }
    public void playOnUI(float volume) {
        var sound = SimpleSoundInstance.forUI(event, volume, pitch);
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
