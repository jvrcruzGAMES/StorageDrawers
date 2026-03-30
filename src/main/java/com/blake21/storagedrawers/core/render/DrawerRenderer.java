package com.blake21.storagedrawers.core.render;

import com.blake21.storagedrawers.core.component.DrawerComponent;
import com.blake21.storagedrawers.core.config.DebugConfig;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.model.config.Model.ModelReference;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DrawerRenderer {
   public static void updateDrawerDisplay(World world, Ref<ChunkStore> drawerRef, DrawerComponent state) {
      world.execute(() -> {
         Store<EntityStore> entityStore = world.getEntityStore().getStore();
         Ref<EntityStore> displayRef = state.getDisplayEntity();
         ItemStack stack = state.getStoredItem();
         if (stack != null && state.getQuantity() > 0) {
            Ref<EntityStore> textRef = state.getTextDisplayEntity();
            if (textRef != null && textRef.isValid()) {
               entityStore.removeEntity(textRef, RemoveReason.REMOVE);
               state.setTextDisplayEntity((Ref)null);
            }

            if (displayRef != null && displayRef.isValid()) {
               updateExistingItemDisplay(world, drawerRef, entityStore, displayRef, state);
            } else {
               spawnNewItemDisplay(world, drawerRef, state);
            }

         } else {
            clearDisplay(world, state);
         }
      });
   }

   private static void spawnNewItemDisplay(World world, Ref<ChunkStore> drawerRef, DrawerComponent state) {
      Store<EntityStore> entityStore = world.getEntityStore().getStore();
      ItemStack stack = state.getStoredItem();
      if (stack != null && !stack.isEmpty()) {
         stack = stack.withQuantity(1);
         stack.setOverrideDroppedItemAnimation(true);
         Model model = resolveModel(stack);
         Vector3d pos = calculateDisplayPosition(world, drawerRef, 0.2D, 0.52D);
         Vector3f rot = calculateDisplayRotation(world, drawerRef);
         Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
         holder.addComponent(Nameplate.getComponentType(), new Nameplate(String.valueOf(state.getQuantity())));
         Item itemAsset = stack.getItem();
         float itemScale = itemAsset != null ? itemAsset.getScale() : 1.0F;
         float targetScale = 0.5F;
         float dynamicScale = targetScale / itemScale;
         dynamicScale = Math.max(0.25F, Math.min(dynamicScale, 0.7F));
         if (model != null) {
            ModelReference ref = new ModelReference(model.getModelAssetId(), model.getScale(), model.getRandomAttachmentIds(), true);
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(ref));
         } else {
            DebugConfig.debug("ResolveModel returned null for " + String.valueOf(stack) + ", using ItemComponent fallback.");
            ItemComponent itemComp = new ItemComponent(stack);
            itemComp.setPickupDelay(Float.MAX_VALUE);
            itemComp.setRemovedByPlayerPickup(false);
            holder.addComponent(ItemComponent.getComponentType(), itemComp);
         }

         holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(dynamicScale));
         holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rot));
         holder.ensureComponent(Intangible.getComponentType());
         holder.ensureComponent(UUIDComponent.getComponentType());
         holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
         holder.ensureComponent(Frozen.getComponentType());
         holder.ensureComponent(PreventPickup.getComponentType());
         holder.ensureComponent(PreventItemMerging.getComponentType());
         Ref<EntityStore> newRef = entityStore.addEntity(holder, AddReason.SPAWN);
         state.setDisplayEntity(newRef);
      }
   }

   private static void updateExistingItemDisplay(World world, Ref<ChunkStore> drawerRef, Store<EntityStore> entityStore, Ref<EntityStore> displayRef, DrawerComponent state) {
      TransformComponent trans = (TransformComponent)entityStore.getComponent(displayRef, TransformComponent.getComponentType());
      if (trans != null) {
         trans.setPosition(calculateDisplayPosition(world, drawerRef, 0.2D, 0.52D));
         trans.setRotation(calculateDisplayRotation(world, drawerRef));
      }

      Nameplate nameplate = (Nameplate)entityStore.getComponent(displayRef, Nameplate.getComponentType());
      if (nameplate != null) {
         nameplate.setText(String.valueOf(state.getQuantity()));
      } else {
         entityStore.putComponent(displayRef, Nameplate.getComponentType(), new Nameplate(String.valueOf(state.getQuantity())));
      }

      entityStore.tryRemoveComponent(displayRef, Velocity.getComponentType());
      entityStore.tryRemoveComponent(displayRef, PhysicsValues.getComponentType());
      entityStore.tryRemoveComponent(displayRef, BoundingBox.getComponentType());
      entityStore.ensureComponent(displayRef, Frozen.getComponentType());
      entityStore.ensureComponent(displayRef, Intangible.getComponentType());
      entityStore.ensureComponent(displayRef, PreventPickup.getComponentType());
      entityStore.ensureComponent(displayRef, PreventItemMerging.getComponentType());
      ItemStack stack = state.getStoredItem();
      if (stack != null && !stack.isEmpty()) {
         Model model = resolveModel(stack);
         Item itemAsset = stack.getItem();
         float itemScale = itemAsset != null ? itemAsset.getScale() : 1.0F;
         float targetScale = 0.5F;
         float dynamicScale = targetScale / itemScale;
         dynamicScale = Math.max(0.25F, Math.min(dynamicScale, 0.7F));
         if (model != null) {
            ModelReference ref = new ModelReference(model.getModelAssetId(), model.getScale(), model.getRandomAttachmentIds(), true);
            entityStore.tryRemoveComponent(displayRef, ItemComponent.getComponentType());
            entityStore.tryRemoveComponent(displayRef, ModelComponent.getComponentType());
            entityStore.putComponent(displayRef, PersistentModel.getComponentType(), new PersistentModel(ref));
         } else {
            entityStore.tryRemoveComponent(displayRef, PersistentModel.getComponentType());
            entityStore.tryRemoveComponent(displayRef, ModelComponent.getComponentType());
            ItemComponent itemComp = (ItemComponent)entityStore.getComponent(displayRef, ItemComponent.getComponentType());
            if (itemComp != null) {
               if (!itemComp.getItemStack().getItemId().equals(stack.getItemId())) {
                  itemComp.setItemStack(stack.withQuantity(1));
               }
            } else {
               itemComp = new ItemComponent(stack.withQuantity(1));
               entityStore.putComponent(displayRef, ItemComponent.getComponentType(), itemComp);
            }

            itemComp.setPickupDelay(Float.MAX_VALUE);
            itemComp.setRemovedByPlayerPickup(false);
            itemComp.getItemStack().setOverrideDroppedItemAnimation(true);
         }

         EntityScaleComponent scaleComp = (EntityScaleComponent)entityStore.getComponent(displayRef, EntityScaleComponent.getComponentType());
         if (scaleComp != null) {
            scaleComp.setScale(dynamicScale);
         } else {
            entityStore.putComponent(displayRef, EntityScaleComponent.getComponentType(), new EntityScaleComponent(dynamicScale));
         }

      }
   }

   public static void clearDisplay(World world, DrawerComponent state) {
      Ref<EntityStore> displayRef = state.getDisplayEntity();
      Ref<EntityStore> textRef = state.getTextDisplayEntity();
      state.setDisplayEntity((Ref)null);
      state.setTextDisplayEntity((Ref)null);
      world.execute(() -> {
         Store<EntityStore> store = world.getEntityStore().getStore();
         if (displayRef != null && displayRef.isValid()) {
            store.removeEntity(displayRef, RemoveReason.REMOVE);
         }

         if (textRef != null && textRef.isValid()) {
            store.removeEntity(textRef, RemoveReason.REMOVE);
         }

      });
   }

   private static Model resolveModel(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         DebugConfig.debug("Resolving model for stack: " + stack.toString());
         Item itemAsset = stack.getItem();
         if (itemAsset == null) {
            DebugConfig.debug("Item asset is null for " + stack.getItemId());
            return null;
         } else {
            String modelId = null;
            if (itemAsset.hasBlockType()) {
               BlockType blockType = (BlockType)BlockType.getAssetMap().getAsset(stack.getBlockKey());
               if (blockType != null) {
                  modelId = blockType.getCustomModel();
                  String var10000 = blockType.getId();
                  DebugConfig.debug("BlockType found: " + var10000 + ", CustomModel: " + modelId);
               }
            }

            if (modelId == null) {
               modelId = itemAsset.getModel();
               DebugConfig.debug("Item model id: " + modelId);
            }

            if (modelId != null) {
               ModelAsset asset = (ModelAsset)ModelAsset.getAssetMap().getAsset(modelId);
               if (asset != null) {
                  DebugConfig.debug("Found asset via Exact Match: " + modelId);
               }

               String stripped;
               if (asset == null) {
                  stripped = modelId.replace(".blockymodel", "").replace(".hymodel", "");
                  if (!stripped.equals(modelId)) {
                     asset = (ModelAsset)ModelAsset.getAssetMap().getAsset(stripped);
                     if (asset != null) {
                        DebugConfig.debug("Found asset via Stripped: " + stripped);
                     }
                  }
               }

               String prefixed;
               if (asset == null) {
                  stripped = modelId.replace(".blockymodel", "").replace(".hymodel", "");
                  prefixed = stripped.toLowerCase();
                  if (!prefixed.equals(stripped)) {
                     asset = (ModelAsset)ModelAsset.getAssetMap().getAsset(prefixed);
                     if (asset != null) {
                        DebugConfig.debug("Found asset via Lowercase: " + prefixed);
                     }
                  }
               }

               if (asset == null) {
                  stripped = modelId.replace(".blockymodel", "").replace(".hymodel", "");
                  prefixed = "vanilla/" + stripped;
                  asset = (ModelAsset)ModelAsset.getAssetMap().getAsset(prefixed);
                  if (asset != null) {
                     DebugConfig.debug("Found asset via Prefix: " + prefixed);
                  }
               }

               if (asset != null) {
                  float targetSize = 0.25F;
                  float maxDim = 1.0F;
                  Box box = asset.getBoundingBox();
                  if (box != null) {
                     double w = box.width();
                     double h = box.height();
                     double d = box.depth();
                     maxDim = (float)Math.max(w, Math.max(h, d));
                  }

                  if (maxDim <= 0.01F) {
                     maxDim = 1.0F;
                  }

                  float dynamicScale = targetSize / maxDim;
                  DebugConfig.debug("Dynamic Scale: " + dynamicScale + " (dim=" + maxDim + ") for " + modelId);
                  return Model.createScaledModel(asset, dynamicScale);
               }

               DebugConfig.debug("ModelAsset not found for id (after all attempts): " + modelId);
            } else {
               DebugConfig.debug("No modelId found for " + stack.getItemId());
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static Vector3f calculateDisplayRotation(World world, Ref<ChunkStore> drawerRef) {
      Store<ChunkStore> store = world.getChunkStore().getStore();
      BlockStateInfo blockInfo = (BlockStateInfo)store.getComponent(drawerRef, BlockStateInfo.getComponentType());
      if (blockInfo == null) {
         return Vector3f.ZERO;
      } else {
         WorldChunk chunk = (WorldChunk)store.getComponent(blockInfo.getChunkRef(), WorldChunk.getComponentType());
         if (chunk == null) {
            return Vector3f.ZERO;
         } else {
            int blockIndex = blockInfo.getIndex();
            int x = ChunkUtil.xFromBlockInColumn(blockIndex);
            int y = ChunkUtil.yFromBlockInColumn(blockIndex);
            int z = ChunkUtil.zFromBlockInColumn(blockIndex);
            RotationTuple tuple = chunk.getRotation(x, y, z);
            return new Vector3f(0.0F, (float)tuple.yaw().getDegrees(), 0.0F);
         }
      }
   }

   private static Vector3d calculateDisplayPosition(World world, Ref<ChunkStore> drawerRef, double yTarget, double forwardOffset) {
      Store<ChunkStore> store = world.getChunkStore().getStore();
      BlockStateInfo blockInfo = (BlockStateInfo)store.getComponent(drawerRef, BlockStateInfo.getComponentType());
      if (blockInfo == null) {
         return new Vector3d(0.0D, 0.0D, 0.0D);
      } else {
         WorldChunk chunk = (WorldChunk)store.getComponent(blockInfo.getChunkRef(), WorldChunk.getComponentType());
         if (chunk == null) {
            return new Vector3d(0.0D, 0.0D, 0.0D);
         } else {
            int blockIndex = blockInfo.getIndex();
            int x = ChunkUtil.xFromBlockInColumn(blockIndex);
            int y = ChunkUtil.yFromBlockInColumn(blockIndex);
            int z = ChunkUtil.zFromBlockInColumn(blockIndex);
            double absX = (double)(chunk.getX() * 32 + x) + 0.5D;
            double absY = (double)y + yTarget;
            double absZ = (double)(chunk.getZ() * 32 + z) + 0.5D;
            Vector3d pos = new Vector3d(absX, absY, absZ);
            RotationTuple tuple = chunk.getRotation(x, y, z);
            Rotation yaw = tuple.yaw();
            switch(yaw) {
            case None:
               pos.add(0.0D, 0.0D, forwardOffset);
               break;
            case Ninety:
               pos.add(forwardOffset, 0.0D, 0.0D);
               break;
            case OneEighty:
               pos.add(0.0D, 0.0D, -forwardOffset);
               break;
            case TwoSeventy:
               pos.add(-forwardOffset, 0.0D, 0.0D);
            }

            return pos;
         }
      }
   }
}
