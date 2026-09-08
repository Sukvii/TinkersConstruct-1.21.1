package slimeknights.tconstruct.library.modifiers.hook.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;
import java.util.Collection;

/** Hook run after a tool's durability changes. It can observe the change but cannot alter its amount. */
public interface ToolDurabilityChangedHook {
  /**
   * Called after a tool is damaged by any cause.
   * @param tool       Tool stack
   * @param modifier   Modifier running this hook
   * @param amount     Actual damage applied after clamping
   * @param holder     Entity holding the tool
   * @param stack      ItemStack containing the tool, or {@link ItemStack#EMPTY}
   */
  default void afterDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, ItemStack stack) {}

  /**
   * Called after a tool is repaired by any cause.
   * @param tool       Tool stack
   * @param modifier   Modifier running this hook
   * @param amount     Actual damage repaired after clamping
   */
  default void afterRepairTool(IToolStackView tool, ModifierEntry modifier, int amount) {}

  /** Merges all durability change hooks in modifier order. */
  record Merger(Collection<ToolDurabilityChangedHook> modules) implements ToolDurabilityChangedHook {
    @Override
    public void afterDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, ItemStack stack) {
      for (ToolDurabilityChangedHook module : modules) {
        module.afterDamageTool(tool, modifier, amount, holder, stack);
      }
    }

    @Override
    public void afterRepairTool(IToolStackView tool, ModifierEntry modifier, int amount) {
      for (ToolDurabilityChangedHook module : modules) {
        module.afterRepairTool(tool, modifier, amount);
      }
    }
  }
}
