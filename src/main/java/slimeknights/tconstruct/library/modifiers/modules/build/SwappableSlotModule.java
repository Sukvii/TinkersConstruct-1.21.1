package slimeknights.tconstruct.library.modifiers.modules.build;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DisplayNameModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.util.ModuleWithKey;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import javax.annotation.Nullable;
import java.util.List;

/** Extra slot modifier whose slot type is selected from persistent data. */
public record SwappableSlotModule(@Nullable ResourceLocation key, int slotCount, ModifierCondition<IToolContext> condition) implements VolatileDataModifierHook, DisplayNameModifierHook, ModifierRemovalHook, ModifierModule, ModuleWithKey, ConditionalModule<IToolContext> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<SwappableSlotModule>defaultHooks(ModifierHooks.VOLATILE_DATA, ModifierHooks.DISPLAY_NAME, ModifierHooks.REMOVE);
  public static final String FORMAT = TConstruct.makeTranslationKey("modifier", "extra_modifier.type_format");
  public static final RecordLoadable<SwappableSlotModule> LOADER = RecordLoadable.create(ModuleWithKey.FIELD, IntLoadable.ANY_SHORT.requiredField("slots", SwappableSlotModule::slotCount), ModifierCondition.CONTEXT_FIELD, SwappableSlotModule::new);
  public SwappableSlotModule(@Nullable ResourceLocation key, int slotCount) { this(key, slotCount, ModifierCondition.ANY_CONTEXT); }
  public SwappableSlotModule(int slotCount) { this(null, slotCount); }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, Component name, @Nullable RegistryAccess access) {
    String slotName = tool.getPersistentData().getString(getKey(entry.getModifier()));
    if (!slotName.isEmpty()) {
      SlotType type = SlotType.getIfPresent(slotName);
      if (type != null) return Component.translatable(FORMAT, name.plainCopy(), type.getDisplayName()).withStyle(style -> style.withColor(type.getColor()));
    }
    return name;
  }

  @Override public Integer getPriority() { return 50; }
  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
    if (condition.matches(context, modifier)) {
      SlotType type = SlotType.getIfPresent(context.getPersistentData().getString(getKey(modifier.getModifier())));
      if (type != null) volatileData.addSlots(type, slotCount);
    }
  }
  @Nullable @Override public Component onRemoved(IToolStackView tool, Modifier modifier) { tool.getPersistentData().remove(getKey(modifier)); return null; }
  @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }
  @Override public RecordLoadable<SwappableSlotModule> getLoader() { return LOADER; }

  /** Adds or removes additional slots for a selected slot type. */
  public record BonusSlot(@Nullable ResourceLocation key, SlotType match, SlotType bonus, int slotCount, ModifierCondition<IToolContext> condition) implements VolatileDataModifierHook, ModifierModule, ModuleWithKey, ConditionalModule<IToolContext> {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = List.of(ModifierHooks.VOLATILE_DATA);
    public static final RecordLoadable<BonusSlot> LOADER = RecordLoadable.create(ModuleWithKey.FIELD, SlotType.LOADABLE.requiredField("match", BonusSlot::match), SlotType.LOADABLE.requiredField("bonus", BonusSlot::bonus), IntLoadable.ANY_SHORT.requiredField("slots", BonusSlot::slotCount), ModifierCondition.CONTEXT_FIELD, BonusSlot::new);
    public BonusSlot(@Nullable ResourceLocation key, SlotType match, SlotType bonus, int slotCount) { this(key, match, bonus, slotCount, ModifierCondition.ANY_CONTEXT); }
    public BonusSlot(SlotType match, SlotType penalty, int slotCount) { this(null, match, penalty, slotCount); }
    @Override public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
      if (condition.matches(context, modifier) && match.getName().equals(context.getPersistentData().getString(getKey(modifier.getModifier())))) volatileData.addSlots(bonus, slotCount);
    }
    @Override public List<ModuleHook<?>> getDefaultHooks() { return DEFAULT_HOOKS; }
    @Override public RecordLoadable<BonusSlot> getLoader() { return LOADER; }
  }
}
