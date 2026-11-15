package dev.lapergods.kontrakdewa;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** Data per pemain. Contract & Battle Mode terlihat semua pemain (untuk weapon di punggung); Faith hanya untuk pemilik. */
public final class ModAttachments {
    /** id dewa ("ra" / "thor"); null = belum berkontrak. */
    public static final AttachmentType<String> CONTRACT = AttachmentRegistry.create(
            KontrakDewa.id("contract"),
            b -> b.persistent(Codec.STRING).copyOnDeath()
                    .syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));

    /** Tidak disimpan: selalu mati lagi setelah login. */
    public static final AttachmentType<Boolean> BATTLE_MODE = AttachmentRegistry.create(
            KontrakDewa.id("battle_mode"),
            b -> b.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    public static final AttachmentType<Integer> FAITH_XP = AttachmentRegistry.create(
            KontrakDewa.id("faith_xp"),
            b -> b.persistent(Codec.INT).copyOnDeath()
                    .syncWith(ByteBufCodecs.INT, AttachmentSyncPredicate.targetOnly()));

    /** Batas XP battle harian: hari (epoch day WIB) dan XP yang sudah didapat hari itu. */
    public static final AttachmentType<Long> DAILY_DAY = AttachmentRegistry.create(
            KontrakDewa.id("daily_day"), b -> b.persistent(Codec.LONG).copyOnDeath());
    public static final AttachmentType<Integer> DAILY_XP = AttachmentRegistry.create(
            KontrakDewa.id("daily_xp"), b -> b.persistent(Codec.INT).copyOnDeath());

    /** Persembahan: hari terakhir (epoch day WIB) dan streak beruntun. */
    public static final AttachmentType<Long> OFFER_DAY = AttachmentRegistry.create(
            KontrakDewa.id("offer_day"), b -> b.persistent(Codec.LONG).copyOnDeath());
    public static final AttachmentType<Integer> STREAK = AttachmentRegistry.create(
            KontrakDewa.id("streak"), b -> b.persistent(Codec.INT).copyOnDeath());

    /** Waktu (ms) terakhir mengambil weapon di patung. */
    public static final AttachmentType<Long> WEAPON_CLAIM_AT = AttachmentRegistry.create(
            KontrakDewa.id("weapon_claim_at"), b -> b.persistent(Codec.LONG).copyOnDeath());

    /** Stack "Charged" Thor (Overcharge). Tidak disimpan; terlihat pemilik untuk HUD. */
    public static final AttachmentType<Integer> CHARGED = AttachmentRegistry.create(
            KontrakDewa.id("charged"),
            b -> b.syncWith(ByteBufCodecs.INT, AttachmentSyncPredicate.targetOnly()));

    /** Waktu (ms) sampai pemain boleh tanda tangan kontrak baru (setelah hukuman ganti kontrak). */
    public static final AttachmentType<Long> LOCK_UNTIL = AttachmentRegistry.create(
            KontrakDewa.id("lock_until"), b -> b.persistent(Codec.LONG).copyOnDeath());

    public static void init() {
        // memaksa class ter-load supaya attachment terdaftar
    }

    private ModAttachments() {}
}
