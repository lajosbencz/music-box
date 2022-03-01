package ru.aiefu.rss.mixin;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.aiefu.rss.RemotePlayerAcc;
import ru.aiefu.rss.sound.RemoteSoundInstance;
import ru.aiefu.rss.sound.RemoteStream;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements RemotePlayerAcc {

    @Shadow private boolean loaded;

    @Shadow protected abstract float calculateVolume(SoundInstance soundInstance);

    @Shadow protected abstract float calculatePitch(SoundInstance soundInstance);

    @Shadow @Final private static Marker MARKER;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private List<SoundEventListener> listeners;

    @Shadow @Final private Listener listener;

    @Shadow @Final private ChannelAccess channelAccess;

    @Shadow private int tickCount;

    @Shadow @Final private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow @Final private Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    public void playRemoteStream(RemoteSoundInstance soundInstance){
        if (this.loaded) {
            ResourceLocation resourceLocation = soundInstance.getLocation();
            WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(resourceLocation, "Remote stream");
            Sound sound = soundInstance.getSound();
            float f = soundInstance.getVolume();
            float g = Math.max(f, 1.0F) * (float)sound.getAttenuationDistance();
            SoundSource soundSource = soundInstance.getSource();
            float h = this.calculateVolume(soundInstance);
            float i = this.calculatePitch(soundInstance);
            SoundInstance.Attenuation attenuation = soundInstance.getAttenuation();
            boolean bl = soundInstance.isRelative();
            if (h == 0.0F && !soundInstance.canStartSilent()) {
                LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", sound.getLocation());
            } else {
                Vec3 vec3 = new Vec3(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
                boolean bl2;
                if (!this.listeners.isEmpty()) {
                    bl2 = bl || attenuation == SoundInstance.Attenuation.NONE || this.listener.getListenerPosition().distanceToSqr(vec3) < (double)(g * g);
                    if (bl2) {

                        for (SoundEventListener soundEventListener : this.listeners) {
                            soundEventListener.onPlaySound(soundInstance, weighedSoundEvents);
                        }
                    } else {
                        LOGGER.debug(MARKER, "Did not notify listeners of soundEvent: {}, it is too far away to hear", resourceLocation);
                    }
                }

                if (this.listener.getGain() <= 0.0F) {
                    LOGGER.debug(MARKER, "Skipped playing soundEvent: {}, master volume was zero", resourceLocation);
                } else {
                    CompletableFuture<ChannelAccess.ChannelHandle> soundEventListener = this.channelAccess.createHandle(sound.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
                    ChannelAccess.ChannelHandle channelHandle = soundEventListener.join();
                    if (channelHandle == null) {
                        if (SharedConstants.IS_RUNNING_IN_IDE) {
                            LOGGER.warn("Failed to create new sound handle");
                        }

                    } else {
                        LOGGER.debug(MARKER, "Playing sound {} for event {}", sound.getLocation(), resourceLocation);
                        this.soundDeleteTime.put(soundInstance, this.tickCount + 20);
                        this.instanceToChannel.put(soundInstance, channelHandle);
                        this.instanceBySource.put(soundSource, soundInstance);
                        channelHandle.execute((channel) -> {
                            channel.setPitch(i);
                            channel.setVolume(h);
                            if (attenuation == SoundInstance.Attenuation.LINEAR) {
                                channel.linearAttenuation(g);
                            } else {
                                channel.disableAttenuation();
                            }

                            channel.setLooping(false);
                            channel.setSelfPosition(vec3);
                            channel.setRelative(bl);
                            channel.attachBufferStream(new RemoteStream(soundInstance.getStream()));
                            channel.play();
                        });
                        if (soundInstance instanceof TickableSoundInstance) {
                            this.tickingSounds.add((TickableSoundInstance)soundInstance);
                        }
                    }
                }
            }
        }
    }
}
