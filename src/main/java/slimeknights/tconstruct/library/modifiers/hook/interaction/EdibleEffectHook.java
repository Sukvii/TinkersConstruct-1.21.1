package slimeknights.tconstruct.library.modifiers.hook.interaction;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.interaction.edible.EdibleModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.List;

/**
 * Hook called by {@link EdibleModule} for effects after eating other than restoring hunger and saturation.
 */
public interface EdibleEffectHook {
  /** Adds representative items and performs other effects after eating a tool. */
  void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot, int hunger, float saturation, List<ItemStack> representativeItems);

  /** Merger running all nested modules. */
  record AllMerger(Collection<EdibleEffectHook> modules) implements EdibleEffectHook {
    @Override
    public void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot, int hunger, float saturation, List<ItemStack> representativeItems) {
      for (EdibleEffectHook module : modules) {
        module.onToolEaten(tool, modifier, player, eatenSlot, hunger, saturation, representativeItems);
      }
    }
  }
}
