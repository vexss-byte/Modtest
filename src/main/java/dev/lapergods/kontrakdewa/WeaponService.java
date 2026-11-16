package dev.lapergods.kontrakdewa;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Weapon takdir: hanya 1 per pemain, tidak jatuh saat mati, bisa diambil lagi di patung. */
public final class WeaponService {
    public static boolean has(Player p, God god) {
        Item w = ModItems.weaponFor(god);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(w)) return true;
        return false;
    }

    public static boolean holding(Player p, God god) {
        return p.getMainHandItem().is(ModItems.weaponFor(god));
    }

    public static void give(ServerPlayer p, God god) {
        ItemStack weapon = new ItemStack(ModItems.weaponFor(god));
        if (!p.getInventory().add(weapon)) {
            p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), weapon));
        }
    }

    /** Ambil ulang weapon di patung (tombol di layar kontrak). */
    public static void claim(ServerPlayer p) {
        God god = BattleService.godOf(p);
        if (god == null) return;
        if (!KontrakDewa.isNearStatue(p, god)) {
            p.sendSystemMessage(Component.literal("Kamu terlalu jauh dari Patung " + god.displayName + "."));
            return;
        }
        if (has(p, god)) {
            p.sendSystemMessage(Component.literal("Kamu masih membawa " + god.weaponName + "."), true);
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = ModConfig.get().weaponClaimCooldownSeconds * 1000L;
        long left = p.getAttachedOrElse(ModAttachments.WEAPON_CLAIM_AT, 0L) + cooldown - now;
        if (left > 0) {
            p.sendSystemMessage(Component.literal("Weapon bisa diambil lagi dalam " + (left / 1000 + 1) + " detik."));
            return;
        }
        p.setAttached(ModAttachments.WEAPON_CLAIM_AT, now);
        give(p, god);
        p.sendSystemMessage(Component.literal(god.displayName + " mengembalikan " + god.weaponName + " kepadamu."));
    }

    /** Hapus semua weapon dewa ini dari inventori (dipakai hukuman ganti kontrak). */
    public static void removeAll(ServerPlayer p, God god) {
        Item w = ModItems.weaponFor(god);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(w)) inv.setItem(i, ItemStack.EMPTY);
    }

    /**
     * Dipanggil tiap tick untuk pemain yang sedang Battle Mode: weapon harus ada tepat 1, dan selalu di tangan utama.
     * Weapon dimunculkan di slot hotbar terpilih (item lama dipindah), dan pemain dipaksa tetap memegangnya.
     */
    public static void enforce(ServerPlayer p, God god) {
        Inventory inv = p.getInventory();
        Item w = ModItems.weaponFor(god);
        int found = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(w)) continue;
            if (found < 0) {
                found = i;
                if (s.getCount() > 1) s.setCount(1);
            } else {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        int sel = inv.getSelectedSlot();
        if (found < 0) {
            ItemStack old = inv.getItem(sel);
            inv.setItem(sel, new ItemStack(w));
            if (!old.isEmpty() && !inv.add(old)) {
                p.level().addFreshEntity(new ItemEntity(p.level(), p.getX(), p.getY(), p.getZ(), old));
            }
            return;
        }
        if (found == sel) return;
        if (Inventory.isHotbarSlot(found)) {
            inv.setSelectedSlot(found);
            p.connection.send(new ClientboundSetHeldSlotPacket(found));
        } else {
            ItemStack selStack = inv.getItem(sel);
            inv.setItem(sel, inv.getItem(found));
            inv.setItem(found, selStack);
        }
    }

    /** Hapus weapon berlebih (anti duplikasi). */
    public static void dedupe(ServerPlayer p, God god) {
        Item w = ModItems.weaponFor(god);
        Inventory inv = p.getInventory();
        boolean seen = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(w)) continue;
            if (seen || s.getCount() > 1) {
                if (!seen) { s.setCount(1); seen = true; } else inv.setItem(i, ItemStack.EMPTY);
            } else seen = true;
        }
    }

    /** Hapus weapon yang jatuh di titik kematian (weapon takdir tidak jatuh saat mati). */
    public static void discardDroppedNear(ServerLevel level, Vec3 pos, God god) {
        Item w = ModItems.weaponFor(god);
        AABB box = new AABB(pos, pos).inflate(6);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(w))) e.discard();
    }

    private WeaponService() {}
}
