package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import slimeknights.mantle.network.PacketContext;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

/**
 * Packet to send the current crafting recipe to a player who opens the tinker station
 */
public class UpdateTinkerStationRecipePacket implements IThreadsafePacket {
  private final BlockPos pos;
  private final ResourceLocation recipe;
  public UpdateTinkerStationRecipePacket(BlockPos pos, ResourceLocation recipe) {
    this.pos = pos;
    this.recipe = recipe;
  }

  public UpdateTinkerStationRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readResourceLocation();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeResourceLocation(recipe);
  }

  @Override
  public void handleThreadsafe(PacketContext context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(UpdateTinkerStationRecipePacket packet) {
      Minecraft mc = Minecraft.getInstance();
      Level world = mc.level;
      if (world != null) {
        ITinkerStationRecipe recipe = RecipeHelper.getRecipe(world.getRecipeManager(), packet.recipe, ITinkerStationRecipe.class).orElse(null);
        if (recipe == null) {
          TConstruct.LOG.error("Failed to update Tinker Station Recipe at {}: unknown recipe {}", packet.pos, packet.recipe);
          return;
        }

        // if the screen is open, use that to get the TE and update the screen
        boolean handled = false;
        if (mc.screen instanceof TinkerStationScreen stationScreen) {
          TinkerStationBlockEntity te = stationScreen.getTileEntity();
          if (te != null && te.getBlockPos().equals(packet.pos)) {
            te.updateRecipe(packet.recipe, recipe);
            stationScreen.updateDisplay();
            handled = true;
          }
        }
        // if the wrong screen is open or no screen, use the tile directly
        if (!handled) {
          if (BlockEntityPacket.getBlockEntity(world, packet.pos, packet) instanceof TinkerStationBlockEntity te) {
            te.updateRecipe(packet.recipe, recipe);
          } else {
            TConstruct.LOG.error("Failed to update Tinker Station Recipe at {} to {}: unable to find Tinker Station", packet.pos, packet.recipe);
          }
        }
      }
    }
  }
}
