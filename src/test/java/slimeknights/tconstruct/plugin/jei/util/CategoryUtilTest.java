package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryUtilTest extends BaseMcTest {
  private LayoutProbe layout;

  @BeforeEach
  void setup() {
    layout = new LayoutProbe();
  }

  @Test
  void emptyFluidListKeepsMinimumAndAddsNoSlots() {
    int max = CategoryUtil.drawMultipleFluids(layout.builder, ignored -> RecipeIngredientRole.INPUT,
      4, 7, 50, 20, List.of(), 125, ignored -> List.of(), ignored -> (view, tooltip) -> {});

    assertThat(max).isEqualTo(125);
    assertThat(layout.xPositions).isEmpty();
  }

  @Test
  void singleFluidUsesEntireAvailableWidth() {
    FluidStack water = new FluidStack(Fluids.WATER, 900);
    int max = CategoryUtil.drawMultipleFluids(layout.builder, ignored -> RecipeIngredientRole.OUTPUT,
      4, 7, 50, 20, List.of(water), 100, List::of, ignored -> (view, tooltip) -> {});

    assertThat(max).isEqualTo(900);
    assertThat(layout.roles).containsExactly(RecipeIngredientRole.OUTPUT);
    assertThat(layout.xPositions).containsExactly(4);
    assertThat(layout.widths).containsExactly(50);
    assertThat(layout.maxAmounts).containsExactly(900L);
  }

  @Test
  void threeFluidsUseTheirRolesAndFillTheRemainder() {
    List<FluidSet> inputs = List.of(
      new FluidSet(new FluidStack(Fluids.WATER, 80), RecipeIngredientRole.CATALYST),
      new FluidSet(new FluidStack(Fluids.WATER, 140), RecipeIngredientRole.INPUT),
      new FluidSet(new FluidStack(Fluids.WATER, 160), RecipeIngredientRole.INPUT));

    int max = CategoryUtil.drawMultipleFluids(layout.builder, FluidSet::role,
      5, 9, 50, 24, inputs, 100,
      input -> List.of(input.fluid()), ignored -> (view, tooltip) -> {});

    assertThat(max).isEqualTo(160);
    assertThat(layout.roles).containsExactly(RecipeIngredientRole.CATALYST, RecipeIngredientRole.INPUT, RecipeIngredientRole.INPUT);
    assertThat(layout.xPositions).containsExactly(5, 21, 37);
    assertThat(layout.widths).containsExactly(16, 16, 18);
    assertThat(layout.widths.stream().mapToInt(Integer::intValue).sum()).isEqualTo(50);
    assertThat(layout.maxAmounts).containsExactly(160L, 160L, 160L);
  }

  private record FluidSet(FluidStack fluid, RecipeIngredientRole role) {}

  private static final class LayoutProbe {
    private final List<RecipeIngredientRole> roles = new java.util.ArrayList<>();
    private final List<Integer> xPositions = new java.util.ArrayList<>();
    private final List<Integer> widths = new java.util.ArrayList<>();
    private final List<Long> maxAmounts = new java.util.ArrayList<>();
    private final IRecipeSlotBuilder slot = proxy(IRecipeSlotBuilder.class, (instance, method, args) -> {
      if (method.getName().equals("setFluidRenderer")) {
        maxAmounts.add((Long)args[0]);
        widths.add((Integer)args[2]);
      }
      return method.getReturnType().isInstance(instance) ? instance : defaultValue(method.getReturnType());
    });
    private final IRecipeLayoutBuilder builder = proxy(IRecipeLayoutBuilder.class, (instance, method, args) -> {
      if (method.getName().equals("addSlot")) {
        roles.add((RecipeIngredientRole)args[0]);
        xPositions.add((Integer)args[1]);
        return slot;
      }
      return defaultValue(method.getReturnType());
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive() || type == void.class) return null;
    if (type == boolean.class) return false;
    if (type == char.class) return '\0';
    return 0;
  }
}
