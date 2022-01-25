package ru.aiefu.rss;

import net.fabricmc.api.ClientModInitializer;

public class RSSClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NetworkHandlerClient.registerClientReceivers();
    }
}
