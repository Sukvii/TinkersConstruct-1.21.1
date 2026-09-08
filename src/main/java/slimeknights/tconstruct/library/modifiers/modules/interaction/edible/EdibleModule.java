package slimeknights.tconstruct.library.modifiers.modules.interaction.edible;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.build.ModifierTraitModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.tools.stat.impl.IntegerToolStat;
import slimeknights.tconstruct.library.tools.stat.impl.PercentToolStat;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Module that makes a tool edible.
 * Use {@link #EDIBLE_TRAIT} as a module to add the common edible behavior to a modifier.
 */
public enum EdibleModule implements ModifierModule, GeneralInteractionModifierHook, UsingToolModifierHook, OnAttackedModifierHook {
  INSTANCE;

  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<EdibleModule>defaultHooks(ModifierHooks.GENERAL_INTERACT, ModifierHooks.TOOL_USING, ModifierHooks.ON_ATTACKED);
  public static final RecordLoadable<EdibleModule> LOADER = new SingletonLoader<>(INSTANCE);

  /** Predicate for valid tools using the stats. */
  private static final IJsonPredicate<Item> VALID_TOOLS = ItemPredicate.or(ItemPredicate.tag(TinkerTags.Items.INTERACTABLE_CHARGE), ItemPredicate.tag(TinkerTags.Items.ARMOR));
  /** Hunger restored upon eating. */
  public static final IntegerToolStat HUNGER = new IntegerToolStat(new ToolStatId(TConstruct.MOD_ID, "hunger"), 0xFFF0A8A4, 0, 0, 200, VALID_TOOLS);
  /** Saturation restored upon eating. */
  public static final FloatToolStat SATURATION = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "saturation"), 0xFFF0A8A4, 0, 0, 200, VALID_TOOLS);
  /** Time required to eat the tool. */
  public static final FloatToolStat EAT_DURATION = new FloatToolStat(new ToolStatId(TConstruct.MOD_ID, "eat_duration"), 0xFFF0A8A4, 16, 0, 100, VALID_TOOLS);
  /** Chance for edible armor to trigger when attacked. */
  public static final PercentToolStat COUNTER_CHANCE = new PercentToolStat(new ToolStatId(TConstruct.MOD_ID, "edible_counter_chance"), 0xFFF0A8A4, 0, 0, 1, VALID_TOOLS);

  /** Module for adding edible behavior to a modifier. */
  public static final ModifierModule EDIBLE_TRAIT = new ModifierTraitModule(TinkerModifiers.edible.getId(), 1, true);

  @Override
  public RecordLoadable<EdibleModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
    if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken() && player.canEat(false) && tool.getStats().getInt(HUNGER) > 0) {
      GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  @Override
  public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
    return UseAnim.EAT;
  }

  @Override
  public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
    return tool.getStats().getInt(EAT_DURATION);
  }

  /** Takes a bite of the tool. */
  private void eat(IToolStackView tool, Player player, EquipmentSlot eatenSlot) {
    StatsNBT stats = tool.getStats();
    int hunger = Math.round(ConditionalStatModifierHook.getModifiedStat(tool, player, HUNGER));
    if (hunger > 0) {
      float saturation = ConditionalStatModifierHook.getModifiedStat(tool, player, SATURATION);
      player.getFoodData().eat(hunger, saturation);

      Level world = player.level();
      world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
      world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);

      List<ItemStack> representativeItems = new ArrayList<>();
      for (ModifierEntry entry : tool.getModifiers()) {
        entry.getHook(ModifierHooks.EDIBLE_EFFECT).onToolEaten(tool, entry, player, eatenSlot, hunger, saturation, representativeItems);
      }
      if (!representativeItems.isEmpty()) {
        ModifierUtil.foodConsumer.onConsume(player, representativeItems, hunger, saturation);
      }
    }
  }

  @Override
  public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (modifier != activeModifier && !entity.level().isClientSide) {
      StatsNBT stats = tool.getStats();
      if (stats.getInt(HUNGER) > 0) {
        int useTime = useDuration - timeLeft;
        if (useTime == stats.getInt(EAT_DURATION) && entity instanceof Player player && player.canEat(false)) {
          eat(tool, player, Util.getSlotType(entity.getUsedItemHand()));
        }
      }
    }
  }

  @Override
  public void beforeReleaseUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (!entity.level().isClientSide && !tool.isBroken() && entity instanceof Player player && player.canEat(false)) {
      StatsNBT stats = tool.getStats();
      if (stats.getInt(HUNGER) > 0 && useDuration - timeLeft == stats.getInt(EAT_DURATION)) {
        eat(tool, player, Util.getSlotType(entity.getUsedItemHand()));
      }
    }
  }

  @Override
  public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
    if (!tool.isBroken() && tool.hasTag(TinkerTags.Items.ARMOR)) {
      StatsNBT stats = tool.getStats();
      if (stats.getInt(HUNGER) > 0) {
        LivingEntity entity = context.getEntity();
        if (context.getLevel().random.nextFloat() < ConditionalStatModifierHook.getModifiedStat(tool, entity, COUNTER_CHANCE) && entity instanceof Player player && player.canEat(true)) {
          eat(tool, player, slotType);
        }
      }
    }
  }
}
