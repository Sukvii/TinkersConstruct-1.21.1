package slimeknights.tconstruct.library.modifiers.fluid.entity;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext.Entity;

/**
 * Effect to clear all effects cured by the given cure, standard is milk
 */
public record CureEffectsFluidEffect(EffectCure cure) implements FluidEffect<FluidEffectContext.Entity> {
  public static final RecordLoadable<CureEffectsFluidEffect> LOADER = RecordLoadable.create(TinkerLoadables.EFFECT_CURE.requiredField("cure", CureEffectsFluidEffect::cure), CureEffectsFluidEffect::new);

  @Override
  public float apply(FluidStack fluid, EffectLevel level, Entity context, FluidAction action) {
    LivingEntity target = context.getLivingTarget();
    if (target != null && level.isFull()) {
      // when simulating, search the effects list directly for curative effects
      // may still be wrong if the event cancels things though, no way to safely simulate it
      if (action.simulate()) {
        return target.getActiveEffects().stream().anyMatch(effect -> effect.getCures().contains(cure)) ? 1 : 0;
      }
      return target.removeEffectsCuredBy(cure) ? 1 : 0;
    }
    return 0;
  }

  @Override
  public RecordLoadable<CureEffectsFluidEffect> getLoader() {
    return LOADER;
  }
}
