package ru.aiefu.rss.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import ru.aiefu.rss.RSS;
import ru.aiefu.rss.network.NetworkHandler;
import ru.aiefu.rss.network.NetworkHandlerClient;

import java.util.ArrayList;
import java.util.List;

public class SpeakerEntity extends BlockEntity {

    //Common Side stuff
    private String currentURL;
    private long ms, currentLength;
    public boolean isPlaylistPlaying;
    private int pos, size;
    private final List<ServerTrackData> stdList = new ArrayList<>();

    //Client Side
    private boolean isPlayerListening;

    private MusicPlayerClient musicPlayer;

    public SpeakerEntity(BlockPos blockPos, BlockState blockState) {
        super(RSS.SPEAKER_ENTITY_TYPE, blockPos, blockState);
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT){
            musicPlayer = new MusicPlayerClient(this);
        }
    }

    @Environment(EnvType.CLIENT)
    public void playTrackOnClient(String currentURL, long seekTo){
        this.currentURL = currentURL;
        musicPlayer.playTrackOnClient(currentURL, seekTo);
    }
    @Environment(EnvType.CLIENT)
    public void playPlaylistOnClient(String currentURL, long seekTo, int pos){
        this.currentURL = currentURL;
        musicPlayer.playPlaylistOnClient(currentURL, seekTo, pos);
    }
    @Environment(EnvType.CLIENT)
    public void startPlayer(String url){
        this.isPlayerListening = true;
        this.currentURL = url;
        musicPlayer.startPlayer(url);
    }

    @Environment(EnvType.CLIENT)
    public void stopPlayer(){
        musicPlayer.stopPlayer();
    }

    @Environment(EnvType.CLIENT)
    public void terminatePlayer() {
        musicPlayer.terminatePlayer();
    }

    @SuppressWarnings("unused")
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
        if(level.getGameTime() % 20 == 0 ) {
            if (!se.isPlayerListening && Minecraft.getInstance().player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4096) {
                se.isPlayerListening = true;
                NetworkHandlerClient.requestAudioInfo(se.getBlockPos());
            } else if (se.isPlayerListening && Minecraft.getInstance().player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 4095){
                se.isPlayerListening = false;
                se.terminatePlayer();
            }
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
        notifyAround(Action.START);
    }

    public void processTrackOnServer(String url, long duration){
        currentURL = url;
        isPlaylistPlaying = false;
        stdList.clear();
        this.pos = 0;
        this.size = 0;
        ms = RSS.currentTimeMS;
        currentLength = ms + duration;
        notifyAround(Action.START);
    }

    public void processStopOnServer(){
        currentURL = null;
        isPlaylistPlaying = false;
        stdList.clear();
        this.pos = 0;
        this.size = 0;
        notifyAround(Action.STOP);
    }

    private void notifyAround(Action action){
        int range = 64;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        List<ServerPlayer> players = level.getEntities(EntityTypeTest.forClass(ServerPlayer.class), new AABB(x - range, y - range, z - range,x + range, y + range, z + range), p -> true);
        if(action == Action.START) {
            players.forEach(p -> NetworkHandler.startTrack(p, worldPosition, currentURL));
        }
        else if(action == Action.STOP){
            players.forEach(p -> NetworkHandler.stopTrack(p, worldPosition));
        }
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

    public void setCurrentURL(String currentURL) {
        this.currentURL = currentURL;
    }

    public record ServerTrackData(long ms){
    }
    public enum Action{
        START,
        STOP,
        SKIP
    }
}
