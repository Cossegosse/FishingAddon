package com.FishingAddon.module

import org.cobalt.api.event.annotation.SubscribeEvent
import org.cobalt.api.event.impl.client.TickEvent
import org.cobalt.api.module.Module
import org.cobalt.api.module.setting.impl.KeyBindSetting
import org.cobalt.api.util.ChatUtils
import org.cobalt.api.util.MouseUtils
import org.cobalt.api.util.helper.KeyBind
import org.cobalt.api.util.InventoryUtils
import com.FishingAddon.util.helper.Clock
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.decoration.ArmorStand
import org.lwjgl.glfw.GLFW
import kotlin.random.Random
import net.minecraft.ChatFormatting

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
  private var macroState = MacroState.IDLE
  private val clock = Clock()
  private val mc = Minecraft.getInstance()

  enum class MacroState {
    IDLE,
    SWAP_TO_ROD,
    CASTING,
    WAITING,
    REELING,
    RESETTING,

  }

  fun detectFishbite(): Boolean {
    val entities = mc.level?.entitiesForRendering() ?: return false
    var armorStandsChecked = 0
    var fishBiteStands = 0
    for (entity in entities) {
      if (entity is ArmorStand) {
        armorStandsChecked++

        if (entity.hasCustomName()) {
          val customName = entity.customName
          if (customName != null) {
            val nameString = customName.string
            if (nameString == "!!!") {
              fishBiteStands++
            }
          }
        }
      }
    }
    return fishBiteStands > 0
  }

  fun swapToFishingRod() {
    val slot = InventoryUtils.findItemInHotbar("rod")

    if (slot == -1) {
      ChatUtils.sendMessage("${ChatFormatting.RED}No Fishing Rod found in hotbar! Disabling macro.${ChatFormatting.RESET}")
      stop()
      return
    }

    InventoryUtils.holdHotbarSlot(slot)
  }
    fun start() {
      isToggled = true
      MouseUtils.ungrabMouse()
      macroState = MacroState.SWAP_TO_ROD
    }

    fun resetStates() {
      macroState = MacroState.IDLE
    }

    fun stop() {
      isToggled = false
      MouseUtils.grabMouse()
      resetStates()
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
      if (!clock.passed()) return
      when (macroState) {
        MacroState.SWAP_TO_ROD -> {
          swapToFishingRod()
          clock.schedule(Random.nextInt(200, 500))
          macroState = MacroState.CASTING
        }

        MacroState.CASTING -> {
          MouseUtils.rightClick()
          clock.schedule(Random.nextInt(100, 200))
          macroState = MacroState.WAITING
        }

        MacroState.WAITING -> {
          if (detectFishbite()) {
            clock.schedule(Random.nextInt(50, 150))
            macroState = MacroState.REELING
          }
        }

        MacroState.REELING -> {
          MouseUtils.rightClick()
          macroState = MacroState.RESETTING

        }

        MacroState.RESETTING -> {
          clock.schedule(Random.nextInt(200, 500))
          macroState = MacroState.CASTING
        }

        MacroState.IDLE -> {
        }
      }
    }
  }

