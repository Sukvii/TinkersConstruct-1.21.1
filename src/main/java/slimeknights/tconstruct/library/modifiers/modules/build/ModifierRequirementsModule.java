package slimeknights.tconstruct.library.modifiers.modules.build;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.predicate.modifier.ModifierPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.HasModifierPredicate;
import slimeknights.tconstruct.library.json.predicate.tool.ToolContextPredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.RequirementsModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.util.LazyModifier;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;

import static slimeknights.tconstruct.library.modifiers.ModifierEntry.VALID_LEVEL;

/** Module that validates prerequisites for adding a modifier to a tool. */
public class ModifierRequirementsModule implements ValidateModifierHook, ModifierModule, RequirementsModifierHook {
  public static final RecordLoadable<ModifierRequirementsModule> LOADER = RecordLoadable.create(
    ToolContextPredicate.LOADER.requiredField("requirement", m -> m.requirement), VALID_LEVEL.defaultField("modifier_level", m -> m.level),
    StringLoadable.DEFAULT.requiredField("translation_key", m -> m.translationKey),
    ModifierEntry.LOADABLE.list(0).defaultField("display_modifiers", List.of(), m -> m.display), ModifierRequirementsModule::new);
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ModifierRequirementsModule>defaultHooks(ModifierHooks.VALIDATE_UPGRADE, ModifierHooks.REQUIREMENTS);

  private final IJsonPredicate<IToolContext> requirement;
  private final IntRange level;
  private final String translationKey;
  private final List<ModifierEntry> display;
  private final Component errorMessage;

  private ModifierRequirementsModule(IJsonPredicate<IToolContext> requirement, IntRange level, String translationKey, List<ModifierEntry> display) {
    this.requirement = requirement;
    this.level = level;
    this.translationKey = translationKey;
    this.display = display;
    this.errorMessage = Component.translatable(translationKey);
  }

  @Nullable @Override
  public Component validate(IToolStackView tool, ModifierEntry modifier) {
    return level.test(modifier.getLevel()) && !requirement.matches(tool) ? errorMessage : null;
  }

  @Nullable @Override
  public Component requirementsError(ModifierEntry entry) { return level.test(entry.getLevel()) ? errorMessage : null; }

  @Override
  public List<ModifierEntry> displayModifiers(ModifierEntry entry) { return level.test(entry.getLevel()) ? display : List.of(); }
  @Override public RecordLoadable<ModifierRequirementsModule> getLoader() { return LOADER; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }
  public static Builder builder() { return new Builder(); }

  @Setter @Accessors(fluent = true)
  public static class Builder {
    private final ImmutableList.Builder<IJsonPredicate<IToolContext>> requirements = ImmutableList.builder();
    private final ImmutableList.Builder<ModifierEntry> displayModifiers = ImmutableList.builder();
    private int minLevel = 1;
    private int maxLevel = VALID_LEVEL.max();
    private String translationKey;
    private Builder() {}
    public Builder modifierKey(ModifierId id) { translationKey = Util.makeTranslationKey("modifier", id) + ".requirements"; return this; }
    public Builder modifierKey(LazyModifier id) { return modifierKey(id.getId()); }
    @CanIgnoreReturnValue public Builder displayModifier(ModifierId id, int level) { displayModifiers.add(new ModifierEntry(id, level)); return this; }
    public Builder requirement(IJsonPredicate<IToolContext> requirement) { requirements.add(requirement); return this; }
    public Builder requireUpgrade(ModifierId id, int level) { displayModifier(id, level); requirements.add(HasModifierPredicate.hasUpgrade(id, level)); return this; }
    public Builder requireModifier(ModifierId id, int level) { displayModifier(id, level); requirements.add(HasModifierPredicate.hasModifier(id, level)); return this; }
    public Builder requireUpgrade(TagKey<Modifier> tag, int level) { requirements.add(HasModifierPredicate.hasUpgrade(ModifierPredicate.tag(tag), level)); return this; }
    public Builder requireModifier(TagKey<Modifier> tag, int level) { requirements.add(HasModifierPredicate.hasModifier(ModifierPredicate.tag(tag), level)); return this; }
    public ModifierRequirementsModule build() {
      if (translationKey == null) throw new IllegalStateException("Must set translation key");
      List<IJsonPredicate<IToolContext>> predicates = requirements.build();
      if (predicates.isEmpty()) throw new IllegalStateException("Must have at least one requirement");
      IntRange range = VALID_LEVEL.range(minLevel, maxLevel);
      List<ModifierEntry> display = displayModifiers.build();
      return new ModifierRequirementsModule(predicates.size() == 1 ? predicates.get(0) : ToolContextPredicate.LOADER.and(predicates), range, translationKey, display);
    }
  }
}
