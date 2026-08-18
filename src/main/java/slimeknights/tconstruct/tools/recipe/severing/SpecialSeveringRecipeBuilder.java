package slimeknights.tconstruct.tools.recipe.severing;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.recipe.data.AbstractRecipeBuilder;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.Objects;
import java.util.function.Supplier;

/** Builder for severing recipes that have only the base chance and looting bonus as fields */
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor(staticName = "serializer")
public class SpecialSeveringRecipeBuilder extends AbstractRecipeBuilder<SpecialSeveringRecipeBuilder> {
  private final RecipeSerializer<? extends SeveringRecipe> serializer;
  private float baseChance = 0.05f;
  private float lootingBonus = 0.01f;

  /** Creates a new builder for the given serializer. */
  public static SpecialSeveringRecipeBuilder serializer(Supplier<? extends RecipeSerializer<? extends SeveringRecipe>> supplier) {
    return serializer(supplier.get());
  }

  /** Doubles the drop chances for this rare mob */
  public SpecialSeveringRecipeBuilder rareMob() {
    baseChance = 0.1f;
    lootingBonus = 0.02f;
    return this;
  }

  @Override
  public void save(RecipeOutput output) {
    save(output, Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer)));
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    output.accept(id, create(id), null);
  }

  /** Creates the concrete recipe expected by the registered loadable serializer. */
  private SeveringRecipe create(ResourceLocation id) {
    if (serializer == TinkerModifiers.playerBeheadingSerializer.get()) {
      return new PlayerBeheadingRecipe(id, baseChance, lootingBonus);
    }
    if (serializer == TinkerModifiers.snowGolemBeheadingSerializer.get()) {
      return new SnowGolemBeheadingRecipe(id, baseChance, lootingBonus);
    }
    if (serializer == TinkerModifiers.mooshroomDemushroomingSerializer.get()) {
      return new MooshroomDemushroomingRecipe(id, baseChance, lootingBonus);
    }
    if (serializer == TinkerModifiers.sheepShearing.get()) {
      return new SheepShearingRecipe(id, baseChance, lootingBonus);
    }
    throw new IllegalArgumentException("Unsupported special severing serializer: " + serializer);
  }
}
