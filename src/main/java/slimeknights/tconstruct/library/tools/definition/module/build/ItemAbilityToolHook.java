package slimeknights.tconstruct.library.tools.definition.module.build;

import net.neoforged.neoforge.common.ItemAbility;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

/** Hook that checks whether the tool can perform the given item ability. */
public interface ItemAbilityToolHook {
  /** Checks whether the tool can perform the item ability. */
  boolean canPerformAction(IToolStackView tool, ItemAbility toolAction);

  /** Merger that returns true if any nested module returns true. */
  record AnyMerger(Collection<ItemAbilityToolHook> modules) implements ItemAbilityToolHook {
    @Override
    public boolean canPerformAction(IToolStackView tool, ItemAbility toolAction) {
      for (ItemAbilityToolHook module : modules) {
        if (module.canPerformAction(tool, toolAction)) {
          return true;
        }
      }
      return false;
    }
  }
}
