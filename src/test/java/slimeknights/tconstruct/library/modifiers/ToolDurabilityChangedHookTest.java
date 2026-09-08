package slimeknights.tconstruct.library.modifiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDurabilityChangedHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ToolItemTest;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regression coverage for durability hook timing and amount semantics. */
class ToolDurabilityChangedHookTest extends ToolItemTest {
  private static final ModifierId HOOK_MODIFIER_ID = new ModifierId("test", "durability_hook");
  private static final List<String> events = new ArrayList<>();
  private static final List<Integer> beforeAmounts = new ArrayList<>();
  @Nullable
  private static ItemStack observedStack;
  @Nullable
  private static LivingEntity observedHolder;

  private static final Modifier HOOK_MODIFIER = new Modifier() {
    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
      builder.addHook(new ToolDamageModifierHook() {
        @Override
        @Deprecated
        public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
          return amount;
        }

        @Override
        public int beforeDamageTool(IToolStackView tool, ModifierEntry modifier, int amount,
                                    @Nullable LivingEntity holder, @Nullable ItemStack stack, ModifierId cause) {
          beforeAmounts.add(amount);
          return amount - 2;
        }
      }, ModifierHooks.TOOL_DAMAGE);
      builder.addHook(observer("damage:first"), ModifierHooks.DURABILITY_CHANGED);
      builder.addHook(observer("damage:second"), ModifierHooks.DURABILITY_CHANGED);
      builder.addHook(new ToolDurabilityChangedHook() {
        @Override
        public void afterRepairTool(IToolStackView tool, ModifierEntry modifier, int amount) {
          events.add("repair:" + amount);
        }
      }, ModifierHooks.DURABILITY_CHANGED);
    }
  };

  private static ToolDurabilityChangedHook observer(String name) {
    return new ToolDurabilityChangedHook() {
      @Override
      public void afterDamageTool(IToolStackView tool, ModifierEntry modifier, int amount,
                                  @Nullable LivingEntity holder, ItemStack stack) {
        events.add(name + ":" + amount);
        observedStack = stack;
        observedHolder = holder;
      }
    };
  }

  @BeforeAll
  static void registerHookModifier() {
    ModifierFixture.init();
    HOOK_MODIFIER.setId(HOOK_MODIFIER_ID);
    ModifierManager.INSTANCE.staticModifiers.put(HOOK_MODIFIER_ID, HOOK_MODIFIER);
  }

  @BeforeEach
  void clearHookEvents() {
    events.clear();
    beforeAmounts.clear();
    observedStack = null;
    observedHolder = null;
  }

  @Test
  void damage_usesBeforeHookAndReportsClampedAmountOnce() {
    ToolStack tool = ToolStack.from(testItemStack);
    tool.addModifier(HOOK_MODIFIER_ID, 1);
    int durability = tool.getStats().getInt(ToolStats.DURABILITY);
    ModifierEntry entry = tool.getModifierList().get(0);

    assertThat(entry.getHook(ModifierHooks.TOOL_DAMAGE)
      .beforeDamageTool(tool, entry, 10, null, testItemStack, new ModifierId("test", "cause"))).isEqualTo(8);
    assertThat(beforeAmounts).containsExactly(10);

    beforeAmounts.clear();
    tool.setDamage(durability - 1);

    assertThat(ToolDamageUtil.directDamage(tool, 10, null, testItemStack)).isTrue();
    assertThat(tool.getDamage()).isEqualTo(durability);
    assertThat(beforeAmounts).isEmpty();
    assertThat(events).containsExactly("damage:first:1", "damage:second:1");
    assertThat(observedStack).isSameAs(testItemStack);
    assertThat(observedHolder).isNull();
  }

  @Test
  void repair_reportsOnlyActualRepairAndDoesNotRunAtBounds() {
    ToolStack tool = ToolStack.from(testItemStack);
    tool.addModifier(HOOK_MODIFIER_ID, 1);
    tool.setDamage(5);

    ToolDamageUtil.repair(tool, 50);
    ToolDamageUtil.repair(tool, 1);
    ToolDamageUtil.repair(tool, -1);

    assertThat(tool.getDamage()).isZero();
    assertThat(events).containsExactly("repair:5");
    assertThat(beforeAmounts).isEmpty();
  }

  @Test
  void creativePlayerDoesNotPayDurabilityCost() {
    ToolStack tool = ToolStack.from(testItemStack);
    tool.addModifier(HOOK_MODIFIER_ID, 1);
    Player creative = mock(Player.class);
    when(creative.isCreative()).thenReturn(true);

    assertThat(ModifierUtil.consumesResources(creative)).isFalse();
    assertThat(ToolDamageUtil.directDamage(tool, 10, creative, testItemStack)).isFalse();
    assertThat(tool.getDamage()).isZero();
    assertThat(events).isEmpty();
  }
}
