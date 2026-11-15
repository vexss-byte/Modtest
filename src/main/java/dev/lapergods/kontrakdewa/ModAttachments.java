package dev.lapergods.kontrakdewa;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Data permanen per pemain (tersimpan di data player, ikut setelah mati). */
public final class ModAttachments {
    /** id dewa yang diikuti pemain ("ra" / "thor"); null = belum berkontrak. */
    public static final AttachmentType<String> CONTRACT = AttachmentRegistry.create(
            KontrakDewa.id("contract"),
            builder -> builder.persistent(Codec.STRING).copyOnDeath());

    public static void init() {
        // memaksa class ter-load supaya attachment terdaftar
    }

    private ModAttachments() {}
}
