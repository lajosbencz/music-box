package ru.aiefu.rss;

import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.aiefu.rss.mixin.SoundEngineAcc;
import ru.aiefu.rss.sound.RemoteSoundInstance;

import javax.sound.sampled.AudioInputStream;
import java.util.List;
import java.util.UUID;

public class SpeakerEntity extends BlockEntity {

    private final AudioPlayer player = RSS.playerManager.createPlayer();
    private AudioInputStream stream;
    private UUID uuid = UUID.randomUUID();

    private String currentURL;
    private long ms;

    public SpeakerEntity(BlockPos blockPos, BlockState blockState) {
        super(RSS.SPEAKER_ENTITY_TYPE, blockPos, blockState);
    }

    public void setData(String currentURL, boolean seek, long seekTo){
        this.currentURL = currentURL;
        RSS.playerManager.loadItem(currentURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if(seek){
                    track.setPosition(System.currentTimeMillis() - seekTo);
                }
                player.playTrack(track);
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
        stream = AudioPlayerInputStream.createStream(player, RSS.PCM_MONO_BE, 10000L, true);
        System.out.println(uuid.toString());
        RemoteSoundInstance si = new RemoteSoundInstance(new ResourceLocation(RSS.MOD_ID,"lava-player-" + uuid.toString()), SoundSource.RECORDS, stream, this.worldPosition);
        ((RemotePlayerAcc) ((SoundEngineAcc)Minecraft.getInstance().getSoundManager()).getSoundEngine()).playRemoteStream(si);

    }

    public void seekTo(long pos){
        player.getPlayingTrack().setPosition(pos);
    }


    public static void serverTick(Level level, BlockPos pos, BlockState state, SpeakerEntity speaker) {

    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpeakerEntity speaker) {

    }

    public void stopTrack(){
        player.stopTrack();
    }

    public void playMusic(String http, ServerPlayer player){
        this.player.setVolume(40);
        if(!level.isClientSide) {
            RSS.playerManager.loadItem(http, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    SpeakerEntity.this.ms = System.currentTimeMillis();
                    currentURL = http;
                    notifyAllAround(player, player.level.dimension(), worldPosition, 64.0D);
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
    }

    private void notifyAllAround(ServerPlayer player, ResourceKey<Level> resourceKey, BlockPos pos, double g){
        List<ServerPlayer> players = player.getServer().getPlayerList().getPlayers();
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

    public String getCurrentURL() {
        return currentURL;
    }
}
