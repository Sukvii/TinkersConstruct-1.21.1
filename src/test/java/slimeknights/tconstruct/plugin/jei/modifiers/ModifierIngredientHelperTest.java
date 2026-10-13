package slimeknights.tconstruct.plugin.jei.modifiers;

import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierFixture;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ModifierIngredientHelperTest extends BaseMcTest {
  @BeforeAll
  static void registerTestModifiers() {
    ModifierFixture.init();
  }

  @Test
  void modifierUidUsesModifierIdentityAndPatternUidUsesPatternObject() {
    ModifierEntry entry = new ModifierEntry(ModifierFixture.TEST_MODIFIER_1, 1);
    Pattern pattern = mock(Pattern.class);

    assertThat(new ModifierIngredientHelper().getUid(entry, UidContext.Ingredient)).isEqualTo(entry.getId());
    assertThat(new PatternIngredientHelper().getUid(pattern, UidContext.Ingredient)).isSameAs(pattern);
  }

  @Test
  void modifierTagEquivalentComparesModifierValuesAndPreservesOrder() {
    Modifier first = ModifierFixture.TEST_MODIFIER_1;
    Modifier second = ModifierFixture.TEST_MODIFIER_2;
    TagKey<Modifier> tag = ModifierManager.getTag(new net.minecraft.resources.ResourceLocation("test", "ordered"));
    ModifierIngredientHelper helper = new ModifierIngredientHelper();

    try (MockedStatic<ModifierManager> manager = mockStatic(ModifierManager.class)) {
      manager.when(ModifierManager::getAllTags).thenReturn(Stream.of(Map.entry(tag, List.of(first, second))));
      assertThat(helper.getTagKeyEquivalent(List.of(
        new ModifierEntry(first, 1), new ModifierEntry(second, 1)))).contains(tag);

      manager.when(ModifierManager::getAllTags).thenReturn(Stream.of(Map.entry(tag, List.of(first, second))));
      assertThat(helper.getTagKeyEquivalent(List.of(
        new ModifierEntry(second, 1), new ModifierEntry(first, 1)))).isEmpty();
    }
  }
}
