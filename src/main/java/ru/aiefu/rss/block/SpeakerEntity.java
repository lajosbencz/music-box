package ru.aiefu.rss.block;

import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import ru.aiefu.rss.RSS;
import ru.aiefu.rss.RemotePlayerAcc;
import ru.aiefu.rss.mixin.SoundEngineAcc;
import ru.aiefu.rss.network.NetworkHandler;
import ru.aiefu.rss.network.NetworkHandlerClient;
import ru.aiefu.rss.sound.RemoteSoundInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpeakerEntity extends BlockEntity {

    //Common Side stuff
    private final UUID uuid;
    private String currentURL;
    private long ms, currentLength;
    public boolean isPlaylistPlaying;
    private int pos, size;
    private final List<ServerTrackData> stdList = new ArrayList<>();

    //Client Side Stuff
    private final AudioPlayer player = craftPlayer();
    public boolean isPlayerListening;
    private SoundInstance currentInstance;
    private final ConcurrentLinkedQueue<AudioTrack> playlist = new ConcurrentLinkedQueue<>();

    private AudioPlayer craftPlayer(){
        AudioPlayer p = RSS.playerManager.createPlayer();
        p.addListener(new PlayerEventListener());
        return p;
    }

    public SpeakerEntity(BlockPos blockPos, BlockState blockState) {
        super(RSS.SPEAKER_ENTITY_TYPE, blockPos, blockState);
        uuid = UUID.randomUUID();
    }

    @Override
    public void load(CompoundTag compoundTag) {

    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {

    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player_id", uuid);
        return tag;
    }

    public void playTrackOnClient(String currentURL, long seekTo){
        this.currentURL = currentURL;
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                Minecraft.getInstance().execute(() -> {
                    if(seekTo < track.getDuration()) {
                        track.setPosition(seekTo);
                        isPlaylistPlaying = false;
                        player.playTrack(track);
                        play();
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
        this.currentURL = currentURL;
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                Minecraft.getInstance().execute(() -> {
                    List<AudioTrack> tracks = playlist.getTracks();
                    if (pos > 0) {
                        tracks.subList(0, pos).clear();
                    }
                    SpeakerEntity.this.playlist.addAll(tracks);
                    AudioTrack track = SpeakerEntity.this.playlist.poll();
                    if(track != null && seekTo < track.getDuration()) {
                        track.setPosition(seekTo);
                    } else {
                        track = SpeakerEntity.this.playlist.poll();
                    }
                    if(track != null) {
                        isPlaylistPlaying = true;
                        player.playTrack(track);
                        play();
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

    public void startPlayer(String url){
        this.currentURL = url;
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                isPlaylistPlaying = false;
                player.playTrack(track);
                play();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                isPlaylistPlaying = true;
                SpeakerEntity.this.playlist.addAll(playlist.getTracks());
                player.playTrack(SpeakerEntity.this.playlist.poll());
                play();
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }

    private void play(){
        RemoteSoundInstance si = new RemoteSoundInstance(new ResourceLocation(RSS.MOD_ID,"lava-player-" + uuid.toString()), SoundSource.RECORDS, AudioPlayerInputStream.createStream(player, RSS.PCM_MONO_LE, 10000L, true), this.worldPosition);
        currentInstance = si;
        getSoundEngine().playRemoteStream(si);
    }

    public void stopPlayer() throws IOException {
        if(currentInstance != null) {
            player.stopTrack();
            Minecraft.getInstance().getSoundManager().stop(currentInstance);
            currentInstance = null;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, SpeakerEntity se){
        if(se.isPlaylistPlaying && RSS.currentTimeMS > se.currentLength){
            if(se.pos + 1 < se.size){
                se.pos++;
                se.ms = RSS.currentTimeMS;
                se.currentLength = RSS.currentTimeMS + se.stdList.get(se.pos).ms;
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState blockState, SpeakerEntity se){
        if(!se.isPlayerListening && level.getGameTime() % 20 == 0 && Minecraft.getInstance().player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096){
            se.isPlayerListening = true;
            NetworkHandlerClient.requestAudioInfo(se.getBlockPos());
        }
    }

    public void processPlaylistOnServer(String url, List<ServerTrackData> data){
        currentURL = url;
        isPlaylistPlaying = true;
        stdList.clear();
        stdList.addAll(data);
        this.pos = 0;
        this.size = stdList.size();
        ms = RSS.currentTimeMS;
        currentLength = stdList.get(0).ms + ms;
        notifyAround();
    }

    public void processTrackOnServer(String url, long duration){
        currentURL = url;
        isPlaylistPlaying = false;
        stdList.clear();
        this.pos = 0;
        this.size = 0;
        ms = RSS.currentTimeMS;
        currentLength = ms + duration;
        notifyAround();
    }

    private void notifyAround(){
        int range = 64;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        List<ServerPlayer> players = level.getEntities(EntityTypeTest.forClass(ServerPlayer.class), new AABB(x - range, y - range, z - range,x + range, y + range, z + range), p -> true);
        players.forEach(p -> NetworkHandler.startTrack(p, worldPosition, currentURL));
    }

    @SuppressWarnings("unused")
    private void notifyAllAround(MinecraftServer server, ResourceKey<Level> resourceKey, BlockPos pos){
        PlayerList pl = server.getPlayerList();
        List<ServerPlayer> players = pl.getPlayers();
        double g = pl.getViewDistance() * 16;
        for (ServerPlayer serverPlayer : players) {
            if (serverPlayer.level.dimension() == resourceKey) {
                double h = pos.getX() - serverPlayer.getX();
                double j = pos.getY() - serverPlayer.getY();
                double k = pos.getZ() - serverPlayer.getZ();
                if (h * h + j * j + k * k < g * g) {
                    NetworkHandler.startTrack(serverPlayer, pos, currentURL);
                }
            }
        }
    }

    public long getMs() {
        return ms;
    }

    public int getPos() {
        return pos;
    }

    public String getCurrentURL() {
        return currentURL;
    }

    public static RemotePlayerAcc getSoundEngine(){
        return (RemotePlayerAcc) ((SoundEngineAcc)Minecraft.getInstance().getSoundManager()).getSoundEngine();
    }

    public class PlayerEventListener extends AudioEventAdapter{
        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if(SpeakerEntity.this.isPlaylistPlaying){
                Minecraft.getInstance().execute(() -> {
                    SpeakerEntity.this.player.playTrack(SpeakerEntity.this.playlist.poll());
                    SpeakerEntity.this.play();
                });
            }
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            super.onTrackException(player, track, exception);
        }

        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            super.onTrackStuck(player, track, thresholdMs);
        }
    }
    public record ServerTrackData(long ms){
    }
}
