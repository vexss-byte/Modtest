package dev.lapergods.kontrakdewa;

import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import dev.lapergods.kontrakdewa.net.ToggleBattlePayload;
import dev.lapergods.kontrakdewa.net.UseSkillPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
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
        ModItems.init();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> {
            entries.accept(ModBlocks.RA_STATUE_ITEM);
            entries.accept(ModBlocks.THOR_STATUE_ITEM);
            entries.accept(ModItems.SUN_STAFF);
            entries.accept(ModItems.THUNDER_HAMMER);
        });

        PayloadTypeRegistry.clientboundPlay().register(OpenContractPayload.TYPE, OpenContractPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SignContractPayload.TYPE, SignContractPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleBattlePayload.TYPE, ToggleBattlePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UseSkillPayload.TYPE, UseSkillPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SignContractPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSign(player, payload.godId()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ToggleBattlePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> BattleService.toggle(player));
        });
        ServerPlayNetworking.registerGlobalReceiver(UseSkillPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSkill(player, payload.slot()));
        });

        // Tiap detik: sinkronkan harga dewa (siang/malam berubah terus).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) BattleService.refresh(p);
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

        // Weapon takdir
        ItemStack weapon = new ItemStack(ModItems.weaponFor(god));
        if (!player.getInventory().add(weapon)) {
            // Inventory penuh: jatuhkan senjata di kaki pemain
            ItemEntity droppedWeapon = new ItemEntity(
                    player.level(), player.getX(), player.getY(), player.getZ(), weapon);
            player.level().addFreshEntity(droppedWeapon);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("Kontrak dengan " + god.displayName + " aktif. Kamu menerima "
                + god.weaponName + ". Tekan B untuk Battle Mode."));
        LOGGER.info("{} menandatangani kontrak dengan {}", player.getName().getString(), god.displayName);
    }

    /** Skill sebenarnya menyusul di tahap 3; sekarang hanya validasi + pesan. */
    private static void handleSkill(ServerPlayer player, int slot) {
        if (slot < 0 || slot > 2) return;
        God god = BattleService.godOf(player);
        if (god == null) return;
        if (!BattleService.inBattle(player)) {
            player.sendSystemMessage(Component.literal("Aktifkan Battle Mode dulu (tekan B)."), true);
            return;
        }
        player.sendSystemMessage(Component.literal(god.skills.get(slot) + " belum aktif (menyusul di tahap 3)."), true);
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
