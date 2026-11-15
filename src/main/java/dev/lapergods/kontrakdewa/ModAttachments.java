package dev.lapergods.kontrakdewa;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** Data per pemain. Keduanya disinkronkan ke client pemain itu sendiri (untuk HUD). */
public final class ModAttachments {
    /** id dewa yang diikuti pemain ("ra" / "thor"); null = belum berkontrak. Permanen, ikut setelah mati. */
    public static final AttachmentType<String> CONTRACT = AttachmentRegistry.create(
            KontrakDewa.id("contract"),
            builder -> builder
                    .persistent(Codec.STRING)
                    .copyOnDeath()
                    .syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.targetOnly()));

    /** Battle Mode menyala/mati. Tidak disimpan: selalu mati lagi setelah login. */
    public static final AttachmentType<Boolean> BATTLE_MODE = AttachmentRegistry.create(
            KontrakDewa.id("battle_mode"),
            builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));

    public static void init() {
        // memaksa class ter-load supaya attachment terdaftar
    }

    private ModAttachments() {}
}
