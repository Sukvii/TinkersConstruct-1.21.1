package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated JEI supplies clickable ingredients through {@link mezz.jei.api.gui.builder.IClickableIngredientFactory}.
 * This record remains for compatibility with callers compiled against the older API.
 */
@Deprecated(forRemoval = true)
public record ClickableIngredient<T>(IIngredientType<T> getType, T getIngredient, Rect2i getArea) implements IClickableIngredient<T>, ITypedIngredient<T>  {
  @SuppressWarnings("removal")
  @Override
  public ITypedIngredient<T> getTypedIngredient() {
    return this;
  }

  @Override
  public ClickableIngredient<T> normalize(IIngredientHelper<T> helper) {
    T normalized = helper.normalizeIngredient(getIngredient);
    return normalized == getIngredient ? this : new ClickableIngredient<>(getType, normalized, getArea);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable <V> ClickableIngredient<V> cast(IIngredientType<V> type) {
    return getType.equals(type) ? (ClickableIngredient<V>)this : null;
  }
}
