package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: buka layar kontrak. currentGodId kosong ("") kalau belum berkontrak. */
public record OpenContractPayload(String godId, String currentGodId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenContractPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("open_contract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenContractPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::godId,
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::currentGodId,
            OpenContractPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
