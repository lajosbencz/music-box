package ru.aiefu.rss;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import ru.aiefu.rss.block.SpeakerEntity;
import ru.aiefu.rss.network.NetworkHandlerClient;

import java.io.IOException;

public class RSSClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkHandlerClient.registerClientReceivers();
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if(blockEntity instanceof SpeakerEntity se){
                try {
                    se.stopPlayer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
