package com.blake21.storagedrawers.core.system;

import com.blake21.storagedrawers.core.config.DebugConfig;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bson.BsonDocument;
import org.bson.BsonValue;

public class DrawerPlacementListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
   private static final Map<Vector3i, DrawerPlacementListener.DrawerData> PENDING_DRAWER_DATA = new ConcurrentHashMap();

   public DrawerPlacementListener() {
      super(PlaceBlockEvent.class);
   }

   public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
      ItemStack itemInHand = event.getItemInHand();
      DebugConfig.debug("[DrawerPlacementListener] PlaceBlockEvent triggered");
      if (itemInHand == null) {
         DebugConfig.debug("[DrawerPlacementListener] Item in hand is null");
      } else {
         String itemId = itemInHand.getItemId();
         DebugConfig.debug("[DrawerPlacementListener] Placing item: " + itemId);
         if (!itemId.contains("storage_drawer") && !itemId.contains("StorageDrawers")) {
            DebugConfig.debug("[DrawerPlacementListener] Not a storage drawer, skipping");
         } else {
            BsonDocument fullMetadata = itemInHand.getMetadata();
            DebugConfig.debug("[DrawerPlacementListener] Item has metadata: " + (fullMetadata != null));
            if (fullMetadata != null) {
               DebugConfig.debug("[DrawerPlacementListener] Metadata keys: " + String.valueOf(fullMetadata.keySet()));
            }

            BsonDocument drawerContents = (BsonDocument)itemInHand.getFromMetadataOrNull("DrawerContents", Codec.BSON_DOCUMENT);
            DebugConfig.debug("[DrawerPlacementListener] DrawerContents found: " + (drawerContents != null));
            if (drawerContents == null) {
               DebugConfig.debug("[DrawerPlacementListener] No DrawerContents in metadata");
            } else {
               BsonValue itemIdValue = drawerContents.get("ItemId");
               BsonValue countValue = drawerContents.get("Count");
               String var10000 = String.valueOf(itemIdValue);
               DebugConfig.debug("[DrawerPlacementListener] ItemId value: " + var10000 + ", Count value: " + String.valueOf(countValue));
               if (itemIdValue != null && countValue != null) {
                  String storedItemId = itemIdValue.asString().getValue();
                  int storedCount = countValue.asInt32().getValue();
                  if (storedCount > 0 && storedItemId != null && !storedItemId.isEmpty()) {
                     Vector3i targetBlock = event.getTargetBlock().clone();
                     PENDING_DRAWER_DATA.put(targetBlock, new DrawerPlacementListener.DrawerData(storedItemId, storedCount));
                     DebugConfig.debug("[DrawerPlacementListener] Queued restore: " + storedItemId + " x" + storedCount + " at " + String.valueOf(targetBlock));
                  }
               }

            }
         }
      }
   }

   @Nullable
   public Query<EntityStore> getQuery() {
      return Archetype.empty();
   }

   @Nullable
   public static DrawerPlacementListener.DrawerData consumePendingData(Vector3i position) {
      DrawerPlacementListener.DrawerData data = (DrawerPlacementListener.DrawerData)PENDING_DRAWER_DATA.remove(position);
      if (data != null) {
         return data;
      } else {
         Vector3i normalizedPos = new Vector3i(position.x & 31, position.y, position.z & 31);
         return (DrawerPlacementListener.DrawerData)PENDING_DRAWER_DATA.remove(normalizedPos);
      }
   }

   public static class DrawerData {
      public final String itemId;
      public final int count;

      public DrawerData(String itemId, int count) {
         this.itemId = itemId;
         this.count = count;
      }
   }
}
