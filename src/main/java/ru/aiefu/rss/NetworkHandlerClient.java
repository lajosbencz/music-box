package ru.aiefu.rss;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NetworkHandlerClient {

    public static void requestAudioInfo(BlockPos pos){
        ClientPlayNetworking.send(NetworkHandler.r_audio_info, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
    }

    public static void registerClientReceivers(){
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.r_audio_info, (client, handler, buf, responseSender) -> {
            ClientLevel l = Minecraft.getInstance().level;
            if(l != null){
                BlockEntity e = l.getBlockEntity(buf.readBlockPos());
                if(e instanceof SpeakerEntity se){
                    se.setData(buf.readUtf(), true, buf.readVarLong());
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.queue_track_on_client, (client, handler, buf, responseSender) -> {
            ClientLevel l = Minecraft.getInstance().level;
            if(l != null){
                BlockEntity e = l.getBlockEntity(buf.readBlockPos());
                if(e instanceof SpeakerEntity se){
                    se.setData(buf.readUtf(), false, 0);
                }
            }
        });
    }
}
