package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: buka layar kontrak + info Faith pemain. currentGodId kosong ("") kalau belum berkontrak. */
public record OpenContractPayload(String godId, String currentGodId, int faithXp, int streak,
                                  boolean offeredToday, boolean hasWeapon) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenContractPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("open_contract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenContractPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::godId,
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::currentGodId,
            ByteBufCodecs.VAR_INT, OpenContractPayload::faithXp,
            ByteBufCodecs.VAR_INT, OpenContractPayload::streak,
            ByteBufCodecs.BOOL, OpenContractPayload::offeredToday,
            ByteBufCodecs.BOOL, OpenContractPayload::hasWeapon,
            OpenContractPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
