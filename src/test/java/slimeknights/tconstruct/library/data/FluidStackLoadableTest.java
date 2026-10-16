package slimeknights.tconstruct.library.data;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression coverage for 1.21 fluid data-component serialization. */
class FluidStackLoadableTest extends BaseMcTest {
  private static final int POTION_COLOR = 0x2468AC;

  private static PotionContents potionContents() {
    return new PotionContents(Optional.of(Potions.LEAPING), Optional.of(POTION_COLOR),
      List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 2)));
  }

  private static FluidStack stack(int amount, boolean withCustomData) {
    FluidStack stack = new FluidStack(Fluids.WATER, amount);
    stack.set(DataComponents.POTION_CONTENTS, potionContents());
    if (withCustomData) {
      CompoundTag tag = new CompoundTag();
      tag.putString("regression", "fluid-stack-loadable");
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    return stack;
  }

  private static void assertPotionAndCustomData(FluidStack stack, boolean withCustomData) {
    PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
    assertThat(potion).isNotNull();
    assertThat(potion.potion()).contains(Potions.LEAPING);
    assertThat(potion.customColor()).contains(POTION_COLOR);
    assertThat(potion.customEffects()).hasSize(1);
    assertThat(potion.customEffects().getFirst().getDuration()).isEqualTo(240);
    assertThat(potion.customEffects().getFirst().getAmplifier()).isEqualTo(2);
    CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    if (withCustomData) {
      assertThat(customData.copyTag().getString("regression")).isEqualTo("fluid-stack-loadable");
    } else {
      assertThat(customData.isEmpty()).isTrue();
    }
  }

  @Test
  void recipeJsonRoundTripPreservesPotionEffectsAndCustomData() {
    FluidOutput original = FluidOutput.fromStack(stack(50, true));
    JsonObject json = FluidOutput.Loadable.REQUIRED.serialize(original).getAsJsonObject();

    assertThat(json.has("components")).isTrue();
    FluidStack restored = FluidOutput.Loadable.REQUIRED.deserialize(json, TypedMap.EMPTY).get();
    assertThat(restored.getAmount()).isEqualTo(50);
    assertPotionAndCustomData(restored, true);
  }

  @Test
  void recipeNetworkRoundTripPreservesPotionEffectsAndCustomData() {
    RegistryFriendlyByteBuf buffer = registryBuffer();
    try {
      FluidOutput.Loadable.REQUIRED.encode(buffer, FluidOutput.fromStack(stack(50, true)));
      FluidStack restored = FluidOutput.Loadable.REQUIRED.decode(buffer, TypedMap.EMPTY).get();

      assertThat(restored.getAmount()).isEqualTo(50);
      assertPotionAndCustomData(restored, true);
    } finally {
      buffer.release();
    }
  }

  @Test
  void fixedSizeNbtDoesNotCompactAwayOtherComponents() {
    FluidStack original = stack(FluidType.BUCKET_VOLUME, false);
    JsonObject json = FluidStackLoadable.OPTIONAL_BUCKET_NBT.serialize(original).getAsJsonObject();

    assertThat(json.has("components")).isTrue();
    FluidStack restored = FluidStackLoadable.OPTIONAL_BUCKET_NBT.deserialize(json, TypedMap.EMPTY);
    assertThat(restored.getAmount()).isEqualTo(FluidType.BUCKET_VOLUME);
    assertPotionAndCustomData(restored, false);
  }

  @Test
  void nbtFieldJsonStoresCustomData() {
    JsonObject json = new JsonObject();
    json.addProperty("fluid", "minecraft:water");
    json.addProperty("amount", 50);
    JsonObject legacyNbt = new JsonObject();
    legacyNbt.addProperty("legacy", "kept");
    json.add("nbt", legacyNbt);

    FluidStack restored = FluidStackLoadable.OPTIONAL_STACK_NBT.deserialize(json, TypedMap.EMPTY);

    assertThat(restored.getAmount()).isEqualTo(50);
    assertThat(restored.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("legacy"))
      .isEqualTo("kept");
  }

  @Test
  void optionalLoadableRoundTripsEmptyStack() {
    JsonObject json = FluidStackLoadable.OPTIONAL_STACK_NBT.serialize(FluidStack.EMPTY).getAsJsonObject();
    assertThat(FluidStackLoadable.OPTIONAL_STACK_NBT.deserialize(json, TypedMap.EMPTY).isEmpty()).isTrue();

    RegistryFriendlyByteBuf buffer = registryBuffer();
    try {
      FluidStackLoadable.OPTIONAL_STACK_NBT.encode(buffer, FluidStack.EMPTY);
      assertThat(FluidStackLoadable.OPTIONAL_STACK_NBT.decode(buffer, TypedMap.EMPTY).isEmpty()).isTrue();
    } finally {
      buffer.release();
    }
  }

  @Test
  void outputCopyDoesNotShareMutableComponents() {
    FluidOutput output = FluidOutput.fromStack(stack(50, true));
    FluidStack copy = output.copy();
    CompoundTag changed = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    changed.putString("regression", "changed-copy");
    copy.set(DataComponents.CUSTOM_DATA, CustomData.of(changed));
    copy.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));

    assertThat(output.get().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("regression"))
      .isEqualTo("fluid-stack-loadable");
    assertThat(output.get().get(DataComponents.POTION_CONTENTS).potion()).contains(Potions.LEAPING);
  }
}
