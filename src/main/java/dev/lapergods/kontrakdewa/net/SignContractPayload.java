package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: pemain menekan Sign Contract. Server memvalidasi semuanya. */
public record SignContractPayload(String godId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SignContractPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("sign_contract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignContractPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SignContractPayload::godId,
            SignContractPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
