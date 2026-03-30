package com.blake21.storagedrawers.core.system;

import com.blake21.storagedrawers.core.component.DrawerComponent;
import com.blake21.storagedrawers.core.config.DebugConfig;
import com.blake21.storagedrawers.core.inventory.DrawerItemContainer;
import com.blake21.storagedrawers.core.render.DrawerRenderer;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import javax.annotation.Nonnull;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

public class DrawerSystem extends RefSystem<ChunkStore> {
   private final ComponentType<ChunkStore, DrawerComponent> componentType;

   public DrawerSystem(ComponentType<ChunkStore, DrawerComponent> componentType) {
      this.componentType = componentType;
   }

   @Nonnull
   public Query<ChunkStore> getQuery() {
      return this.componentType;
   }

   public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
      BlockStateInfo blockInfo = (BlockStateInfo)commandBuffer.getComponent(ref, BlockStateInfo.getComponentType());
      if (blockInfo != null) {
         ItemContainerBlock containerBlock = (ItemContainerBlock)commandBuffer.getComponent(ref, ItemContainerBlock.getComponentType());
         DrawerComponent drawerComponent = (DrawerComponent)commandBuffer.getComponent(ref, this.componentType);
         if (containerBlock != null && drawerComponent != null) {
            World world = ((ChunkStore)store.getExternalData()).getWorld();
            WorldChunk chunk = (WorldChunk)store.getComponent(blockInfo.getChunkRef(), WorldChunk.getComponentType());
            if (chunk != null) {
               int blockIndex = blockInfo.getIndex();
               BlockType blockType = chunk.getBlockType(ChunkUtil.xFromBlockInColumn(blockIndex), ChunkUtil.yFromBlockInColumn(blockIndex), ChunkUtil.zFromBlockInColumn(blockIndex));
               if (blockType != null) {
                  Item item = blockType.getItem();
                  if (item != null) {
                     drawerComponent.setBlockTypeId(item.getId());
                     DebugConfig.debug("[DrawerSystem] Stored blockTypeId: " + item.getId());
                  } else {
                     drawerComponent.setBlockTypeId(blockType.getId());
                     DebugConfig.debug("[DrawerSystem] Stored blockTypeId (fallback): " + blockType.getId());
                  }
               }
            }

            Vector3d pos = this.getBlockPosition(store, ref);
            DebugConfig.debug("[DrawerSystem] onEntityAdded - Raw position: " + String.valueOf(pos));
            if (pos != null) {
               Vector3i blockPos = new Vector3i((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
               String var10000 = String.valueOf(blockPos);
               DebugConfig.debug("[DrawerSystem] onEntityAdded - Looking for pending data at: " + var10000);
               DrawerPlacementListener.DrawerData pendingData = DrawerPlacementListener.consumePendingData(blockPos);
               DebugConfig.debug("[DrawerSystem] onEntityAdded - Pending data found: " + (pendingData != null));
               if (pendingData != null) {
                  drawerComponent.setItemId(pendingData.itemId);
                  drawerComponent.setQuantity(pendingData.count);
                  var10000 = pendingData.itemId;
                  DebugConfig.debug("[DrawerSystem] Restored contents: " + var10000 + " x" + pendingData.count + " at " + String.valueOf(blockPos));
               }
            }

            DrawerItemContainer proxy = new DrawerItemContainer(world, ref, drawerComponent);
            containerBlock.setItemContainer(proxy);
            DrawerRenderer.updateDrawerDisplay(world, ref, drawerComponent);
         }

      }
   }

   public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
      DebugConfig.debug("[DrawerSystem] onEntityRemove called with reason: " + String.valueOf(reason));
      if (reason == RemoveReason.REMOVE) {
         DrawerComponent drawerComponent = (DrawerComponent)commandBuffer.getComponent(ref, this.componentType);
         DebugConfig.debug("[DrawerSystem] DrawerComponent is: " + (drawerComponent != null ? "present" : "null"));
         if (drawerComponent != null) {
            World world = ((ChunkStore)store.getExternalData()).getWorld();
            String var10000 = world != null ? world.getName() : "null";
            DebugConfig.debug("[DrawerSystem] World is: " + var10000);

            try {
               String blockId = drawerComponent.getBlockTypeId();
               DebugConfig.debug("[DrawerSystem] Using stored blockTypeId: " + blockId);
               BlockStateInfo blockInfo = (BlockStateInfo)store.getComponent(ref, BlockStateInfo.getComponentType());
               if (blockInfo != null) {
                  ItemContainerBlock containerBlock = (ItemContainerBlock)commandBuffer.getComponent(ref, ItemContainerBlock.getComponentType());
                  if (containerBlock != null) {
                     containerBlock.getItemContainer().clear();
                  }
               }

               if (blockId == null || blockId.isEmpty()) {
                  blockId = "StorageDrawers:storage_drawer";
                  DebugConfig.debug("[DrawerSystem] Using fallback ID: " + blockId);
               }

               ItemStack drawerItem = new ItemStack(blockId, 1);
               DebugConfig.debug("[DrawerSystem] Created ItemStack: " + blockId);
               if (drawerComponent.getItemId() != null && drawerComponent.getQuantity() > 0) {
                  BsonDocument drawerContents = new BsonDocument();
                  drawerContents.put("ItemId", new BsonString(drawerComponent.getItemId()));
                  drawerContents.put("Count", new BsonInt32(drawerComponent.getQuantity()));
                  drawerItem = drawerItem.withMetadata("DrawerContents", drawerContents);
                  var10000 = drawerComponent.getItemId();
                  DebugConfig.debug("[DrawerSystem] Saving drawer contents: " + var10000 + " x" + drawerComponent.getQuantity());
               }

               Vector3d dropPosition = this.getBlockPosition(store, ref);
               var10000 = dropPosition != null ? dropPosition.toString() : "null";
               DebugConfig.debug("[DrawerSystem] Drop position is: " + var10000);
               if (dropPosition != null) {
                  Store<EntityStore> estore = world.getEntityStore().getStore();
                  DebugConfig.debug("[DrawerSystem] Generating item drops...");
                  Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(estore, List.of(drawerItem), dropPosition, Vector3f.ZERO);
                  DebugConfig.debug("[DrawerSystem] Generated " + itemEntityHolders.length + " item holders");
                  if (itemEntityHolders.length > 0) {
                     world.execute(() -> {
                        estore.addEntities(itemEntityHolders, AddReason.SPAWN);
                     });
                     DebugConfig.debug("[DrawerSystem] Scheduled entity spawn");
                  }
               } else {
                  System.err.println("[DrawerSystem] Cannot drop - position is null!");
               }
            } catch (Exception var14) {
               System.err.println("[DrawerSystem] Error dropping item: " + var14.getMessage());
               var14.printStackTrace();
            }

            DrawerRenderer.clearDisplay(world, drawerComponent);
         }
      }

   }

   private Vector3d getBlockPosition(Store<ChunkStore> store, Ref<ChunkStore> ref) {
      BlockStateInfo blockInfo = (BlockStateInfo)store.getComponent(ref, BlockStateInfo.getComponentType());
      if (blockInfo == null) {
         return null;
      } else {
         WorldChunk chunk = (WorldChunk)store.getComponent(blockInfo.getChunkRef(), WorldChunk.getComponentType());
         if (chunk == null) {
            return null;
         } else {
            int blockIndex = blockInfo.getIndex();
            int x = ChunkUtil.xFromBlockInColumn(blockIndex);
            int y = ChunkUtil.yFromBlockInColumn(blockIndex);
            int z = ChunkUtil.zFromBlockInColumn(blockIndex);
            double absX = (double)(chunk.getX() * 32 + x) + 0.5D;
            double absY = (double)y + 0.5D;
            double absZ = (double)(chunk.getZ() * 32 + z) + 0.5D;
            return new Vector3d(absX, absY, absZ);
         }
      }
   }
}
