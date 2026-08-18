package slimeknights.tconstruct.tools.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.TConstruct;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Writes compatibility recipes whose inputs are supplied by optional mods. */
public class ModifierCompatRecipeProvider extends GenericDataProvider {
  private static final String THE_ONE_PROBE = "theoneprobe";

  public ModifierCompatRecipeProvider(PackOutput output) {
    super(output, Target.DATA_PACK, "");
  }

  @Override
  public CompletableFuture<?> run(CachedOutput output) {
    return allOf(List.of(
      saveJson(output, recipe("recipe/tools/modifiers/compat/the_one_probe"), modifierRecipe()),
      saveJson(output, recipe("recipe/tools/modifiers/salvage/compat/the_one_probe"), salvageRecipe())
    ));
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Modifier Compatibility Recipes";
  }

  private static ResourceLocation recipe(String path) {
    return TConstruct.getResource(path);
  }

  private static JsonArray conditions() {
    JsonObject condition = new JsonObject();
    condition.addProperty("type", "neoforge:mod_loaded");
    condition.addProperty("modid", THE_ONE_PROBE);
    JsonArray conditions = new JsonArray();
    conditions.add(condition);
    return conditions;
  }

  private static JsonArray tools() {
    JsonArray tools = new JsonArray();
    tools.add(tag("tconstruct:modifiable/armor/helmets"));
    tools.add(tag("tconstruct:modifiable/held"));
    return tools;
  }

  private static JsonObject tag(String value) {
    JsonObject tag = new JsonObject();
    tag.addProperty("tag", value);
    return tag;
  }

  private static JsonObject modifierRecipe() {
    JsonObject recipe = new JsonObject();
    recipe.addProperty("type", "tconstruct:modifier");
    recipe.addProperty("allow_crystal", true);
    recipe.addProperty("check_trait_level", true);
    recipe.add("neoforge:conditions", conditions());
    JsonArray inputs = new JsonArray();
    JsonObject probe = new JsonObject();
    probe.addProperty("item", "theoneprobe:probe");
    inputs.add(probe);
    recipe.add("inputs", inputs);
    recipe.addProperty("level", 1);
    recipe.addProperty("result", "tconstruct:the_one_probe");
    JsonObject slots = new JsonObject();
    slots.addProperty("upgrades", 1);
    recipe.add("slots", slots);
    recipe.add("tools", tools());
    return recipe;
  }

  private static JsonObject salvageRecipe() {
    JsonObject recipe = new JsonObject();
    recipe.addProperty("type", "tconstruct:modifier_salvage");
    recipe.add("neoforge:conditions", conditions());
    recipe.addProperty("modifier", "tconstruct:the_one_probe");
    JsonObject slots = new JsonObject();
    slots.addProperty("upgrades", 1);
    recipe.add("slots", slots);
    recipe.add("tools", tools());
    return recipe;
  }
}
