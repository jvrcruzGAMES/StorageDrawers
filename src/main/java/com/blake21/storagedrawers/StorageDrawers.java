package com.blake21.storagedrawers;

import com.blake21.storagedrawers.core.component.DrawerComponent;
import com.blake21.storagedrawers.core.interaction.DrawerInteraction;
import com.blake21.storagedrawers.core.system.DrawerPlacementListener;
import com.blake21.storagedrawers.core.system.DrawerSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class StorageDrawers extends JavaPlugin {
   private static StorageDrawers instance;

   public StorageDrawers(@Nonnull JavaPluginInit init) {
      super(init);
      instance = this;
   }

   protected void setup() {
      super.setup();
      this.getLogger().at(Level.INFO).log("StorageDrawers: setup() started...");

      try {
         try {
            this.getLogger().at(Level.INFO).log("Registering DrawerInteraction...");
            this.getCodecRegistry(Interaction.CODEC).register("StorageDrawers:DrawerInteraction", DrawerInteraction.class, DrawerInteraction.CODEC);
            this.getLogger().at(Level.INFO).log("DrawerInteraction registered.");
         } catch (Throwable var4) {
            ((Api)this.getLogger().at(Level.SEVERE).withCause(var4)).log("Failed to register Interaction!");
            throw var4;
         }

         try {
            this.getLogger().at(Level.INFO).log("Registering DrawerComponent...");
            ComponentType<ChunkStore, DrawerComponent> componentType = this.getChunkStoreRegistry().registerComponent(DrawerComponent.class, "StorageDrawers:DrawerComponent", DrawerComponent.CODEC);
            DrawerComponent.setComponentType(componentType);
            this.getLogger().at(Level.INFO).log("DrawerComponent registered.");
            this.getLogger().at(Level.INFO).log("Registering DrawerSystem...");
            this.getChunkStoreRegistry().registerSystem(new DrawerSystem(componentType));
            this.getLogger().at(Level.INFO).log("DrawerSystem registered.");
         } catch (Throwable var3) {
            ((Api)this.getLogger().at(Level.SEVERE).withCause(var3)).log("Failed to register Component or System!");
            throw var3;
         }

         try {
            this.getLogger().at(Level.INFO).log("Registering DrawerPlacementListener...");
            this.getEntityStoreRegistry().registerSystem(new DrawerPlacementListener());
            this.getLogger().at(Level.INFO).log("DrawerPlacementListener registered.");
         } catch (Throwable var2) {
            ((Api)this.getLogger().at(Level.SEVERE).withCause(var2)).log("Failed to register DrawerPlacementListener!");
            throw var2;
         }

         this.getLogger().at(Level.INFO).log("StorageDrawers setup complete!");
      } catch (Throwable var5) {
         ((Api)this.getLogger().at(Level.SEVERE).withCause(var5)).log("StorageDrawers: Critical failure during setup!");
         throw var5;
      }
   }

   public static StorageDrawers getInstance() {
      return instance;
   }
}
