package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.JsonCodec;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching an instrument item with a particular instrument. */
public class InstrumentIngredient implements ICustomIngredient {
  private static final String KEY_INSTRUMENT = "instrument";
  public static final ResourceLocation ID = TConstruct.getResource("instrument");

  private static final JsonCodec<InstrumentIngredient> CODEC_BODY = new JsonCodec<>() {
    @Override
    public InstrumentIngredient deserialize(JsonElement element, DynamicOps<?> ops) {
      JsonObject json = element.getAsJsonObject();
      Item item = Loadables.ITEM.getIfPresent(json, "item");
      ResourceKey<Instrument> instrument = null;
      TagKey<Instrument> ignore = null;
      if (json.has(KEY_INSTRUMENT)) {
        instrument = ResourceKey.create(Registries.INSTRUMENT, JsonHelper.getResourceLocation(json, KEY_INSTRUMENT));
      } else if (json.has("ignore")) {
        ignore = TinkerLoadables.INSTRUMENT_TAGS.getIfPresent(json, "ignore");
      } else {
        throw new JsonSyntaxException("Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
      }
      return new InstrumentIngredient(item, instrument, ignore);
    }

    @Override
    public JsonElement serialize(InstrumentIngredient ingredient, DynamicOps<?> ops) {
      JsonObject json = new JsonObject();
      json.addProperty("item", Loadables.ITEM.getString(ingredient.item));
      if (ingredient.instrument != null) {
        json.addProperty(KEY_INSTRUMENT, ingredient.instrument.location().toString());
      }
      if (ingredient.ignore != null) {
        json.addProperty("ignore", ingredient.ignore.location().toString());
      }
      return json;
    }
  };
  public static final MapCodec<InstrumentIngredient> CODEC = MapCodec.assumeMapUnsafe(CODEC_BODY);
  public static final StreamCodec<RegistryFriendlyByteBuf,InstrumentIngredient> STREAM_CODEC = StreamCodec.of(InstrumentIngredient::write, InstrumentIngredient::read);
  public static final IngredientType<InstrumentIngredient> TYPE = new IngredientType<>(CODEC, STREAM_CODEC);

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;

  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    this.item = item;
    this.instrument = instrument;
    this.ignore = ignore;
  }

  /** Creates an ingredient matching the given instrument. */
  public static Ingredient of(Item item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item, instrument, null).toVanilla();
  }

  /** Creates an ingredient matching an instrument outside the given tag. */
  public static Ingredient of(Item item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item, null, ignore).toVanilla();
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    Holder<Instrument> holder = stack.get(DataComponents.INSTRUMENT);
    if (holder != null) {
      if (instrument != null) {
        return holder.is(instrument);
      }
      return ignore != null && !holder.is(ignore);
    }
    // A missing component is the vanilla default for an instrument item. It is
    // acceptable for an ignore ingredient, but cannot identify one specific instrument.
    return instrument == null;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public Stream<ItemStack> getItems() {
    if (instrument != null) {
      return BuiltInRegistries.INSTRUMENT.getHolder(instrument)
        .map(holder -> Stream.of(InstrumentItem.create(item, holder)))
        .orElseGet(Stream::empty);
    }
    return Stream.of(new ItemStack(item));
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  private static InstrumentIngredient read(RegistryFriendlyByteBuf buffer) {
    Item item = Loadables.ITEM.decode(buffer);
    boolean specific = buffer.readBoolean();
    ResourceLocation id = buffer.readResourceLocation();
    if (specific) {
      return new InstrumentIngredient(item, ResourceKey.create(Registries.INSTRUMENT, id), null);
    }
    return new InstrumentIngredient(item, null, TagKey.create(Registries.INSTRUMENT, id));
  }

  private static void write(RegistryFriendlyByteBuf buffer, InstrumentIngredient ingredient) {
    Loadables.ITEM.encode(buffer, ingredient.item);
    if (ingredient.instrument != null) {
      buffer.writeBoolean(true);
      buffer.writeResourceLocation(ingredient.instrument.location());
    } else {
      buffer.writeBoolean(false);
      buffer.writeResourceLocation(Objects.requireNonNull(ingredient.ignore).location());
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof InstrumentIngredient other
      && item == other.item && Objects.equals(instrument, other.instrument) && Objects.equals(ignore, other.ignore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, instrument, ignore);
  }
}
