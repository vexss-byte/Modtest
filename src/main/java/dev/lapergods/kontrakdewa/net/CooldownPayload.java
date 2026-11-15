package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: slot skill mulai cooldown selama N tick (untuk HUD). */
public record CooldownPayload(int slot, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CooldownPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CooldownPayload::slot,
            ByteBufCodecs.VAR_INT, CooldownPayload::ticks,
            CooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
