package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: buka layar kontrak + info Faith pemain. currentGodId kosong ("") kalau belum berkontrak. */
public record OpenContractPayload(String godId, String currentGodId, int faithXp, int streak,
                                  int flags, int lockMinutes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenContractPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("open_contract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenContractPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::godId,
            ByteBufCodecs.STRING_UTF8, OpenContractPayload::currentGodId,
            ByteBufCodecs.VAR_INT, OpenContractPayload::faithXp,
            ByteBufCodecs.VAR_INT, OpenContractPayload::streak,
            ByteBufCodecs.VAR_INT, OpenContractPayload::flags,
            ByteBufCodecs.VAR_INT, OpenContractPayload::lockMinutes,
            OpenContractPayload::new);

    public static int flags(boolean offeredToday, boolean hasWeapon) {
        return (offeredToday ? 1 : 0) | (hasWeapon ? 2 : 0);
    }

    public boolean offeredToday() {
        return (flags & 1) != 0;
    }

    public boolean hasWeapon() {
        return (flags & 2) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
