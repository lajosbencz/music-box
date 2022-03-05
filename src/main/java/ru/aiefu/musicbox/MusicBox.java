package ru.aiefu.musicbox;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.Pcm16AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.aiefu.musicbox.block.SpeakerBlock;
import ru.aiefu.musicbox.block.SpeakerEntity;
import ru.aiefu.musicbox.network.NetworkHandler;

public class MusicBox implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "musicbox";

	public static final Logger LOGGER = LogManager.getLogger("Music Box");

	public static final SpeakerBlock SPEAKER = new SpeakerBlock(FabricBlockSettings.of(Material.WOOD).strength(3.0F));

	public static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

	public static BlockEntityType<SpeakerEntity> SPEAKER_ENTITY_TYPE;

	public static final AudioDataFormat PCM_MONO_LE = new Pcm16AudioDataFormat(1, 48000, 960, false);

	public static long currentTimeMS = System.currentTimeMillis();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			currentTimeMS = System.currentTimeMillis();
		});
		LOGGER.info("Hello Fabric world!");
		AudioSourceManagers.registerRemoteSources(playerManager);
		playerManager.setPlayerCleanupThreshold(60_000);
		playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
		playerManager.getConfiguration().setOutputFormat(PCM_MONO_LE);
		NetworkHandler.registerReceivers();

		Registry.register(Registry.BLOCK, new ResourceLocation(MOD_ID, "speaker"), SPEAKER);
		Registry.register(Registry.ITEM, new ResourceLocation(MOD_ID, "speaker_block"), new BlockItem(SPEAKER, new FabricItemSettings()));
		SPEAKER_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, "musicbox:speaker_entity", FabricBlockEntityTypeBuilder.create(SpeakerEntity::new, SPEAKER).build(null));
	}
}
