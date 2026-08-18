package slimeknights.tconstruct.library.recipe.material;

import lombok.NoArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.ArrayList;
import java.util.List;

/** Special variant of {@link MaterialsConsumerBuilder} for the legacy {@link ShapedMaterialRecipe}. */
@Deprecated
@NoArgsConstructor(staticName = "wrap")
public class ShapedMaterialConsumerBuilder {
  private final List<MaterialVariantId> materials = new ArrayList<>();

  /** Adds a material to the builder */
  public ShapedMaterialConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  /** Builds a recipe output wrapper that creates the legacy shaped material recipe. */
  public RecipeOutput build(RecipeOutput output) {
    List<MaterialVariantId> extraMaterials = List.copyOf(materials);
    return new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
        if (!(recipe instanceof ShapedRecipe shaped)) {
          throw new IllegalArgumentException("Expected a shaped recipe, got " + recipe.getClass().getName());
        }
        output.accept(id, new ShapedMaterialRecipe(shaped, extraMaterials), advancement, conditions);
      }

      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }
    };
  }
}
