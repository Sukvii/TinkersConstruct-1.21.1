package slimeknights.tconstruct.library.modifiers.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.modules.build.EnchantmentModule;
import slimeknights.tconstruct.library.modifiers.modules.combat.LootingModule;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("removal")
class BuilderLevelApiTest extends BaseMcTest {
  private static final LevelingInt VALUE = new LevelingInt(2, 3);

  @Test
  void enchantmentLevelOverloadIsPublicAndFeedsEveryBuilderOutput() {
    EnchantmentModule.Builder builder = EnchantmentModule.builder(Enchantments.FORTUNE);
    assertThat(builder.level(VALUE)).isSameAs(builder);
    assertLevel(builder.constant().level());
    assertLevel(builder.protection().level());
    assertLevel(builder.mainHandHarvest(ResourceLocation.fromNamespaceAndPath("tconstruct", "builder_level_test")).level());
    assertLevel(builder.armorHarvest(EquipmentSlot.CHEST).level());

    assertThat(EnchantmentModule.builder(Enchantments.FORTUNE).constant().level()).isEqualTo(LevelingInt.LEVEL);
    assertThat(EnchantmentModule.builder(Enchantments.FORTUNE).level(3).constant().level()).isEqualTo(LevelingInt.eachLevel(3));
    assertThat(EnchantmentModule.builder(Enchantments.FORTUNE).lootingLevel(VALUE).constant().level()).isEqualTo(VALUE);
    assertThat(EnchantmentModule.builder(Enchantments.FORTUNE).level(VALUE).level(3).constant().level()).isEqualTo(LevelingInt.eachLevel(3));
    assertThat(EnchantmentModule.builder(Enchantments.FORTUNE).level(3).level(VALUE).constant().level()).isEqualTo(VALUE);
  }

  @Test
  void lootingLevelOverloadIsPublicAndFeedsWeaponAndArmorBuilders() {
    LootingModule.Builder builder = LootingModule.builder();
    assertThat(builder.level(VALUE)).isSameAs(builder);
    assertLevel(builder.weapon().level());
    assertLevel(builder.armor(EquipmentSlot.CHEST).level());

    assertThat(LootingModule.builder().weapon().level()).isEqualTo(LevelingInt.LEVEL);
    assertThat(LootingModule.builder().level(3).weapon().level()).isEqualTo(LevelingInt.eachLevel(3));
    assertThat(LootingModule.builder().lootingLevel(VALUE).weapon().level()).isEqualTo(VALUE);
    assertThat(LootingModule.builder().level(VALUE).level(3).weapon().level()).isEqualTo(LevelingInt.eachLevel(3));
    assertThat(LootingModule.builder().level(3).level(VALUE).weapon().level()).isEqualTo(VALUE);
  }

  private static void assertLevel(LevelingInt actual) {
    assertThat(actual).isEqualTo(VALUE);
    assertThat(actual.compute(1)).isEqualTo(5);
    assertThat(actual.compute(4)).isEqualTo(14);
  }
}
