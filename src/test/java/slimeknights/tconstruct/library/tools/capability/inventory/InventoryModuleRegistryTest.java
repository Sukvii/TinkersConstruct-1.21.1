package slimeknights.tconstruct.library.tools.capability.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.utils.RegistryAccessUtil;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryModuleRegistryTest extends BaseMcTest {
  @Test
  void enchantedStackRoundTripsWithWorldRegistries() {
    HolderLookup.Provider registries = VanillaRegistries.createLookup();
    ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
    ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    enchantments.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
    original.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

    CompoundTag serialized = InventoryModule.writeStack(original, 0, registries);
    ItemStack restored = InventoryModule.readStack(serialized, registries);

    assertThat(restored.get(DataComponents.ENCHANTMENTS)).isEqualTo(original.get(DataComponents.ENCHANTMENTS));
  }

  @Test
  void builtinRegistriesCannotSerializeDatadrivenEnchantment() {
    ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
    HolderLookup.Provider registries = VanillaRegistries.createLookup();
    ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    enchantments.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
    enchanted.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

    assertThatThrownBy(() -> InventoryModule.writeStack(enchanted, 0, RegistryAccessUtil.BUILTIN))
      .isInstanceOf(IllegalStateException.class);
  }
}
