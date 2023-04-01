package ru.aiefu.musicbox.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

import javax.sound.sampled.AudioInputStream;

public class RemoteSoundInstance extends AbstractSoundInstance {
    private final AudioInputStream stream;

    public RemoteSoundInstance(ResourceLocation loc, SoundSource soundSource, AudioInputStream stream, BlockPos pos) {
        super(loc, soundSource, SoundInstance.createUnseededRandom());
        this.stream = stream;
        this.sound = new Sound(this.getLocation().toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.SOUND_EVENT, true, false, 24);
        this.relative = false;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();

    }

    public AudioInputStream getStream() {
        return stream;
    }
}
