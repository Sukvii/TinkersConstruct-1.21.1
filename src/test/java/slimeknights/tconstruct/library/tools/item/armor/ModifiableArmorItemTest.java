package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import slimeknights.tconstruct.common.TagFixture;
import slimeknights.tconstruct.fixture.MaterialFixture;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.fixture.ToolDefinitionFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.library.modifiers.ModifierFixture;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MaterialRegistryExtension.class)
class ModifiableArmorItemTest extends BaseMcTest {

  @BeforeAll
  static void initFixtures() {
    MaterialItemFixture.init();
    ModifierFixture.init();
    TagFixture.init();
  }

  @Test
  void armorDurability_doesNotDependOnTieredItemProperties() {
    // Constructing a tiered item mutates the properties passed to it. Armor
    // must therefore have its own durability component and not borrow the
    // shared unstackable tool properties.
    Item.Properties armorProperties = new Item.Properties().durability(-1);
    ModifiableArmorItem armor = new ModifiableArmorItem(
      Holder.direct(ArmorMaterials.DIAMOND.value()), ArmorItem.Type.CHESTPLATE,
      armorProperties, ToolDefinitionFixture.getStandardToolDefinition());
    new slimeknights.tconstruct.library.tools.item.ModifiableItem(
      new Item.Properties().stacksTo(1), ToolDefinitionFixture.getStandardToolDefinition());

    ItemStack stack = ToolBuildHandler.buildItemFromMaterials(armor, MaterialNBT.of(
      MaterialFixture.MATERIAL_WITH_HEAD, MaterialFixture.MATERIAL_WITH_HANDLE, MaterialFixture.MATERIAL_WITH_EXTRA));

    assertThat(stack.isDamageableItem()).isTrue();
    stack.setDamageValue(5);
    assertThat(stack.getDamageValue()).isEqualTo(5);
    assertThat(ToolStack.from(stack).getDamage()).isEqualTo(5);
  }
}
