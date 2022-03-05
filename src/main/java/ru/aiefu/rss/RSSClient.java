package ru.aiefu.rss;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import ru.aiefu.rss.block.SpeakerEntity;
import ru.aiefu.rss.network.NetworkHandlerClient;

import java.io.IOException;

public class RSSClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkHandlerClient.registerClientReceivers();
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if(blockEntity instanceof SpeakerEntity se){
                se.terminatePlayer();
            }
        });
    }
    public static void openSpeakerScreen(SpeakerEntity se){
        Minecraft.getInstance().setScreen(new SpeakerGui(new TextComponent("Speaker Settings"), se));
    }
}
