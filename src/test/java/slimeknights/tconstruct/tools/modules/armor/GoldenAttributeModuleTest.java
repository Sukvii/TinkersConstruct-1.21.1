package slimeknights.tconstruct.tools.modules.armor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for {@link GoldenAttributeModule} health clamping.
 * <p>
 * In 1.21 {@link Attributes#MAX_HEALTH} is a {@code Holder<Attribute>}, so the module must compare holders instead of
 * raw {@link Attribute} instances. Otherwise the clamp never runs and dropping the gold bonus leaves the entity above
 * its new max health.
 */
class GoldenAttributeModuleTest extends BaseMcTest {
  /** Base max health used by the mocked entity, matching the player value the module is balanced around */
  private static final float BASE_HEALTH = 20;
  /** Amount the provider uses for gold guard: 4 flat plus 4 per level of gold */
  private static final float FLAT = 4;
  private static final float EACH_LEVEL = 4;

  /** Current health of the mocked entity */
  private float health;
  /** Attribute instance backing the mocked entity */
  private AttributeInstance instance;

  private static GoldenAttributeModule module(Attribute attribute) {
    return GoldenAttributeModule.builder(attribute, Operation.ADD_VALUE).amount(FLAT, EACH_LEVEL);
  }

  /** Creates an entity mock backed by a real attribute instance and mutable health */
  private LivingEntity mockEntity(Attribute attribute) {
    LivingEntity living = mock(LivingEntity.class);
    instance = new AttributeInstance(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), ignored -> {});
    when(living.getAttribute(any())).thenReturn(instance);
    when(living.getItemBySlot(any())).thenReturn(ItemStack.EMPTY);
    when(living.getHealth()).thenAnswer(invocation -> health);
    doAnswer(invocation -> {
      health = invocation.getArgument(0);
      return null;
    }).when(living).setHealth(anyFloat());
    return living;
  }

  /** Applies the modifier the module would apply for the given amount of gold */
  private void applyModifier(GoldenAttributeModule module, int totalGold) {
    instance.addTransientModifier(new AttributeModifier(module.modifierId(), module.amount().compute(totalGold), Operation.ADD_VALUE));
  }

  private static EquipmentChangeContext context(LivingEntity living) {
    return new EquipmentChangeContext(living, EquipmentSlot.HEAD, ItemStack.EMPTY, ItemStack.EMPTY);
  }

  @Test
  void unequipClampsHealthToNewMax() {
    Attribute attribute = Attributes.MAX_HEALTH.value();
    GoldenAttributeModule module = module(attribute);
    LivingEntity living = mockEntity(attribute);
    instance.setBaseValue(BASE_HEALTH);
    applyModifier(module, 1);
    health = BASE_HEALTH + FLAT + EACH_LEVEL;
    assertThat(instance.getValue()).isEqualTo(health);

    // losing the modifier drops max health back to the base value, so the current health must follow
    module.onUnequip(mock(IToolStackView.class), mock(ModifierEntry.class), context(living));

    assertThat(instance.getValue()).isEqualTo(BASE_HEALTH);
    assertThat(health).isEqualTo(BASE_HEALTH);
    verify(living).setHealth(BASE_HEALTH);
  }

  @Test
  void losingGoldClampsHealthToNewMax() {
    Attribute attribute = Attributes.MAX_HEALTH.value();
    GoldenAttributeModule module = module(attribute);
    LivingEntity living = mockEntity(attribute);
    instance.setBaseValue(BASE_HEALTH);
    applyModifier(module, 1);
    health = BASE_HEALTH + FLAT + EACH_LEVEL;
    // one armor slot currently counts as gold, and the changed slot stops counting below
    GoldenAttributeModule.TotalGold gold = new GoldenAttributeModule.TotalGold();
    gold.setGold(EquipmentSlot.HEAD, true);
    TinkerDataCapability.getData(living).put(module.dataKey(), gold);

    module.onEquipmentChange(mock(IToolStackView.class), mock(ModifierEntry.class), context(living), EquipmentSlot.CHEST);

    // the bonus drops to the flat amount, so the max health drops by one level and clamps the current health
    assertThat(gold.getTotalGold()).isZero();
    assertThat(instance.getValue()).isEqualTo(BASE_HEALTH + FLAT);
    assertThat(health).isEqualTo(BASE_HEALTH + FLAT);
    verify(living).setHealth(BASE_HEALTH + FLAT);
  }

  @Test
  void nonHealthAttributeLeavesHealthUntouched() {
    Attribute attribute = Attributes.ARMOR.value();
    GoldenAttributeModule module = module(attribute);
    LivingEntity living = mockEntity(attribute);
    instance.setBaseValue(0);
    applyModifier(module, 1);
    health = 5;

    module.onUnequip(mock(IToolStackView.class), mock(ModifierEntry.class), context(living));

    assertThat(instance.getValue()).isZero();
    assertThat(health).isEqualTo(5);
    verify(living, never()).setHealth(anyFloat());
  }
}
