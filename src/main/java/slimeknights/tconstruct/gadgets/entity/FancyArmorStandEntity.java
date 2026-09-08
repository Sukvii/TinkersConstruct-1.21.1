package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import slimeknights.tconstruct.gadgets.TinkerGadgets;

import java.util.Locale;

/** Extension of {@link ArmorStand} with extra visual variants. */
public class FancyArmorStandEntity extends ArmorStand {
  private static final String TAG_VARIANT = "Variant";
  private static final String TAG_LEFT = "LeftHanded";
  private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(FancyArmorStandEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> LEFT_HANDED = SynchedEntityData.defineId(FancyArmorStandEntity.class, EntityDataSerializers.BOOLEAN);

  public FancyArmorStandEntity(EntityType<? extends FancyArmorStandEntity> type, Level level) {
    super(type, level);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(VARIANT, 0);
    builder.define(LEFT_HANDED, false);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    compound.putInt(TAG_VARIANT, getStandId());
    compound.putBoolean(TAG_LEFT, isLeft());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    entityData.set(VARIANT, compound.getInt(TAG_VARIANT));
    entityData.set(LEFT_HANDED, compound.getBoolean(TAG_LEFT));
  }

  @Override
  public boolean fireImmune() {
    return true;
  }

  /** Checks if this clear stand should be hidden while it has equipment. */
  public boolean isClearHidden() {
    if (getStandType() == StandType.CLEAR) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
        if (!getItemBySlot(slot).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isInvisibleTo(Player player) {
    return isClearHidden() || super.isInvisibleTo(player);
  }

  private boolean isLeft() {
    return entityData.get(LEFT_HANDED);
  }

  /** Sets whether the stand uses the player's left hand as its main hand. */
  public void setLeft(boolean left) {
    entityData.set(LEFT_HANDED, left);
  }

  @Override
  public HumanoidArm getMainArm() {
    return isLeft() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
  }

  protected int getStandId() {
    return entityData.get(VARIANT);
  }

  public StandType getStandType() {
    return StandType.byId(getStandId());
  }

  public void setStandType(StandType type) {
    entityData.set(VARIANT, type.getId());
  }

  private void setSmallVariant() {
    entityData.set(DATA_CLIENT_FLAGS, (byte)(entityData.get(DATA_CLIENT_FLAGS) | CLIENT_FLAG_SMALL));
  }

  /** Gets the item representing this stand variant. */
  public Item getStandItem() {
    return TinkerGadgets.armorStand.get(getStandType());
  }

  @Override
  public ItemStack getPickResult() {
    return new ItemStack(getStandItem());
  }

  @Override
  protected Component getTypeName() {
    return Component.translatable(getStandItem().getDescriptionId());
  }

  @Override
  protected void brokenByPlayer(ServerLevel level, DamageSource source) {
    ItemStack stack = new ItemStack(getStandItem());
    Component customName = getCustomName();
    if (customName != null) {
      stack.set(DataComponents.CUSTOM_NAME, customName);
    }
    Block.popResource(level, blockPosition(), stack);
    brokenByAnything(level, source);
  }

  /** All available stand variants. */
  public enum StandType {
    /** Small bamboo stand. */
    BAMBOO {
      @Override
      public void onPlace(FancyArmorStandEntity entity) {
        super.onPlace(entity);
        entity.setSmallVariant();
      }
    },
    /** Bone stand with visible arms. */
    BONE {
      @Override
      public void onPlace(FancyArmorStandEntity entity) {
        super.onPlace(entity);
        entity.setShowArms(true);
      }
    },
    /** Glowing bone stand with visible arms. */
    NECROTIC_BONE {
      @Override
      public void onPlace(FancyArmorStandEntity entity) {
        super.onPlace(entity);
        entity.setShowArms(true);
      }
    },
    /** Hidden while wearing equipment. */
    CLEAR;

    private static final StandType[] VALUES = values();
    private final int id = ordinal();

    public int getId() {
      return id;
    }

    public void onPlace(FancyArmorStandEntity entity) {
      entity.setStandType(this);
    }

    public boolean isFullbright() {
      return this == NECROTIC_BONE;
    }

    public static StandType byId(int id) {
      if (id < 0 || id >= VALUES.length) {
        id = 0;
      }
      return VALUES[id];
    }

    public String getSerializedName() {
      return name().toLowerCase(Locale.US);
    }
  }
}
