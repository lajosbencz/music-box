package ru.aiefu.rss.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.aiefu.rss.RSS;
import ru.aiefu.rss.block.SpeakerEntity;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandler {
    private static final String ID = RSS.MOD_ID;

    public static final ResourceLocation request_client_info = new ResourceLocation(ID, "ns_rci");

    public static final ResourceLocation info_to_client = new ResourceLocation(ID, "ns_itc");

    public static final ResourceLocation start_on_client = new ResourceLocation(ID, "ns_soc");

    public static final ResourceLocation stop_on_client = new ResourceLocation(ID, "ns_st_oc");

    public static final ResourceLocation info_to_server = new ResourceLocation(ID, "ns_its");

    public static final ResourceLocation stop_to_server = new ResourceLocation(ID, "ns_sts");


    public static void startTrack(ServerPlayer player, BlockPos pos, String url){
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeBlockPos(pos);
        b.writeUtf(url);
        ServerPlayNetworking.send(player, start_on_client, b);
    }
    public static void stopTrack(ServerPlayer player, BlockPos pos){
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeBlockPos(pos);
        ServerPlayNetworking.send(player, stop_on_client, b);
    }

    public static void skipTrack(ServerPlayer player, BlockPos pos){

    }

    public static void registerReceivers(){
        ServerPlayNetworking.registerGlobalReceiver(request_client_info, (server, player, handler, buf, responseSender) -> {
            BlockPos p = buf.readBlockPos();
            server.execute(() -> {
                BlockEntity e = player.level.getBlockEntity(p);
                if(e instanceof SpeakerEntity se){
                    if(se.getCurrentURL() != null) {
                        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
                        b.writeUtf(se.getCurrentURL());
                        b.writeBlockPos(p);
                        if(se.isPlaylistPlaying){
                            writePlaylistData(se, b);
                        } else writeTrackData(se, b);
                        ServerPlayNetworking.send(player, info_to_client, b);
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(info_to_server, (server, player, handler, buf, responseSender) -> {
            BlockPos p = buf.readBlockPos();
            String url = buf.readUtf();
            boolean flag = buf.readBoolean();
            List<SpeakerEntity.ServerTrackData> list = new ArrayList<>();
            long duration = 0;
            if(flag){
                int size = buf.readVarInt();
                for (int i = 0; i < size; i++) {
                    list.add(new SpeakerEntity.ServerTrackData(buf.readVarLong()));
                }
            } else {
                duration = buf.readVarLong();
            }
            long finalDuration = duration;
            server.execute(() -> {
                BlockEntity e = player.level.getBlockEntity(p);
                if(e instanceof SpeakerEntity se) {
                    if (flag) {
                        se.processPlaylistOnServer(url, list);
                    } else {
                        se.processTrackOnServer(url, finalDuration);
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(stop_to_server, (server, player, handler, buf, responseSender) -> {
            BlockPos p = buf.readBlockPos();
            server.execute(() -> {
                if(player.level.getBlockEntity(p) instanceof SpeakerEntity se){
                    se.processStopOnServer();
                }
            });
        });
    }

    public static void writePlaylistData(SpeakerEntity se, FriendlyByteBuf buf){
        buf.writeBoolean(true);
        buf.writeVarLong(se.getMs() + 1000);
        buf.writeVarInt(se.getPos());
    }

    public static void writeTrackData(SpeakerEntity se, FriendlyByteBuf buf){
        buf.writeBoolean(false);
        buf.writeVarLong(se.getMs() + 1000);
    }
}
