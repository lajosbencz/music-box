package ru.aiefu.musicbox.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.aiefu.musicbox.block.SpeakerEntity;

public class NetworkHandlerClient {

    public static void requestAudioInfo(BlockPos pos){
        ClientPlayNetworking.send(NetworkHandler.request_client_info, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
    }

    public static void registerClientReceivers(){
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.info_to_client, (client, handler, buf, responseSender) -> {
            String url = buf.readUtf();
            BlockPos p = buf.readBlockPos();
            boolean flag = buf.readBoolean();
            long ms = System.currentTimeMillis() - buf.readVarLong();
            int pos = 0;
            if(flag){
                pos = buf.readVarInt();
            }
            int finalPos = pos;
            Minecraft.getInstance().execute(() -> {
                ClientLevel l = Minecraft.getInstance().level;
                if(l != null){
                    BlockEntity e = l.getBlockEntity(p);
                    if(e instanceof SpeakerEntity se){
                        if(flag){
                            se.playPlaylistOnClient(url, ms, finalPos);
                        } else {
                            se.playTrackOnClient(url, ms);
                        }
                    }
                }
            });
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
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.stop_on_client, (client, handler, buf, responseSender) -> {
            ClientLevel l = Minecraft.getInstance().level;
            if(l != null){
                BlockEntity e = l.getBlockEntity(buf.readBlockPos());
                if(e instanceof SpeakerEntity se){
                    se.stopPlayer();
                }
            }
        });
    }
}
