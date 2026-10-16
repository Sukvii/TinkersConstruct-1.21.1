package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for use with our extensions of resource location for some type safety in IDs.
 * Note we left {@link ResourceLocation#withPath(String)} and alike as returning {@link ResourceLocation} as there is not much use extending an ID.
 * @see IdParser
 */
public abstract class ResourceId extends ResourceLocation {
  public ResourceId(ResourceLocation location) {
    super(location.getNamespace(), location.getPath());
  }

  public ResourceId(String namespace, String path) {
    super(namespace, path);
  }

  public ResourceId(String location) {
    this(ResourceLocation.parse(location));
  }


  /* Helpers for static constructors */

  /**
   * Creates a new ID from the given string
   * @param string  String
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceLocation> T tryParse(String string, BiFunction<String,String,T> constructor) {
    ResourceLocation location = ResourceLocation.tryParse(string);
    return location == null ? null : constructor.apply(location.getNamespace(), location.getPath());
  }

  /**
   * Creates a new ID from the given namespace and path
   * @param namespace  Namespace
   * @param path       Path
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends ResourceLocation> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (isValidNamespace(namespace) && isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
