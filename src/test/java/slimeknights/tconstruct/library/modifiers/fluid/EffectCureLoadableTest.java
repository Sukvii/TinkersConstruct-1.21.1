package slimeknights.tconstruct.library.modifiers.fluid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.EffectCures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.modifiers.fluid.entity.CureEffectsFluidEffect;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EffectCureLoadableTest extends BaseMcTest {
  @BeforeAll
  static void registerCureEffectsLoader() {
    if (FluidEffect.ENTITY_EFFECTS.getName(CureEffectsFluidEffect.LOADER) == null) {
      FluidEffect.ENTITY_EFFECTS.register(ResourceLocation.fromNamespaceAndPath("tconstruct", "cure_effects"), CureEffectsFluidEffect.LOADER);
    }
  }

  @Test
  void cureEffectUsesNeoForgeCureNameAndRoundTripsJson() {
    JsonObject json = JsonParser.parseString("""
      {"type":"tconstruct:cure_effects","cure":"milk"}
      """).getAsJsonObject();

    CureEffectsFluidEffect effect = (CureEffectsFluidEffect)FluidEffect.ENTITY_EFFECTS.deserialize(json, TypedMap.EMPTY);
    assertThat(effect.cure()).isEqualTo(EffectCures.MILK);
    assertThat(FluidEffect.ENTITY_EFFECTS.serialize(effect).getAsJsonObject()).isEqualTo(json);
  }

  @Test
  void fluidMobEffectWritesConfiguredCuresToInstance() {
    FluidMobEffect effect = new FluidMobEffect(MobEffects.POISON.value(), 200, 1, List.of(EffectCures.MILK));

    assertThat(effect.effectWithTime(200).getCures()).containsExactly(EffectCures.MILK);
  }
}
