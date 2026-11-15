package dev.lapergods.kontrakdewa.net;

import dev.lapergods.kontrakdewa.KontrakDewa;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: tekan tombol skill. slot 0..2 = Z/C/V. */
public record UseSkillPayload(int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UseSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(KontrakDewa.id("use_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseSkillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UseSkillPayload::slot,
            UseSkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
