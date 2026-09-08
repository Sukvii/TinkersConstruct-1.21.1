package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import slimeknights.tconstruct.gadgets.TinkerGadgets;

import java.util.function.Consumer;

/** Dispenser behavior for a fancy armor stand. */
public class DispenseFancyArmorStand extends DefaultDispenseItemBehavior {
  private final FancyArmorStandEntity.StandType type;

  public DispenseFancyArmorStand(FancyArmorStandEntity.StandType type) {
    this.type = type;
  }

  @Override
  protected ItemStack execute(BlockSource source, ItemStack stack) {
    Direction direction = source.state().getValue(DispenserBlock.FACING);
    BlockPos pos = source.pos().relative(direction);
    ServerLevel level = source.level();
    Consumer<FancyArmorStandEntity> consumer = EntityType.appendDefaultStackConfig(
      stand -> stand.setYRot(direction.toYRot()), level, stack, null);
    FancyArmorStandEntity stand = TinkerGadgets.armorStandEntity.get().spawn(level, consumer, pos, MobSpawnType.DISPENSER, false, false);
    if (stand != null) {
      stack.shrink(1);
      type.onPlace(stand);
    }
    return stack;
  }
}
