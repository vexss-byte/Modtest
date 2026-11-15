package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: tekan tombol Battle Mode. */
public record ToggleBattlePayload() implements CustomPacketPayload {
    public static final ToggleBattlePayload INSTANCE = new ToggleBattlePayload();
    public static final CustomPacketPayload.Type<ToggleBattlePayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("toggle_battle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleBattlePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
