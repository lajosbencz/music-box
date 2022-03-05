package ru.aiefu.rss.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import ru.aiefu.rss.RSS;
import ru.aiefu.rss.RSSClient;


public class SpeakerBlock extends BaseEntityBlock {
    public SpeakerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SpeakerEntity(blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if(level.isClientSide){
            BlockEntity e = level.getBlockEntity(blockPos);
            if(e instanceof SpeakerEntity se)
                RSSClient.openSpeakerScreen(se);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState state, BlockEntityType<T> type) {
        return l.isClientSide ? createTickerHelper(type, RSS.SPEAKER_ENTITY_TYPE, (SpeakerEntity::clientTick)) : createTickerHelper(type, RSS.SPEAKER_ENTITY_TYPE, (SpeakerEntity::serverTick));
    }
}
