package slimeknights.tconstruct.plugin.jei.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import slimeknights.mantle.plugin.jei.entity.EntityIngredientHelper;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.mantle.recipe.ingredient.EntityIngredient.EntityInput;
import slimeknights.tconstruct.test.BaseMcTest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class EntityIngredientSpawnEggTest extends BaseMcTest {
  private final EntityIngredientHelper helper = new EntityIngredientHelper();

  @Test
  void cheatStackIncludesVanillaSpawnEgg() {
    ItemStack result = helper.getCheatItemStack(new EntityInput(EntityType.PIG));

    assertThat(result.getItem()).isSameAs(Items.PIG_SPAWN_EGG);
    assertThat(result.getCount()).isEqualTo(1);
  }

  @Test
  void cheatStackKeepsDeferredModSpawnEgg() throws ReflectiveOperationException {
    withDeferredEgg(EntityType.COW, egg -> {
      assertThat(SpawnEggItem.byId(EntityType.COW)).isSameAs(egg);
      assertThat(helper.getCheatItemStack(new EntityInput(EntityType.COW)).getItem()).isSameAs(egg.asItem());
    });
  }

  @Test
  void entityWithoutSpawnEggStaysEmpty() {
    assertThat(helper.getCheatItemStack(new EntityInput(EntityType.PLAYER)).isEmpty()).isTrue();
    assertThat(EntityIngredient.of(EntityType.PLAYER).getEggs()).singleElement()
      .matches(ItemStack::isEmpty);
  }

  @Test
  void mixedIngredientEggsMatchDisplayOrderForVanillaDeferredAndMissingEggs() throws ReflectiveOperationException {
    withDeferredEgg(EntityType.COW, egg -> {
      EntityIngredient ingredient = EntityIngredient.of(EntityType.PIG, EntityType.COW, EntityType.PLAYER);
      List<EntityInput> display = ingredient.getDisplay();
      List<ItemStack> eggs = ingredient.getEggs();

      assertThat(SpawnEggItem.byId(EntityType.COW)).isSameAs(egg);
      assertThat(display).extracting(EntityInput::type)
        .containsExactly(EntityType.PIG, EntityType.COW, EntityType.PLAYER);
      assertThat(eggs).hasSameSizeAs(display);
      assertThat(eggs.get(0).getItem()).isSameAs(Items.PIG_SPAWN_EGG);
      assertThat(eggs.get(1).getItem()).isSameAs(Items.COW_SPAWN_EGG);
      assertThat(eggs.get(2).isEmpty()).isTrue();
      for (int i = 0; i < display.size(); i++) {
        SpawnEggItem expected = SpawnEggItem.byId(display.get(i).type());
        if (expected == null) {
          assertThat(eggs.get(i).isEmpty()).isTrue();
        } else {
          assertThat(eggs.get(i).getItem()).isSameAs(expected.asItem());
        }
      }
    });
  }

  private static void withDeferredEgg(EntityType<? extends Mob> type, Consumer<DeferredSpawnEggItem> assertion)
      throws ReflectiveOperationException {
    Map<EntityType<?>, SpawnEggItem> deferredByType = deferredByType();
    // The unit-test launcher has frozen the item registry, so model an already-registered deferred egg in its lookup table.
    DeferredSpawnEggItem egg = Mockito.mock(DeferredSpawnEggItem.class);
    // ItemStack needs a registered item as the mock's backing stack item.
    Mockito.when(egg.asItem()).thenReturn(Items.COW_SPAWN_EGG);
    SpawnEggItem previous = deferredByType.put(type, egg);
    try {
      assertion.accept(egg);
    } finally {
      if (previous == null) {
        deferredByType.remove(type);
      } else {
        deferredByType.put(type, previous);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<EntityType<?>, SpawnEggItem> deferredByType() throws ReflectiveOperationException {
    return (Map<EntityType<?>, SpawnEggItem>) staticField(DeferredSpawnEggItem.class, "TYPE_MAP");
  }

  private static Object staticField(Class<?> owner, String name) throws ReflectiveOperationException {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(null);
  }
}
