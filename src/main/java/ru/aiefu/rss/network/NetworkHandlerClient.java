package ru.aiefu.rss.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.aiefu.rss.block.SpeakerEntity;

public class NetworkHandlerClient {

    public static void requestAudioInfo(BlockPos pos){
        ClientPlayNetworking.send(NetworkHandler.request_client_info, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
    }

    public static void registerClientReceivers(){
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.info_to_client, (client, handler, buf, responseSender) -> {
            ClientLevel l = Minecraft.getInstance().level;
            if(l != null){
                BlockEntity e = l.getBlockEntity(buf.readBlockPos());
                if(e instanceof SpeakerEntity se){
                    String url = buf.readUtf();
                    if(buf.readBoolean()){
                        int pos = buf.readVarInt();
                        long ms = System.currentTimeMillis() - buf.readVarLong();
                        se.playPlaylistOnClient(url, ms, pos);
                    } else {
                        se.playTrackOnClient(url, System.currentTimeMillis() - buf.readVarLong());
                    }
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.start_on_client, (client, handler, buf, responseSender) -> {
           ClientLevel l = Minecraft.getInstance().level;
           if(l != null){
               BlockEntity e = l.getBlockEntity(buf.readBlockPos());
               if(e instanceof SpeakerEntity se){
                   se.startPlayer(buf.readUtf());
               }
           }
        });
    }
}
