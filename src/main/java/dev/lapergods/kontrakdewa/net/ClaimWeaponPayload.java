package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: tombol "Ambil Weapon" di layar kontrak. */
public record ClaimWeaponPayload() implements CustomPacketPayload {
    public static final ClaimWeaponPayload INSTANCE = new ClaimWeaponPayload();
    public static final CustomPacketPayload.Type<ClaimWeaponPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("claim_weapon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimWeaponPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
