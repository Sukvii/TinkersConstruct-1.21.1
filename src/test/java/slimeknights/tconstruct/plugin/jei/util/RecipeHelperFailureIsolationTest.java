package slimeknights.tconstruct.plugin.jei.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.test.BaseMcTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeHelperFailureIsolationTest extends BaseMcTest {
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void aBrokenMultiRecipeIsLoggedAndSkippedWithoutHidingNormalRecipes() {
    RegistryAccess access = mock(RegistryAccess.class);
    Recipe<?> normal = mock(Recipe.class);
    Recipe<?> broken = mock(Recipe.class, Mockito.withSettings().extraInterfaces(IMultiRecipe.class));
    IllegalStateException failure = new IllegalStateException("intentional extension failure");
    when(((IMultiRecipe)broken).getRecipes(access)).thenThrow(failure);
    when(broken.toString()).thenReturn("failing JEI extension fixture");

    org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger)LogManager.getLogger("Mantle");
    Level previousLevel = logger.getLevel();
    CapturingAppender appender = new CapturingAppender();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.ERROR);
    List<Recipe> result;
    try {
      result = RecipeHelper.getJEIRecipes(access, Stream.of(normal, broken), Recipe.class);
    } finally {
      logger.removeAppender(appender);
      logger.setLevel(previousLevel);
      appender.stop();
    }

    assertThat(result).containsExactly(normal);
    assertThat(appender.events).hasSize(1);
    LogEvent event = appender.events.getFirst();
    assertThat(event.getMessage().getFormattedMessage()).contains("failing JEI extension fixture");
    assertThat(event.getThrown()).isSameAs(failure);
  }

  private static class CapturingAppender extends AbstractAppender {
    private final List<LogEvent> events = new ArrayList<>();

    private CapturingAppender() {
      super("RecipeHelperFailureIsolationTest", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      events.add(event.toImmutable());
    }
  }
}
