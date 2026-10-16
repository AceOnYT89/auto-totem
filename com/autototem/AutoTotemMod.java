package com.autototem;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BeforeInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyPress;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.BeforeKeyRelease;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_357;
import net.minecraft.class_3675;
import net.minecraft.class_370;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_746;
import net.minecraft.class_304.class_11900;
import net.minecraft.class_3675.class_307;
import net.minecraft.class_370.class_9037;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoTotemMod implements ClientModInitializer, ModMenuApi {
   private static final class_11900 CATEGORY = class_11900.method_74698(class_2960.method_60655("autototem", "main"));
   private static class_304 toggleKey;
   private static int toggleKeyCode = 59;
   private static int delayMs = 0;
   private static boolean active = false;
   private static boolean toggleHeld = false;
   private static int manualProtectionTicks = 0;
   private static boolean fWasDown = false;
   private static boolean leftMouseWasDown = false;
   private static boolean rightMouseWasDown = false;
   private static long nextAutomaticSwapTime = 0L;
   private static long automaticClickGuardUntil = 0L;
   private static boolean toastShown = false;
   private static final Path CONFIG_PATH = Path.of(System.getProperty("user.dir"), "config", "autototem.txt");

   public void onInitializeClient() {
      loadConfig();
      registerKeyBinding();
      registerScreenKeyboardEvents();
      ClientTickEvents.END_CLIENT_TICK.register(AutoTotemMod::tick);
   }

   private static void tick(class_310 client) {
      showLaunchToast(client);
      if (client.field_1755 != null) {
         while (toggleKey.method_1436()) {
         }
      } else {
         while (toggleKey.method_1436()) {
            toggleAutoTotem();
         }

         toggleHeld = false;
      }

      if (client.field_1724 != null && client.field_1761 != null) {
         class_746 player = client.field_1724;
         boolean fDown = class_3675.method_15987(client.method_22683(), 70);
         if (fDown && !fWasDown) {
            protectFromManualAction(2);
         }

         fWasDown = fDown;
         boolean leftMouseDown = GLFW.glfwGetMouseButton(client.method_22683().method_4490(), 0) == 1;
         boolean rightMouseDown = GLFW.glfwGetMouseButton(client.method_22683().method_4490(), 1) == 1;
         if (leftMouseDown && !leftMouseWasDown && client.field_1755 != null) {
            protectFromManualAction(3);
         }

         if (rightMouseDown && !rightMouseWasDown && client.field_1755 != null) {
            protectFromManualAction(3);
         }

         leftMouseWasDown = leftMouseDown;
         rightMouseWasDown = rightMouseDown;
         if (manualProtectionTicks > 0) {
            manualProtectionTicks--;
         } else if (active) {
            if (!player.method_6115()) {
               if (!player.method_6079().method_31574(class_1802.field_8288)) {
                  long now = System.currentTimeMillis();
                  if (now >= nextAutomaticSwapTime) {
                     if (now >= automaticClickGuardUntil) {
                        int totemSlot = findTotem(player);
                        if (totemSlot != -1) {
                           int screenSlot = totemSlot < 9 ? totemSlot + 36 : totemSlot;
                           int actualDelay = clampDelay(delayMs);
                           nextAutomaticSwapTime = now + actualDelay;
                           automaticClickGuardUntil = now + 1L;
                           client.field_1761.method_2906(player.field_7512.field_7763, screenSlot, 40, class_1713.field_7791, player);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int findTotem(class_746 player) {
      for (int slot = 0; slot < 36; slot++) {
         class_1799 stack = player.method_31548().method_5438(slot);
         if (stack.method_31574(class_1802.field_8288)) {
            return slot;
         }
      }

      return -1;
   }

   private static void toggleAutoTotem() {
      active = !active;
      manualProtectionTicks = 0;
      nextAutomaticSwapTime = 0L;
      automaticClickGuardUntil = 0L;
   }

   private static void protectFromManualAction(int ticks) {
      manualProtectionTicks = Math.max(manualProtectionTicks, ticks);
      nextAutomaticSwapTime = 0L;
      automaticClickGuardUntil = 0L;
   }

   private static void registerScreenKeyboardEvents() {
      ScreenEvents.BEFORE_INIT.register((BeforeInit)(client, screen, scaledWidth, scaledHeight) -> {
         ScreenKeyboardEvents.beforeKeyPress(screen).register((BeforeKeyPress)(currentScreen, event) -> {
            int key = event.comp_4795();
            int boundKey = KeyBindingHelper.getBoundKeyOf(toggleKey).method_1444();
            if (key == boundKey) {
               if (!toggleHeld) {
                  toggleAutoTotem();
                  toggleHeld = true;
               }
            } else if (key == 70) {
               protectFromManualAction(2);
            } else {
               if (key >= 49 && key <= 57) {
                  protectFromManualAction(2);
               }
            }
         });
         ScreenKeyboardEvents.beforeKeyRelease(screen).register((BeforeKeyRelease)(currentScreen, event) -> {
            int key = event.comp_4795();
            int boundKey = KeyBindingHelper.getBoundKeyOf(toggleKey).method_1444();
            if (key == boundKey) {
               toggleHeld = false;
            }
         });
      });
   }

   private static void showLaunchToast(class_310 client) {
      if (!toastShown && client.field_1687 == null && client.field_1755 != null) {
         class_370.method_27024(
            client.method_1566(), class_9037.field_47588, class_2561.method_43470("AutoTotem Unlocked"), class_2561.method_43470("Press ; to enable it.")
         );
         toastShown = true;
      }
   }

   private static void registerKeyBinding() {
      toggleKey = KeyBindingHelper.registerKeyBinding(new class_304("key.autototem.toggle", class_307.field_1668, toggleKeyCode, CATEGORY));
   }

   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return AutoTotemMod.ConfigScreen::new;
   }

   private static void loadConfig() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         if (!Files.exists(CONFIG_PATH)) {
            delayMs = 0;
            toggleKeyCode = 59;
            saveConfig();
            return;
         }

         for (String line : Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.startsWith("delayMs=")) {
               try {
                  delayMs = Integer.parseInt(value.substring("delayMs=".length()));
               } catch (NumberFormatException ignored) {
                  delayMs = 0;
               }
            }

            if (value.startsWith("toggleKey=")) {
               try {
                  toggleKeyCode = Integer.parseInt(value.substring("toggleKey=".length()));
               } catch (NumberFormatException ignored) {
                  toggleKeyCode = 59;
               }
            }
         }

         delayMs = clampDelay(delayMs);
      } catch (IOException ignored) {
         delayMs = 0;
         toggleKeyCode = 59;
      }
   }

   private static void saveConfig() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());
         String text = "# AutoTotem configuration\n# delayMs = 0-150 milliseconds\n# 0 = instant / no artificial delay\n# toggleKey = GLFW key code\n\ndelayMs="
            + delayMs
            + "\ntoggleKey="
            + toggleKeyCode
            + "\n";
         Files.writeString(CONFIG_PATH, text, StandardCharsets.UTF_8);
      } catch (IOException var1) {
      }
   }

   private static int clampDelay(int value) {
      return Math.max(0, Math.min(150, value));
   }

   private static final class ConfigScreen extends class_437 {
      private final class_437 parent;
      private AutoTotemMod.DelaySlider delaySlider;

      protected ConfigScreen(class_437 parent) {
         super(class_2561.method_43470("AutoTotem Settings"));
         this.parent = parent;
      }

      protected void method_25426() {
         int centerX = this.field_22789 / 2;
         int sliderWidth = 250;
         int sliderX = centerX - sliderWidth / 2;
         int sliderY = this.field_22790 / 2 - 30;
         this.delaySlider = new AutoTotemMod.DelaySlider(sliderX, sliderY, sliderWidth, 20, AutoTotemMod.delayMs);
         this.method_37063(this.delaySlider);
         class_4185 doneButton = class_4185.method_46430(class_2561.method_43470("Done"), button -> {
            AutoTotemMod.delayMs = AutoTotemMod.clampDelay(this.delaySlider.getDelay());
            AutoTotemMod.saveConfig();
            if (this.field_22787 != null) {
               this.field_22787.method_1507(this.parent);
            }
         }).method_46434(centerX - 100, sliderY + 45, 200, 20).method_46431();
         this.method_37063(doneButton);
      }

      public void method_25419() {
         if (this.delaySlider != null) {
            AutoTotemMod.delayMs = AutoTotemMod.clampDelay(this.delaySlider.getDelay());
         }

         AutoTotemMod.saveConfig();
         if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
         }
      }
   }

   private static final class DelaySlider extends class_357 {
      private int delay;

      private DelaySlider(int x, int y, int width, int height, int initialDelay) {
         super(x, y, width, height, class_2561.method_43473(), AutoTotemMod.clampDelay(initialDelay) / 150.0);
         this.delay = AutoTotemMod.clampDelay(initialDelay);
         this.method_25346();
      }

      protected void method_25346() {
         if (this.delay == 0) {
            this.method_25355(class_2561.method_43470("Swap delay: INSTANT (0 ms)"));
         } else {
            this.method_25355(class_2561.method_43470("Swap delay: " + this.delay + " ms"));
         }
      }

      protected void method_25344() {
         this.delay = (int)Math.round(this.field_22753 * 150.0);
         this.delay = AutoTotemMod.clampDelay(this.delay);
         this.method_25346();
      }

      private int getDelay() {
         return this.delay;
      }
   }
}
