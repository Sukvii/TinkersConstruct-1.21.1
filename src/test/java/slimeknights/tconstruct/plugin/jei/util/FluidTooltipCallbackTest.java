package slimeknights.tconstruct.plugin.jei.util;

import com.mojang.datafixers.util.Either;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class FluidTooltipCallbackTest extends BaseMcTest {
  @Test
  void replacingAmountRetainsTextAndRichImageRows() {
    List<Either<FormattedText,TooltipComponent>> lines = new ArrayList<>();
    IRecipeSlotView slot = slotView(Optional.empty());
    ITooltipBuilder tooltip = tooltipBuilder(lines);
    Component name = Component.literal("Water");
    Component modName = Component.literal("Minecraft");
    Component amount = Component.translatable(FluidTooltipCallback.AMOUNT_KEY, 1000);
    TooltipComponent image = new TooltipComponent() {};
    lines.addAll(List.of(
      Either.left(name), Either.right(image), Either.left(amount), Either.left(modName)));

    FluidTooltipCallback.NO_AMOUNT.onRichTooltip(slot, tooltip);

    assertThat(lines).containsExactly(Either.left(name), Either.right(image), Either.left(modName));
  }

  @Test
  void missingAmountAppendsTheFluidSpecificRichRowsOnce() {
    IRecipeSlotView slot = slotView(Optional.of(new FluidStack(Fluids.WATER, 1000)));
    ITooltipBuilder tooltip = tooltipBuilder(new ArrayList<>());
    List<Either<FormattedText,TooltipComponent>> lines = tooltip.getLines();
    FluidTooltipCallback callback = (displayed, view, rows) -> rows.add(Component.literal("1000 mB"));

    callback.onRichTooltip(slot, tooltip);

    assertThat(lines).containsExactly(Either.left(Component.literal("1000 mB")));
  }

  private static IRecipeSlotView slotView(Optional<FluidStack> fluid) {
    return proxy(IRecipeSlotView.class, (instance, method, args) -> {
      if (method.getName().equals("getDisplayedIngredient")) return fluid;
      return defaultValue(method.getReturnType());
    });
  }

  private static ITooltipBuilder tooltipBuilder(List<Either<FormattedText,TooltipComponent>> lines) {
    return proxy(ITooltipBuilder.class, (instance, method, args) -> {
      if (method.getName().equals("getLines")) return lines;
      if (method.getName().equals("addAll")) {
        @SuppressWarnings("unchecked")
        List<Component> added = (List<Component>)args[0];
        added.forEach(component -> lines.add(Either.left(component)));
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
