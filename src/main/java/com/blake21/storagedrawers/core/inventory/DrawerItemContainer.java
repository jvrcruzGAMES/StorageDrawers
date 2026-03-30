package com.blake21.storagedrawers.core.inventory;

import com.blake21.storagedrawers.core.component.DrawerComponent;
import com.blake21.storagedrawers.core.render.DrawerRenderer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ClearTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class DrawerItemContainer extends SimpleItemContainer {
   private final DrawerComponent drawerComponent;
   private final World world;
   private final Ref<ChunkStore> blockRef;
   private static final short VIRTUAL_CAPACITY = 1;
   private static final int MAX_DRAWER_CAPACITY = 2400;

   public DrawerItemContainer(World world, Ref<ChunkStore> blockRef, DrawerComponent drawerComponent) {
      super((short)1);
      this.world = world;
      this.blockRef = blockRef;
      this.drawerComponent = drawerComponent;
   }

   public short getCapacity() {
      return 1;
   }

   protected ItemStack internal_getSlot(short slot) {
      return slot != 0 ? null : this.drawerComponent.getStoredItem();
   }

   protected ItemStack internal_setSlot(short slot, ItemStack itemStack) {
      if (slot != 0) {
         return null;
      } else {
         ItemStack old = this.internal_getSlot((short)0);
         if (ItemStack.isEmpty(itemStack)) {
            this.drawerComponent.setItemId((String)null);
            this.drawerComponent.setQuantity(0);
         } else if (!ItemStack.isSameItemType(old, itemStack)) {
            this.drawerComponent.setItemId(itemStack.getItemId());
            this.drawerComponent.setQuantity(Math.min(itemStack.getQuantity(), 2400));
         } else {
            this.drawerComponent.setQuantity(Math.min(itemStack.getQuantity(), 2400));
         }

         this.updateDisplay();
         return old;
      }
   }

   protected ItemStack internal_removeSlot(short slot) {
      if (slot != 0) {
         return null;
      } else {
         ItemStack old = this.internal_getSlot((short)0);
         this.drawerComponent.setItemId((String)null);
         this.drawerComponent.setQuantity(0);
         this.updateDisplay();
         return old;
      }
   }

   @Nonnull
   public ItemStackTransaction addItemStack(@Nonnull ItemStack itemStack) {
      if (ItemStack.isEmpty(itemStack)) {
         return new ItemStackTransaction(false, ActionType.ADD, itemStack, itemStack, false, false, Collections.emptyList());
      } else {
         String currentId = this.drawerComponent.getItemId();
         if (currentId != null && !currentId.equals(itemStack.getItemId())) {
            return new ItemStackTransaction(false, ActionType.ADD, itemStack, itemStack, false, false, Collections.emptyList());
         } else {
            int currentCount = this.drawerComponent.getQuantity();
            int spaceRemaining = 2400 - currentCount;
            if (spaceRemaining <= 0) {
               return new ItemStackTransaction(false, ActionType.ADD, itemStack, itemStack, false, false, Collections.emptyList());
            } else {
               int toAdd = Math.min(itemStack.getQuantity(), spaceRemaining);
               if (currentId == null) {
                  this.drawerComponent.setItemId(itemStack.getItemId());
               }

               this.drawerComponent.setQuantity(currentCount + toAdd);
               this.updateDisplay();
               ItemStack remainder = itemStack.withQuantity(itemStack.getQuantity() - toAdd);
               return new ItemStackTransaction(true, ActionType.ADD, itemStack, remainder != null ? remainder : ItemStack.EMPTY, false, false, Collections.emptyList());
            }
         }
      }
   }

   @Nonnull
   public ItemStackTransaction removeItemStack(@Nonnull ItemStack itemStack) {
      if (ItemStack.isEmpty(itemStack)) {
         return new ItemStackTransaction(false, ActionType.REMOVE, itemStack, itemStack, false, false, Collections.emptyList());
      } else {
         String currentId = this.drawerComponent.getItemId();
         if (currentId != null && currentId.equals(itemStack.getItemId())) {
            int currentCount = this.drawerComponent.getQuantity();
            int toRemove = Math.min(itemStack.getQuantity(), currentCount);
            this.drawerComponent.setQuantity(currentCount - toRemove);
            if (this.drawerComponent.getQuantity() <= 0) {
               this.drawerComponent.setItemId((String)null);
               this.drawerComponent.setQuantity(0);
            }

            this.updateDisplay();
            ItemStack remainderRequest = itemStack.withQuantity(itemStack.getQuantity() - toRemove);
            boolean succeeded = toRemove > 0;
            return new ItemStackTransaction(succeeded, ActionType.REMOVE, itemStack, remainderRequest != null ? remainderRequest : ItemStack.EMPTY, false, false, Collections.emptyList());
         } else {
            return new ItemStackTransaction(false, ActionType.REMOVE, itemStack, itemStack, false, false, Collections.emptyList());
         }
      }
   }

   private void updateDisplay() {
      if (this.world != null && this.blockRef != null) {
         DrawerRenderer.updateDrawerDisplay(this.world, this.blockRef, this.drawerComponent);
      }

   }

   @Nonnull
   public List<ItemStack> dropAllItemStacks() {
      return Collections.emptyList();
   }

   protected ClearTransaction internal_clear() {
      return ClearTransaction.EMPTY;
   }
}
