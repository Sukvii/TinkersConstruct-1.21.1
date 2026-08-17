package slimeknights.tconstruct.library.modifiers.modules.build;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;

import java.util.List;

/** Module for a modifier to add a nested modifier as a trait. */
public record ModifierTraitModule(ModifierEntry modifier, boolean fixedLevel, ModifierCondition<IToolContext> condition) implements ModifierTraitHook, ModifierModule, ConditionalModule<IToolContext> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ModifierTraitModule>defaultHooks(ModifierHooks.MODIFIER_TRAITS);
  public static final RecordLoadable<ModifierTraitModule> LOADER = RecordLoadable.create(
    ModifierEntry.LOADABLE.directField(ModifierTraitModule::modifier),
    BooleanLoadable.INSTANCE.requiredField("fixed_level", ModifierTraitModule::fixedLevel),
    ModifierCondition.CONTEXT_FIELD,
    ModifierTraitModule::new);

  @Deprecated(forRemoval = true)
  public ModifierTraitModule {}

  public ModifierTraitModule(ModifierEntry modifier, boolean fixedLevel, IJsonPredicate<IToolContext> tool) {
    this(modifier, fixedLevel, ModifierCondition.ANY_CONTEXT.with(tool));
  }

  @Deprecated(forRemoval = true)
  public ModifierTraitModule(ModifierId id, int level, boolean fixedLevel, ModifierCondition<IToolContext> condition) {
    this(new ModifierEntry(id, level), fixedLevel, condition);
  }

  public ModifierTraitModule(ModifierId id, int level, boolean fixedLevel, IJsonPredicate<IToolContext> tool) {
    this(new ModifierEntry(id, level), fixedLevel, tool);
  }

  public ModifierTraitModule(ModifierEntry modifier, boolean fixedLevel) {
    this(modifier, fixedLevel, ModifierCondition.ANY_CONTEXT);
  }

  public ModifierTraitModule(ModifierId id, int level, boolean fixedLevel) {
    this(id, level, fixedLevel, ModifierCondition.ANY_CONTEXT);
  }

  public static ModifierTraitModule tagCondition(ModifierId id, TagKey<Item> tag) {
    return new ModifierTraitModule(id, 1, false, ToolContextPredicate.tag(tag));
  }

  @Override
  public void addTraits(IToolContext context, ModifierEntry self, TraitBuilder builder, boolean firstEncounter) {
    if (condition.matches(context, self)) {
      if (fixedLevel) {
        if (firstEncounter) {
          builder.add(this.modifier);
        }
      } else if (self.getLevel() == 1) {
        builder.add(this.modifier);
      } else {
        builder.add(this.modifier.withLevel(this.modifier.getLevel() * self.getLevel()));
      }
    }
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public RecordLoadable<ModifierTraitModule> getLoader() {
    return LOADER;
  }
}
