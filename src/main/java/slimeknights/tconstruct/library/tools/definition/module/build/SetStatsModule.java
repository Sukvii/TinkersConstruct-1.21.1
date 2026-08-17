package slimeknights.tconstruct.library.tools.definition.module.build;

import net.minecraft.world.item.ArmorItem;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.modules.ArmorModuleBuilder;

import java.util.List;

/** Module to set stats on the tool. */
public record SetStatsModule(StatsNBT stats) implements ToolStatsHook, ToolModule {
  public static final RecordLoadable<SetStatsModule> LOADER = RecordLoadable.create(StatsNBT.LOADABLE.requiredField("stats", SetStatsModule::stats), SetStatsModule::new);
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SetStatsModule>defaultHooks(ToolHooks.TOOL_STATS);

  @Override
  public RecordLoadable<SetStatsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  private <T> void setStat(IToolStat<T> stat, ModifierStatsBuilder builder) {
    stat.update(builder, stats.get(stat));
  }

  @Override
  public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
    for (IToolStat<?> stat : stats.getContainedStats()) {
      setStat(stat, builder);
    }
  }

  public static ArmorBuilder armor(List<ArmorItem.Type> slots) {
    return new ArmorBuilder(slots);
  }

  public static class ArmorBuilder implements ArmorModuleBuilder<SetStatsModule> {
    private final List<ArmorItem.Type> slotTypes;
    private final StatsNBT.Builder[] builders = new StatsNBT.Builder[4];

    private ArmorBuilder(List<ArmorItem.Type> slotTypes) {
      this.slotTypes = slotTypes;
      for (ArmorItem.Type slotType : slotTypes) {
        builders[slotType.ordinal()] = StatsNBT.builder();
      }
    }

    protected StatsNBT.Builder getBuilder(ArmorItem.Type slotType) {
      StatsNBT.Builder builder = builders[slotType.ordinal()];
      if (builder == null) {
        throw new IllegalArgumentException("Unsupported slot type " + slotType);
      }
      return builder;
    }

    public <T> ArmorBuilder set(ArmorItem.Type slotType, IToolStat<T> stat, T value) {
      getBuilder(slotType).set(stat, value);
      return this;
    }

    public ArmorBuilder set(ArmorItem.Type slotType, IToolStat<Float> stat, float value) {
      return set(slotType, stat, (Float)value);
    }

    public <T> ArmorBuilder setAll(IToolStat<T> stat, T value) {
      for (ArmorItem.Type slotType : slotTypes) {
        set(slotType, stat, value);
      }
      return this;
    }

    public ArmorBuilder setAll(IToolStat<Float> stat, float value) {
      return setAll(stat, (Float)value);
    }

    public final ArmorBuilder setInOrder(IToolStat<Float> stat, float... values) {
      if (values.length != slotTypes.size()) {
        throw new IllegalStateException("Wrong number of stats set");
      }
      for (int i = 0; i < values.length; i++) {
        set(slotTypes.get(i), stat, values[i]);
      }
      return this;
    }

    public ArmorBuilder durabilityFactor(float maxDamageFactor) {
      for (ArmorItem.Type slotType : slotTypes) {
        set(slotType, ToolStats.DURABILITY, ArmorModuleBuilder.MAX_DAMAGE_ARRAY[slotType.ordinal()] * maxDamageFactor);
      }
      return this;
    }

    @Override
    public SetStatsModule build(ArmorItem.Type slot) {
      return new SetStatsModule(getBuilder(slot).build());
    }
  }
}
