package ru.aiefu.rss.block;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import ru.aiefu.rss.RSS;
import ru.aiefu.rss.RemotePlayerAcc;
import ru.aiefu.rss.mixin.SoundEngineAcc;
import ru.aiefu.rss.sound.RemoteSoundInstance;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MusicPlayerClient {

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("music-delay-execute")
            .setDaemon(true)
            .build());

    private final SpeakerEntity se;
    private final UUID uuid;
    protected final AudioPlayer player = craftPlayer();
    public boolean isPlaylistPlaying;
    private SoundInstance currentInstance;
    private final ConcurrentLinkedQueue<AudioTrack> playlist = new ConcurrentLinkedQueue<>();

    private AudioPlayer craftPlayer(){
        AudioPlayer p = RSS.playerManager.createPlayer();
        p.addListener(new PlayerEventListener());
        return p;
    }

    public MusicPlayerClient(SpeakerEntity se){
        this.se = se;
        this.uuid = UUID.randomUUID();
    }

    @Environment(EnvType.CLIENT)
    public void playTrackOnClient(String currentURL, long seekTo){
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                Minecraft.getInstance().execute(() -> {
                    if(seekTo < track.getDuration()) {
                        if(track.isSeekable()) {
                            track.setPosition(seekTo);
                        }
                        isPlaylistPlaying = false;
                        play(track);
                    }
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }

    public void playPlaylistOnClient(String currentURL, long seekTo, int pos){
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                Minecraft.getInstance().execute(() -> {
                    MusicPlayerClient.this.playlist.clear();
                    List<AudioTrack> tracks = playlist.getTracks();
                    if (pos > 0) {
                        tracks.subList(0, pos).clear();
                    }
                    MusicPlayerClient.this.playlist.addAll(tracks);
                    AudioTrack track = MusicPlayerClient.this.playlist.poll();
                    if(track != null){
                        if(track.isSeekable()){
                            if(seekTo < track.getDuration())
                            track.setPosition(seekTo);
                            else track = MusicPlayerClient.this.playlist.poll();
                        }
                    }
                    if(track != null) {
                        isPlaylistPlaying = true;
                        play(track);
                    }
                });
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }
    @Environment(EnvType.CLIENT)
    public void startPlayer(String url){
        RSS.playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                Minecraft.getInstance().execute(() -> {
                    isPlaylistPlaying = false;
                    play(track);
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                Minecraft.getInstance().execute(() -> {
                    MusicPlayerClient.this.playlist.clear();
                    isPlaylistPlaying = true;
                    MusicPlayerClient.this.playlist.addAll(playlist.getTracks());
                    play(MusicPlayerClient.this.playlist.poll());
                });
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }

    @Environment(EnvType.CLIENT)
    protected void play(AudioTrack track){
        stopInstance();
        executorService.schedule(() -> Minecraft.getInstance().execute(() -> {
            player.playTrack(track);
            RemoteSoundInstance si = new RemoteSoundInstance(new ResourceLocation(RSS.MOD_ID,"lava-player-" + uuid.toString()), SoundSource.RECORDS, AudioPlayerInputStream.createStream(player, RSS.PCM_MONO_LE, 10000L, true), this.se.getBlockPos());
            getSoundEngine().playRemoteStream(si);
            currentInstance = si;
        }), 1200L, TimeUnit.MILLISECONDS);
    }

    @Environment(EnvType.CLIENT)
    public void stopPlayer(){
        stopInstance();
        player.stopTrack();
    }

    public void stopInstance(){
        if(currentInstance != null){
            SoundInstance s = currentInstance;
            Minecraft.getInstance().getSoundManager().stop(s);
            currentInstance = null;
            se.setCurrentURL(null);
        }
    }

    public void terminatePlayer() {
        if(currentInstance != null) {
            SoundInstance instance = currentInstance;
            currentInstance = null;
            player.stopTrack();
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public void playNext(){
        Minecraft.getInstance().execute(() -> {
            if (MusicPlayerClient.this.isPlaylistPlaying) {
                AudioTrack track = this.playlist.poll();
                if(track != null) {
                    MusicPlayerClient.this.play(track);
                } else {
                    isPlaylistPlaying = false;
                    stopInstance();
                }
            } else stopInstance();
        });
    }

    public static RemotePlayerAcc getSoundEngine(){
        return (RemotePlayerAcc) ((SoundEngineAcc)Minecraft.getInstance().getSoundManager()).getSoundEngine();
    }

    public class PlayerEventListener extends AudioEventAdapter {
        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if(endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.LOAD_FAILED) {
                MusicPlayerClient.this.playNext();
            } else if (endReason == AudioTrackEndReason.CLEANUP){
                Minecraft.getInstance().execute(MusicPlayerClient.this::stopInstance);
            }
        }
        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            MusicPlayerClient.this.playNext();
        }
        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            MusicPlayerClient.this.playNext();
        }
    }

}
