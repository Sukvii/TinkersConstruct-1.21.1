package slimeknights.tconstruct.smeltery.network;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.network.PacketContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;

@RequiredArgsConstructor
@ToString
public class FluidUpdatePacket implements BlockEntityPacket<FluidUpdatePacket.IFluidPacketReceiver> {

  protected final BlockPos pos;
  protected final FluidStack fluid;

  public FluidUpdatePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf)buffer);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    FluidStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf)buffer, fluid);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<IFluidPacketReceiver> type() {
    return IFluidPacketReceiver.class;
  }

  @Override
  public void handleBlockEntity(PacketContext context, IFluidPacketReceiver be) {
    be.updateFluidTo(fluid);
  }

  /** Interface to implement for anything wishing to receive fluid updates */
  public interface IFluidPacketReceiver {

    /**
     * Updates the current fluid to the specified value
     *
     * @param fluid New fluidstack
     */
    void updateFluidTo(FluidStack fluid);
  }

}
