package com.blake21.storagedrawers.core.interaction;

import com.blake21.storagedrawers.core.component.DrawerComponent;
import com.blake21.storagedrawers.core.config.DebugConfig;
import com.blake21.storagedrawers.core.render.DrawerRenderer;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockFace;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DrawerInteraction extends SimpleBlockInteraction {
   public static final BuilderCodec<DrawerInteraction> CODEC = BuilderCodec.builder(DrawerInteraction.class, DrawerInteraction::new).build();

   protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack heldItem, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
      if (interactionType == InteractionType.Primary || interactionType == InteractionType.Use || interactionType == InteractionType.Secondary) {
         Ref<EntityStore> entityRef = interactionContext.getEntity();
         Store<EntityStore> entityStore = entityRef.getStore();
         Player player = (Player)entityStore.getComponent(entityRef, Player.getComponentType());
         if (player != null) {
            Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
            if (blockRef != null) {
               DrawerComponent drawer = (DrawerComponent)blockRef.getStore().getComponent(blockRef, DrawerComponent.getComponentType());
               if (drawer != null) {
                  BlockFace hitFace = BlockFace.None;
                  if (interactionContext.getClientState() != null) {
                     hitFace = interactionContext.getClientState().blockFace;
                  }

                  BlockFace frontFace = this.getFrontFace(world, pos);
                  boolean isFrontFace = hitFace != BlockFace.None && hitFace == frontFace;
                  if (hitFace == BlockFace.None) {
                     isFrontFace = this.isHittingFrontFaceVector(world, player, pos, frontFace);
                  }

                  String var10000 = String.valueOf(interactionType);
                  DebugConfig.debug("[DrawerDebug] Interaction: " + var10000 + " HitFace: " + String.valueOf(hitFace) + " Front: " + String.valueOf(frontFace) + " IsFront: " + isFrontFace);
                  if (interactionType == InteractionType.Primary) {
                     if (isFrontFace) {
                        boolean sneaking = false;
                        MovementStatesComponent movement = (MovementStatesComponent)entityStore.getComponent(entityRef, MovementStatesComponent.getComponentType());
                        if (movement != null) {
                           sneaking = movement.getMovementStates().crouching;
                        }

                        int amount = sneaking ? 64 : 1;
                        this.handleWithdraw(drawer, player, amount);
                        DrawerRenderer.updateDrawerDisplay(world, blockRef, drawer);
                        interactionContext.getState().state = InteractionState.Finished;
                     } else {
                        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
                        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
                        if (chunkRef != null && chunkRef.isValid()) {
                           BlockHarvestUtils.performBlockDamage(player, entityRef, pos, heldItem, (ItemTool)null, (String)null, false, 1.0F, 2048, chunkRef, commandBuffer, chunkStore);
                        }

                        interactionContext.getState().state = InteractionState.Finished;
                     }
                  } else if (interactionType == InteractionType.Use || interactionType == InteractionType.Secondary) {
                     if (this.handleDeposit(drawer, player, heldItem)) {
                        DrawerRenderer.updateDrawerDisplay(world, blockRef, drawer);
                     }

                     interactionContext.getState().state = InteractionState.Finished;
                  }

               }
            }
         }
      }
   }

   private BlockFace getFrontFace(World world, Vector3i pos) {
      WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
      if (chunk == null) {
         return BlockFace.North;
      } else {
         int rotationIndex = chunk.getRotationIndex(pos.x, pos.y, pos.z);
         int yawIndex = rotationIndex & 3;
         switch(yawIndex) {
         case 0:
            return BlockFace.South;
         case 1:
            return BlockFace.East;
         case 2:
            return BlockFace.North;
         case 3:
            return BlockFace.West;
         default:
            return BlockFace.South;
         }
      }
   }

   private boolean isHittingFrontFaceVector(World world, Player player, Vector3i pos, BlockFace frontFace) {
      double frontX = 0.0D;
      double frontZ = 0.0D;
      switch(frontFace) {
      case North:
         frontZ = -1.0D;
         break;
      case South:
         frontZ = 1.0D;
         break;
      case West:
         frontX = -1.0D;
         break;
      case East:
         frontX = 1.0D;
         break;
      default:
         frontZ = -1.0D;
      }

      TransformComponent transform = (TransformComponent)player.getReference().getStore().getComponent(player.getReference(), TransformComponent.getComponentType());
      if (transform == null) {
         return false;
      } else {
         Vector3d playerPos = transform.getPosition();
         double centerX = (double)pos.x + 0.5D;
         double centerZ = (double)pos.z + 0.5D;
         double vecX = playerPos.x - centerX;
         double vecZ = playerPos.z - centerZ;
         double len = Math.sqrt(vecX * vecX + vecZ * vecZ);
         if (len > 1.0E-4D) {
            vecX /= len;
            vecZ /= len;
         }

         double dot = frontX * vecX + frontZ * vecZ;
         return dot > 0.4D;
      }
   }

   private void handleWithdraw(DrawerComponent drawer, Player player, int amount) {
      if (drawer.getItemId() != null && drawer.getQuantity() > 0) {
         int toExtract = Math.min(amount, drawer.getQuantity());
         ItemStack toGive = new ItemStack(drawer.getItemId(), toExtract);
         ItemStackTransaction transaction = player.getInventory().getCombinedHotbarFirst().addItemStack(toGive);
         int taken = toExtract;
         if (transaction.getRemainder() != null && !transaction.getRemainder().isEmpty()) {
            taken = toExtract - transaction.getRemainder().getQuantity();
         }

         if (taken > 0) {
            drawer.setQuantity(drawer.getQuantity() - taken);
            if (drawer.getQuantity() <= 0) {
               drawer.setItemId((String)null);
            }

            DebugConfig.debug("[Drawer] Withdrew " + taken);
         }

      }
   }

   private boolean handleDeposit(DrawerComponent drawer, Player player, @Nullable ItemStack heldItem) {
      if (heldItem != null && !heldItem.isEmpty()) {
         boolean canAccept = drawer.getItemId() == null || drawer.getItemId().equals(heldItem.getItemId());
         if (!canAccept) {
            return false;
         } else {
            int capacity = 2400;
            int current = drawer.getQuantity();
            int space = capacity - current;
            if (space <= 0) {
               return false;
            } else {
               int toAdd = Math.min(heldItem.getQuantity(), space);
               ItemStack toRemove = heldItem.withQuantity(toAdd);
               ItemStackTransaction transaction = player.getInventory().getCombinedHotbarFirst().removeItemStack(toRemove);
               if (transaction.succeeded()) {
                  if (drawer.getItemId() == null) {
                     drawer.setItemId(heldItem.getItemId());
                  }

                  drawer.setQuantity(current + toAdd);
                  DebugConfig.debug("[Drawer] Deposited " + toAdd);
                  return true;
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull World world, @Nonnull Vector3i pos) {
      if (interactionType == InteractionType.Primary || interactionType == InteractionType.Use || interactionType == InteractionType.Secondary) {
         BlockFace hitFace = BlockFace.None;
         if (interactionContext.getState().blockFace != BlockFace.None) {
            hitFace = interactionContext.getState().blockFace;
         } else if (interactionContext.getClientState() != null) {
            hitFace = interactionContext.getClientState().blockFace;
         }

         BlockFace frontFace = this.getFrontFace(world, pos);
         boolean isFrontFace = hitFace != BlockFace.None && hitFace == frontFace;
         if (interactionType == InteractionType.Primary) {
            if (isFrontFace) {
               interactionContext.getState().state = InteractionState.Finished;
            } else {
               interactionContext.getState().state = InteractionState.Failed;
            }
         } else if (interactionType == InteractionType.Use || interactionType == InteractionType.Secondary) {
            interactionContext.getState().state = InteractionState.Finished;
         }

      }
   }
}
