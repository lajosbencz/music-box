package ru.aiefu.rss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SpeakerBlock extends BaseEntityBlock {
    protected SpeakerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SpeakerEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState state, BlockEntityType<T> type) {
        return l.isClientSide ? createTickerHelper(type, RSS.SPEAKER_ENTITY_TYPE, (SpeakerEntity::clientTick)) : createTickerHelper(type, RSS.SPEAKER_ENTITY_TYPE, SpeakerEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        BlockEntity e = level.getBlockEntity(blockPos);
        if(e instanceof SpeakerEntity se && livingEntity instanceof ServerPlayer p){
            se.setLevel(level);
            se.playMusic("https://www.youtube.com/watch?v=WzcFB5_UlbM", p); //https://www.youtube.com/watch?v=CbPVI9bQ4pI
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        BlockEntity e = level.getBlockEntity(blockPos);
        if(e instanceof SpeakerEntity se){
            se.stopTrack();
        }
        super.onRemove(blockState, level, blockPos, blockState2, bl);
    }
}
