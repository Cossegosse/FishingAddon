package com.FishingAddon.module

import org.cobalt.api.event.annotation.SubscribeEvent
import org.cobalt.api.event.impl.client.TickEvent
import org.cobalt.api.module.Module
import org.cobalt.api.module.setting.impl.KeyBindSetting
import org.cobalt.api.util.ChatUtils
import org.cobalt.api.util.MouseUtils
import org.cobalt.api.util.helper.KeyBind
import org.cobalt.api.util.InventoryUtils
import org.lwjgl.glfw.GLFW

object Main : Module(
  name = "Main tab",
) {
  var keyBind by KeyBindSetting(
    name = "Macro Keybind",
    description = "Keybind to toggle the macro",
    defaultValue = KeyBind(GLFW.GLFW_KEY_J)
  )

  private var isToggled = false
  private var wasKeyPressed = false
  private var macroState = Macrostate.IDLE
  private var hasValidFishingRod = false

  enum class Macrostate {
    IDLE,
    CASTING,
    REELING,
    RESETTING
  }

  fun checkFishingRod() {
    hasValidFishingRod = InventoryUtils.findItemInHotbar("fishing_rod")
  }

  fun start() {
    isToggled = true
    MouseUtils.ungrabMouse()
  }

  fun stop() {
    isToggled = false
    MouseUtils.grabMouse()
    resetStates()
  }

  fun resetStates() {
    macroState = Macrostate.IDLE
  }

  @SubscribeEvent
  fun keybindListener(event: TickEvent) {
    val isPressed = keyBind.isPressed()
    if (isPressed && !wasKeyPressed) {
      isToggled = !isToggled

      if (isToggled) start()
      else stop()

      ChatUtils.sendMessage(
        "Fishing Macro is now "
                + (if (isToggled) "§aEnabled" else "§cDisabled")
                + "§r"
      )
    }
    wasKeyPressed = isPressed
  }

  fun isToggled(): Boolean {
    return isToggled
  }

  @SubscribeEvent
  fun onTick(event: TickEvent) {
    if (!isToggled) {
      return
    }
  }
}
