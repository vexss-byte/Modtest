package dev.lapergods.kontrakdewa;

import dev.lapergods.kontrakdewa.net.ClaimWeaponPayload;
import dev.lapergods.kontrakdewa.net.CooldownPayload;
import dev.lapergods.kontrakdewa.net.OpenContractPayload;
import dev.lapergods.kontrakdewa.net.SignContractPayload;
import dev.lapergods.kontrakdewa.net.ToggleBattlePayload;
import dev.lapergods.kontrakdewa.net.UseSkillPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KontrakDewa implements ModInitializer {
    public static final String MOD_ID = "kontrakdewa";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int REACH = 8; // jarak maksimum pemain dari patung saat tanda tangan / ambil weapon

    /** Detik berturut-turut pemain berada di Battle Mode (untuk XP Faith). */
    private static final Map<UUID, Integer> BATTLE_SECONDS = new HashMap<>();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModConfig.load();
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
        PayloadTypeRegistry.clientboundPlay().register(CooldownPayload.TYPE, CooldownPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SignContractPayload.TYPE, SignContractPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleBattlePayload.TYPE, ToggleBattlePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UseSkillPayload.TYPE, UseSkillPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClaimWeaponPayload.TYPE, ClaimWeaponPayload.CODEC);

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
            context.server().execute(() -> SkillService.use(player, payload.slot()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ClaimWeaponPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> WeaponService.claim(player));
        });

        // Tiap detik: harga dewa (siang/malam berubah), XP Faith dari Battle Mode, anti duplikasi weapon.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            ModConfig cfg = ModConfig.get();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                BattleService.refresh(p);
                God god = BattleService.godOf(p);
                if (god != null) WeaponService.dedupe(p, god);
                if (god != null && BattleService.inBattle(p)) {
                    int s = BATTLE_SECONDS.merge(p.getUUID(), 1, Integer::sum);
                    if (s % cfg.battleXpIntervalSeconds == 0) Faith.addBattleXp(p, cfg.battleXpPerInterval);
                } else {
                    BATTLE_SECONDS.remove(p.getUUID());
                }
            }
        });

        // Weapon takdir tidak jatuh saat mati: buang yang jatuh di titik kematian, beri baru saat respawn.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            String godId = oldPlayer.getAttached(ModAttachments.CONTRACT);
            God god = godId == null ? null : God.byId(godId);
            if (god == null) return;
            if (oldPlayer.level() instanceof ServerLevel oldLevel) {
                WeaponService.discardDroppedNear(oldLevel, oldPlayer.position(), god);
            }
            if (!WeaponService.has(newPlayer, god)) WeaponService.give(newPlayer, god);
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
                    + ". Ganti kontrak = hukuman dewa (menyusul di tahap 3b)."));
            return;
        }

        player.setAttached(ModAttachments.CONTRACT, god.id);
        if (!WeaponService.has(player, god)) WeaponService.give(player, god);

        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("Kontrak dengan " + god.displayName + " aktif. Kamu menerima "
                + god.weaponName + ". Tekan B untuk Battle Mode."));
        LOGGER.info("{} menandatangani kontrak dengan {}", player.getName().getString(), god.displayName);
    }

    public static boolean isNearStatue(ServerPlayer player, God god) {
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
