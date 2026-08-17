package slimeknights.tconstruct.library.tools.definition.module.build;

import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.Collection;

/** Hook for adding direct stats to a tool. */
public interface ToolStatsHook {
  /** Adds raw stats to the tool whenever tool stats are rebuilt. */
  void addToolStats(IToolContext context, ModifierStatsBuilder builder);

  /** Merger that runs all hooks. */
  record AllMerger(Collection<ToolStatsHook> modules) implements ToolStatsHook {
    @Override
    public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
      for (ToolStatsHook module : modules) {
        module.addToolStats(context, builder);
      }
    }
  }
}
