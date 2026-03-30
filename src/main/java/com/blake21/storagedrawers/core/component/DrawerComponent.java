package com.blake21.storagedrawers.core.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;

public class DrawerComponent implements Component<ChunkStore> {
   public static final BuilderCodec<DrawerComponent> CODEC;
   private static ComponentType<ChunkStore, DrawerComponent> COMPONENT_TYPE;
   @Nullable
   private String itemId;
   private int quantity;
   private transient Ref<EntityStore> displayEntity;
   private transient Ref<EntityStore> textDisplayEntity;
   private transient String blockTypeId;

   public static ComponentType<ChunkStore, DrawerComponent> getComponentType() {
      return COMPONENT_TYPE;
   }

   public static void setComponentType(ComponentType<ChunkStore, DrawerComponent> type) {
      COMPONENT_TYPE = type;
   }

   public DrawerComponent() {
   }

   public DrawerComponent(String itemId, int quantity) {
      this.itemId = itemId;
      this.quantity = quantity;
   }

   @Nullable
   public String getItemId() {
      return this.itemId;
   }

   public void setItemId(@Nullable String itemId) {
      this.itemId = itemId;
   }

   public int getQuantity() {
      return this.quantity;
   }

   public void setQuantity(int quantity) {
      this.quantity = quantity;
   }

   @Nullable
   public ItemStack getStoredItem() {
      return this.itemId != null && !this.itemId.isEmpty() ? new ItemStack(this.itemId, this.quantity) : null;
   }

   public void setStoredItem(@Nullable ItemStack storedItem) {
      if (storedItem != null && !storedItem.isEmpty()) {
         this.itemId = storedItem.getItemId();
      } else {
         this.itemId = null;
      }

   }

   public int getCount() {
      return this.quantity;
   }

   public void setCount(int count) {
      this.quantity = count;
   }

   public Ref<EntityStore> getDisplayEntity() {
      return this.displayEntity;
   }

   public void setDisplayEntity(Ref<EntityStore> displayEntity) {
      this.displayEntity = displayEntity;
   }

   public Ref<EntityStore> getTextDisplayEntity() {
      return this.textDisplayEntity;
   }

   public void setTextDisplayEntity(Ref<EntityStore> textDisplayEntity) {
      this.textDisplayEntity = textDisplayEntity;
   }

   @Nullable
   public String getBlockTypeId() {
      return this.blockTypeId;
   }

   public void setBlockTypeId(@Nullable String blockTypeId) {
      this.blockTypeId = blockTypeId;
   }

   public Component<ChunkStore> clone() {
      return new DrawerComponent(this.itemId, this.quantity);
   }

   static {
      CODEC = BuilderCodec.builder(DrawerComponent.class, DrawerComponent::new)
         .append(new KeyedCodec<>("ItemId", Codec.STRING), DrawerComponent::setItemId, DrawerComponent::getItemId)
         .add()
         .append(new KeyedCodec<>("Count", Codec.INTEGER), DrawerComponent::setQuantity, DrawerComponent::getQuantity)
         .add()
         .build();
   }
}
