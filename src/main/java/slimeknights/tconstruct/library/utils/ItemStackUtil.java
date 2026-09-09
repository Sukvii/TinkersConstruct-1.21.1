package slimeknights.tconstruct.library.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Compatibility accessors for Tinkers' legacy compound data stored in the 1.21 custom-data component. */
public final class ItemStackUtil {
  private ItemStackUtil() {}

  /**
   * Gets the legacy Tinkers data for read-only access.
   *
   * <p>The returned tag is live. Call {@link #getOrCreateTag(ItemStack)} or
   * {@link #mutateTag(ItemStack, Consumer)} before modifying item data.</p>
   */
  @Nullable
  public static CompoundTag getTag(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.getUnsafe();
  }

  /**
   * Gets a writable copy of the legacy Tinkers data.
   *
   * <p>Unlike 1.20 item NBT, 1.21 item stack copies may share the component
   * value by reference. Copy the component before exposing its tag so callers
   * can safely mutate the returned tag without changing another stack.</p>
   */
  public static CompoundTag getOrCreateTag(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      data = CustomData.of(new CompoundTag());
    } else {
      // CustomData#getUnsafe() is a live tag. Detach it before handing it to a
      // caller that is expected to mutate the result.
      data = data.update(tag -> {});
    }
    stack.set(DataComponents.CUSTOM_DATA, data);
    return data.getUnsafe();
  }

  /** Mutates the legacy Tinkers data without exposing a shared component tag. */
  public static void mutateTag(ItemStack stack, Consumer<CompoundTag> updater) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
  }

  public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
    if (tag == null) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
  }

  @Nullable
  public static CompoundTag getTagElement(ItemStack stack, String key) {
    CompoundTag tag = getTag(stack);
    return tag != null && tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : null;
  }

  public static CompoundTag getOrCreateTagElement(ItemStack stack, String key) {
    CompoundTag tag = getOrCreateTag(stack);
    if (!tag.contains(key, Tag.TAG_COMPOUND)) {
      tag.put(key, new CompoundTag());
    }
    return tag.getCompound(key);
  }

  public static void removeTagKey(ItemStack stack, String key) {
    mutateTag(stack, tag -> tag.remove(key));
  }
}
