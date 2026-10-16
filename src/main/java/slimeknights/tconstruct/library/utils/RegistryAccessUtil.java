package slimeknights.tconstruct.library.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.client.SafeClientAccess;

/** Registry lookups for serializing stacks outside of a level context. */
public final class RegistryAccessUtil {
  private RegistryAccessUtil() {}

  /** Static registries only. Valid for fluids, whose components never reference data-driven registries. Never use for item stacks. */
  public static final HolderLookup.Provider BUILTIN = HolderLookup.Provider.create(
    BuiltInRegistries.REGISTRY.stream().map(Registry::asLookup));

  /** Registries of the loaded world on the current logical side. Item stacks may reference data-driven registries such as enchantments. */
  public static HolderLookup.Provider current() {
    HolderLookup.Provider registries;
    if (EffectiveSide.get().isClient()) {
      registries = SafeClientAccess.getRegistryAccess();
    } else {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      registries = server == null ? null : server.registryAccess();
    }
    if (registries == null) {
      throw new IllegalStateException("No world registries available on the " + EffectiveSide.get() + " side");
    }
    return registries;
  }
}
