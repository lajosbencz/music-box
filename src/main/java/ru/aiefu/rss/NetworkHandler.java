package ru.aiefu.rss;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NetworkHandler {
    private static final String ID = RSS.MOD_ID;
    public static final ResourceLocation send_playing_track = new ResourceLocation(ID, "ns_play_track");

    public static final ResourceLocation r_audio_info = new ResourceLocation(ID, "ns_r_ainfo");
    public static final ResourceLocation info_to_client = new ResourceLocation(ID, "ns_i_to_client");

    public static final ResourceLocation queue_track_on_client = new ResourceLocation(ID, "ns_q_track");


    public static void sendPlayerInfo(ServerPlayer player, BlockPos pos){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(player, send_playing_track, buf);
    }

    public static void startTrack(ServerPlayer player, BlockPos pos, String url){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        ServerPlayNetworking.send(player, queue_track_on_client, buf);
    }

    public static void registerReceivers(){
        ServerPlayNetworking.registerGlobalReceiver(r_audio_info, (server, player, handler, buf, responseSender) -> {
            BlockPos p = buf.readBlockPos();
            BlockEntity e = player.level.getBlockEntity(p);
            if(e instanceof SpeakerEntity se){
                if(se.getCurrentURL() != null) {
                    FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
                    b.writeBlockPos(p);
                    b.writeUtf(se.getCurrentURL());
                    b.writeVarLong(se.getMs());
                    ServerPlayNetworking.send(player, info_to_client, b);
                }
            }
        });
    }
}
