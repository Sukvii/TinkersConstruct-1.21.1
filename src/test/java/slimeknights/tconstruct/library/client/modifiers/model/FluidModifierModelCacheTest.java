package slimeknights.tconstruct.library.client.modifiers.model;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FluidModifierModelCacheTest extends BaseMcTest {
  private static Object fluidKey(FluidStack fluid) {
    ToolTankHelper helper = mock(ToolTankHelper.class);
    IToolStackView tool = mock(IToolStackView.class);
    when(helper.getFluid(tool)).thenReturn(fluid);
    return new FluidModifierModel(null, null, helper).getCacheKey(tool, ModifierEntry.EMPTY);
  }

  private static Object tankKey(FluidStack fluid, int capacity) {
    ToolTankHelper helper = mock(ToolTankHelper.class);
    IToolStackView tool = mock(IToolStackView.class);
    when(helper.getFluid(tool)).thenReturn(fluid);
    when(helper.getCapacity(tool)).thenReturn(capacity);
    TankModifierModel model = new TankModifierModel(null, null, null, null, 0) {
      @Override
      protected ToolTankHelper tankHelper() {
        return helper;
      }
    };
    return model.getCacheKey(tool, ModifierEntry.EMPTY);
  }

  private static FluidStack water(PotionContents contents) {
    FluidStack fluid = new FluidStack(Fluids.WATER, 50);
    fluid.set(DataComponents.POTION_CONTENTS, contents);
    return fluid;
  }

  @Test
  void fluidCacheSeparatesPotionTypes() {
    assertThat(fluidKey(water(new PotionContents(Potions.LEAPING))))
      .isNotEqualTo(fluidKey(water(new PotionContents(Potions.HEALING))));
  }

  @Test
  void fluidCacheSeparatesCustomColorsAndEffects() {
    PotionContents colorA = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233), List.of());
    PotionContents colorB = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x445566), List.of());
    PotionContents effectA = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233),
      List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0)));
    PotionContents effectB = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233),
      List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)));

    assertThat(fluidKey(water(colorA))).isNotEqualTo(fluidKey(water(colorB)));
    assertThat(fluidKey(water(effectA))).isNotEqualTo(fluidKey(water(effectB)));
  }

  @Test
  void equalComponentCopiesReuseFluidCacheKey() {
    FluidStack original = water(new PotionContents(Potions.LEAPING));
    assertThat(fluidKey(original)).isEqualTo(fluidKey(original.copy()));
  }

  @Test
  void emptyFluidHasNoFluidOrTankCacheKey() {
    assertThat(fluidKey(FluidStack.EMPTY)).isNull();
    assertThat(tankKey(FluidStack.EMPTY, 100)).isNull();
  }

  @Test
  void tankCacheSeparatesPotionComponentsAndFillState() {
    FluidStack leaping = water(new PotionContents(Potions.LEAPING));
    FluidStack healing = water(new PotionContents(Potions.HEALING));
    assertThat(tankKey(leaping, 100)).isNotEqualTo(tankKey(healing, 100));

    PotionContents colorA = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233), List.of());
    PotionContents colorB = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x445566), List.of());
    PotionContents effectA = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233),
      List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0)));
    PotionContents effectB = new PotionContents(Optional.of(Potions.LEAPING), Optional.of(0x112233),
      List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)));
    assertThat(tankKey(water(colorA), 100)).isNotEqualTo(tankKey(water(colorB), 100));
    assertThat(tankKey(water(effectA), 100)).isNotEqualTo(tankKey(water(effectB), 100));

    FluidStack partial = new FluidStack(Fluids.WATER, 50);
    FluidStack full = new FluidStack(Fluids.WATER, 100);
    assertThat(tankKey(partial, 100)).isNotEqualTo(tankKey(full, 100));
    assertThat(tankKey(leaping, 100)).isEqualTo(tankKey(leaping.copy(), 100));
  }
}
