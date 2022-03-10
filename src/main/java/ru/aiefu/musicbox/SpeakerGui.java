package ru.aiefu.musicbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import ru.aiefu.musicbox.block.SpeakerEntity;
import ru.aiefu.musicbox.network.NetworkHandler;

public class SpeakerGui extends Screen {

    private int centerX, centerY;
    protected EditBox urlBox;
    protected Button doneButton;
    protected Button stopButton;
    private final SpeakerEntity e;

    public SpeakerGui(Component component, SpeakerEntity e) {
        super(component);
        this.e = e;
    }

    @Override
    protected void init() {
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;

        this.urlBox = new EditBox(this.font, centerX - 150, centerY, 300, 20, new TextComponent("Track url"));
        this.urlBox.setMaxLength(2000);
        this.addWidget(urlBox);
        String currentURL = e.getCurrentURL();
        if(currentURL != null){
            this.urlBox.setValue(currentURL);
        }
        this.setInitialFocus(urlBox);
        this.urlBox.setFocus(true);

        this.doneButton = this.addRenderableWidget(new Button(centerX + 60, centerY + 25, 90, 20, new TranslatableComponent("musicbox.gui.play"), button -> {
            String url = urlBox.getValue();
            if(!url.isEmpty()) {
                MusicBox.playerManager.loadItem(url, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        sendTrackInfo(track, url);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        sendPlaylistInfo(playlist, url);
                    }

                    @Override
                    public void noMatches() {

                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {

                    }
                });
            }
        }));
        this.stopButton = this.addRenderableWidget(new Button(centerX - 45, centerY + 25, 90, 20, new TranslatableComponent("musicbox.gui.stop"), button -> sendStop()));
        this.addRenderableWidget(new Button(centerX - 150, centerY + 25, 90, 20, CommonComponents.GUI_CANCEL, button -> this.onClose()));
    }

    private void sendStop(){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(e.getBlockPos());
        ClientPlayNetworking.send(NetworkHandler.stop_to_server, buf);
    }

    private void sendTrackInfo(AudioTrack track, String url){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(e.getBlockPos());
        buf.writeUtf(url);
        buf.writeBoolean(false);
        buf.writeVarLong(track.getDuration());
        ClientPlayNetworking.send(NetworkHandler.info_to_server, buf);
    }

    private void sendPlaylistInfo(AudioPlaylist list, String url){
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(e.getBlockPos());
        buf.writeUtf(url);
        buf.writeBoolean(true);
        buf.writeVarInt(list.getTracks().size());
        list.getTracks().forEach(track -> buf.writeVarLong(track.getDuration()));
        ClientPlayNetworking.send(NetworkHandler.info_to_server, buf);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        String s = urlBox.getValue();
        super.resize(minecraft, i, j);
        urlBox.setValue(s);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if(urlBox.keyPressed(i, j, k)){
            return true;
        } else return super.keyPressed(i, j, k);
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        this.urlBox.render(poseStack, i, j, f);
        super.render(poseStack, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
