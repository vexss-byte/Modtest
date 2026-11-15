package dev.lapergods.kontrakdewa;

import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KontrakDewa implements ModInitializer {
    public static final String MOD_ID = "kontrakdewa";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int REACH = 8; // jarak maksimum pemain dari patung saat tanda tangan

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModAttachments.init();
        ModBlocks.init();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> {
            entries.accept(ModBlocks.RA_STATUE_ITEM);
            entries.accept(ModBlocks.THOR_STATUE_ITEM);
        });

        PayloadTypeRegistry.clientboundPlay().register(OpenContractPayload.TYPE, OpenContractPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SignContractPayload.TYPE, SignContractPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SignContractPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSign(player, payload.godId()));
        });
    }

    /** Semua aturan kontrak dicek di server (anti-eksploit): dewa valid, pemain dekat patung, belum berkontrak. */
    private static void handleSign(ServerPlayer player, String godId) {
        God god = God.byId(godId);
        if (god == null) return;
        if (!isNearStatue(player, god)) {
            player.sendSystemMessage(Component.literal("Kamu terlalu jauh dari Patung " + god.displayName + "."));
            return;
        }

        String current = player.getAttached(ModAttachments.CONTRACT);
        if (god.id.equals(current)) {
            player.sendSystemMessage(Component.literal("Kamu sudah terikat kontrak dengan " + god.displayName + "."));
            return;
        }
        if (current != null) {
            God other = God.byId(current);
            player.sendSystemMessage(Component.literal("Kamu sudah terikat kontrak dengan "
                    + (other != null ? other.displayName : current)
                    + ". Ganti kontrak = hukuman dewa (menyusul di tahap 3)."));
            return;
        }

        player.setAttached(ModAttachments.CONTRACT, god.id);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("Kontrak dengan " + god.displayName + " aktif. Kamu meminjam kekuatannya."));
        LOGGER.info("{} menandatangani kontrak dengan {}", player.getName().getString(), god.displayName);
    }

    private static boolean isNearStatue(ServerPlayer player, God god) {
        ServerLevel level = (ServerLevel) player.level();
        Block target = god == God.RA ? ModBlocks.RA_STATUE : ModBlocks.THOR_STATUE;
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dy = -REACH; dy <= REACH; dy++)
                for (int dz = -REACH; dz <= REACH; dz++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(p).is(target)) return true;
                }
        return false;
    }
}
