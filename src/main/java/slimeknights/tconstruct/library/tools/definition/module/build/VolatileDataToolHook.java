package slimeknights.tconstruct.library.tools.definition.module.build;

import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import java.util.Collection;

/** Hook for data that resets every time tool modifiers or stats are rebuilt. */
public interface VolatileDataToolHook {
  /** Adds relevant volatile data to the tool data. */
  void addVolatileData(IToolContext context, ToolDataNBT volatileData);

  /** Merger that runs all hooks. */
  record AllMerger(Collection<VolatileDataToolHook> modules) implements VolatileDataToolHook {
    @Override
    public void addVolatileData(IToolContext context, ToolDataNBT volatileData) {
      for (VolatileDataToolHook module : modules) {
        module.addVolatileData(context, volatileData);
      }
    }
  }
}
