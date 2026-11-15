package dev.lapergods.kontrakdewa;

import com.mojang.serialization.MapCodec;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GodStatueBlock extends BaseEntityBlock {
    public static final MapCodec<GodStatueBlock> CODEC = simpleCodec(p -> new GodStatueBlock(God.RA, p));

    // Bentuk interaksi sementara (model aslinya besar, ~3,6 x 6,4 blok). Disetel lagi nanti.
    private static final VoxelShape SHAPE = Block.box(-8, 0, -8, 24, 32, 24);

    private final God god;

    public GodStatueBlock(God god, Properties properties) {
        super(properties);
        this.god = god;
    }

    public God god() {
        return god;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return god == God.RA ? new GodStatueBlockEntity.Ra(pos, state) : new GodStatueBlockEntity.Thor(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // digambar oleh renderer GeckoLib
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // Alat tes: Creative + Shift + klik kanan = reset kontrak (nanti diganti command admin).
        if (sp.isCreative() && sp.isShiftKeyDown()) {
            sp.removeAttached(ModAttachments.CONTRACT);
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Tes] Kontrak direset."));
            return InteractionResult.SUCCESS;
        }

        String current = sp.getAttached(ModAttachments.CONTRACT);
        ServerPlayNetworking.send(sp, new OpenContractPayload(god.id, current == null ? "" : current));
        return InteractionResult.SUCCESS;
    }
}
