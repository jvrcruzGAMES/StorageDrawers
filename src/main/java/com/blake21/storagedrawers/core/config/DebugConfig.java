package com.blake21.storagedrawers.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class DebugConfig {
   private static boolean debugEnabled = false;
   private static boolean initialized = false;

   private static void loadConfig() {
      if (!initialized) {
         initialized = true;

         try {
            InputStream is = DebugConfig.class.getClassLoader().getResourceAsStream("storagedrawers_config.json");

            label74: {
               try {
                  if (is != null) {
                     Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                     StringBuilder sb = new StringBuilder();

                     int ch;
                     while((ch = reader.read()) != -1) {
                        sb.append((char)ch);
                     }

                     String json = sb.toString();
                     if (json.contains("\"debugEnabled\"")) {
                        int idx = json.indexOf("\"debugEnabled\"");
                        String afterKey = json.substring(idx + 14);
                        debugEnabled = afterKey.contains("true");
                     }

                     System.out.println("[StorageDrawers] Debug mode: " + (debugEnabled ? "ENABLED" : "DISABLED"));
                     break label74;
                  }

                  System.out.println("[StorageDrawers] Config not found, debug disabled by default.");
               } catch (Throwable var8) {
                  if (is != null) {
                     try {
                        is.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (is != null) {
                  is.close();
               }

               return;
            }

            if (is != null) {
               is.close();
            }
         } catch (IOException var9) {
            System.err.println("[StorageDrawers] Failed to load config: " + var9.getMessage());
         }

      }
   }

   public static boolean isDebugEnabled() {
      return debugEnabled;
   }

   public static void debug(String message) {
      if (debugEnabled) {
         System.out.println(message);
      }

   }

   static {
      loadConfig();
   }
}
