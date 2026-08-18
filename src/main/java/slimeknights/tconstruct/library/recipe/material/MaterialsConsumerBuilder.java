package slimeknights.tconstruct.library.recipe.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import java.util.ArrayList;
import java.util.List;

/** Adds material metadata to a vanilla shaped or shapeless recipe before it is serialized. */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MaterialsConsumerBuilder {
  private final String parts;
  private final int partCount;
  private final List<MaterialVariantId> materials = new ArrayList<>();

  /** Creates a new shaped recipe with the given ingredients as parts */
  public static MaterialsConsumerBuilder shaped(String parts) {
    if (parts.isEmpty()) {
      throw new IllegalArgumentException("Parts may not be empty");
    }
    return new MaterialsConsumerBuilder(parts, 0);
  }

  /** Creates a new shapeless recipe with the first ingredients as parts */
  public static MaterialsConsumerBuilder shapeless(int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("Parts must be greater than 0");
    }
    return new MaterialsConsumerBuilder("", parts);
  }

  /** Adds a material to the builder */
  public MaterialsConsumerBuilder material(MaterialVariantId material) {
    materials.add(material);
    return this;
  }

  /** Builds a RecipeOutput wrapper that replaces the vanilla recipe with the material-aware type. */
  public RecipeOutput build(RecipeOutput output) {
    List<MaterialVariantId> extraMaterials = List.copyOf(materials);
    return new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
        Recipe<?> wrapped;
        if (partCount > 0) {
          wrapped = new ShapelessMaterialsRecipe((ShapelessRecipe) recipe, partCount, extraMaterials);
        } else {
          wrapped = new ShapedMaterialsRecipe((ShapedRecipe) recipe, shapedParts((ShapedRecipe) recipe, parts), extraMaterials);
        }
        output.accept(id, wrapped, advancement, conditions);
      }

      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }
    };
  }

  private static List<Ingredient> shapedParts(ShapedRecipe recipe, String pattern) {
    JsonObject json = ShapedRecipe.Serializer.CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe)
      .getOrThrow(JsonParseException::new).getAsJsonObject();
    JsonObject key = json.getAsJsonObject("key");
    List<Ingredient> parts = new ArrayList<>(pattern.length());
    for (int i = 0; i < pattern.length(); i++) {
      String symbol = pattern.substring(i, i + 1);
      if (key == null || !key.has(symbol)) {
        throw new IllegalStateException("Parts references undefined symbol '" + symbol + "'");
      }
      parts.add(Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, key.get(symbol)).getOrThrow(JsonParseException::new));
    }
    return List.copyOf(parts);
  }
}
