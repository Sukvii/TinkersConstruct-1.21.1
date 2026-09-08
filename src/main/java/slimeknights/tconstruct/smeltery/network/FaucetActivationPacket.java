package slimeknights.tconstruct.smeltery.network;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.network.PacketContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/** Packet sent to clients when a faucet starts or stops pouring. */
@RequiredArgsConstructor
@ToString
public class FaucetActivationPacket implements BlockEntityPacket<FaucetBlockEntity> {
  private final BlockPos pos;
  private final FluidStack fluid;
  private final boolean isPouring;

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer);
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, fluid);
    buffer.writeBoolean(isPouring);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<FaucetBlockEntity> type() {
    return FaucetBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(PacketContext context, FaucetBlockEntity blockEntity) {
    blockEntity.onActivationPacket(fluid, isPouring);
  }
}
