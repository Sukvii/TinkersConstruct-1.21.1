package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.special.CapacityBarHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Module allowing draining fluids matching a predicate as a capacity bar. */
public record FluidPredicateAsCapacityModule(ToolTankHelper helper, IJsonPredicate<Fluid> fluid) implements ModifierModule, CapacityBarHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<FluidPredicateAsCapacityModule>defaultHooks(ModifierHooks.CAPACITY_BAR);
  public static final RecordLoadable<FluidPredicateAsCapacityModule> LOADER = RecordLoadable.create(
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, FluidPredicateAsCapacityModule::helper),
    FluidPredicate.LOADER.defaultField("fluid", FluidPredicateAsCapacityModule::fluid),
    FluidPredicateAsCapacityModule::new);

  public FluidPredicateAsCapacityModule(IJsonPredicate<Fluid> fluid) {
    this(ToolTankHelper.TANK_HELPER, fluid);
  }

  @Override
  public RecordLoadable<FluidPredicateAsCapacityModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public int getAmount(IToolStackView tool) {
    FluidStack fluid = helper.getFluid(tool);
    return !fluid.isEmpty() && this.fluid.matches(fluid.getFluid()) ? fluid.getAmount() : 0;
  }

  @Override
  public int getCapacity(IToolStackView tool, ModifierEntry entry) {
    FluidStack fluid = helper.getFluid(tool);
    return fluid.isEmpty() || this.fluid.matches(fluid.getFluid()) ? helper.getCapacity(tool) : 0;
  }

  @Override
  public void setAmount(IToolStackView tool, ModifierEntry entry, int amount) {
    FluidStack fluid = helper.getFluid(tool);
    if (!fluid.isEmpty() && amount < fluid.getAmount() && this.fluid.matches(fluid.getFluid())) {
      helper.setFluid(tool, fluid.copyWithAmount(amount));
    }
  }
}
