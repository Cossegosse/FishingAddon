package com.FishingAddon.module

import org.cobalt.api.event.annotation.SubscribeEvent
import org.cobalt.api.event.impl.client.TickEvent
import org.cobalt.api.module.Module
import org.cobalt.api.module.setting.impl.KeyBindSetting
import org.cobalt.api.module.setting.impl.TextSetting
import org.cobalt.api.module.setting.impl.SliderSetting
import org.cobalt.api.util.ChatUtils
import org.cobalt.api.util.helper.KeyBind
import org.cobalt.api.util.MouseUtils
import org.lwjgl.glfw.GLFW
import net.minecraft.ChatFormatting
import com.FishingAddon.util.helper.Clock
import kotlin.random.Random
import net.minecraft.client.Minecraft
import java.awt.Robot
import java.awt.event.KeyEvent
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.network.HashedStack
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

object ShardFusion : Module(
  name = "Shard Fusion",
) {
  var keyBind by KeyBindSetting(
    name = "Shard Fusion Keybind",
    description = "Keybind to toggle the shard fusion macro",
    defaultValue = KeyBind(GLFW.GLFW_KEY_K)
  )

  var codSlot by TextSetting(
    name = "Cod slot",
    description = "Slot index for Cod shard (e.g. 11)",
    defaultValue = "11"
  )

  var salmonSlot by TextSetting(
    name = "Salmon slot",
    description = "Slot index for Salmon shard (e.g. 32)",
    defaultValue = "32"
  )

  var fusionSlot by TextSetting(
    name = "Fusion slot",
    description = "Slot index for 'select fusion' (e.g. 15)",
    defaultValue = "15"
  )

  var confirmSlot by TextSetting(
    name = "Confirm slot",
    description = "Slot index for 'confirm fusion' (e.g. 33)",
    defaultValue = "33"
  )

  var openFusionDelay by SliderSetting(
    name = "Open Fusion Delay",
    description = "Delay in ms after opening fusion box",
    defaultValue = 550.0,
    min = 200.0,
    max = 1500.0
  )

  var selectionDelay by SliderSetting(
    name = "Selection Delay",
    description = "Delay in ms between each slot selection",
    defaultValue = 450.0,
    min = 200.0,
    max = 1500.0
  )

  var confirmDelay by SliderSetting(
    name = "Confirm Delay",
    description = "Delay in ms after confirmation",
    defaultValue = 1000.0,
    min = 200.0,
    max = 2000.0
  )

  var cycleDelay by SliderSetting(
    name = "Cycle Delay",
    description = "Delay in ms between fusion cycles",
    defaultValue = 2000.0,
    min = 500.0,
    max = 5000.0
  )

  private var isToggled = false
  private var wasKeyPressed = false
  private var fusionState = FusionState.IDLE
  private val clock = Clock()
  private val mc = Minecraft.getInstance()

  enum class FusionState {
    IDLE,
    OPENING_FUSION,
    SELECTING_COD,
    SELECTING_SALMON,
    SELECTING_FUSION,
    CONFIRMING,
    WAITING,
  }

  fun start() {
    isToggled = true
    fusionState = FusionState.OPENING_FUSION
    ChatUtils.sendMessage("${ChatFormatting.YELLOW}Shard Fusion started!${ChatFormatting.RESET}")
  }

  fun stop() {
    isToggled = false
    fusionState = FusionState.IDLE
    ChatUtils.sendMessage("${ChatFormatting.YELLOW}Shard Fusion stopped!${ChatFormatting.RESET}")
  }

  fun pressKey(key: Int) {
    // kept for compatibility if needed; actual key sending is done by Robot
    GLFW.glfwPostEmptyEvent()
  }

  fun typeNumber(number: String) {
    try {
      val robot = Robot()
      for (digit in number) {
        val keyEvent = when (digit) {
          '0' -> KeyEvent.VK_0
          '1' -> KeyEvent.VK_1
          '2' -> KeyEvent.VK_2
          '3' -> KeyEvent.VK_3
          '4' -> KeyEvent.VK_4
          '5' -> KeyEvent.VK_5
          '6' -> KeyEvent.VK_6
          '7' -> KeyEvent.VK_7
          '8' -> KeyEvent.VK_8
          '9' -> KeyEvent.VK_9
          else -> return
        }
        robot.keyPress(keyEvent)
        Thread.sleep(50)
        robot.keyRelease(keyEvent)
        Thread.sleep(50)
      }
    } catch (e: Exception) {
      // Fallback when Robot unavailable (headless environment, etc)
      // Attempt to click slots directly via container click packets
      for (i in 0 until number.length step 2) {
        val substr = if (i + 2 <= number.length) number.substring(i, i + 2) else number.substring(i, i + 1)
        try {
          val slotIndex = substr.toIntOrNull() ?: continue
          clickSlot(slotIndex)
          Thread.sleep(150)
        } catch (ignored: Exception) { }
      }
    }
  }

  fun clickSlot(slot: Int) {
    try {
      val player = mc.player ?: return
      val menu = player.containerMenu
      val windowId = menu.containerId
      val stateId = menu.incrementStateId()
      val slotNum: Short = slot.toShort()
      val buttonNum: Byte = 0
      val changed = Int2ObjectOpenHashMap<HashedStack>()
      val carried = HashedStack.EMPTY
      val packet = ServerboundContainerClickPacket(windowId, stateId, slotNum, buttonNum, ClickType.PICKUP, changed, carried)
      player.connection.send(packet)
    } catch (e: Exception) {
      ChatUtils.sendMessage("${ChatFormatting.RED}Slot click failed: ${e.message}${ChatFormatting.RESET}")
    }
  }



  @SubscribeEvent
  fun keybindListener(event: TickEvent) {
    val isPressed = keyBind.isPressed()
    if (isPressed && !wasKeyPressed) {
      isToggled = !isToggled

      if (isToggled) start()
      else stop()

      ChatUtils.sendMessage(
        "Shard Fusion is now "
          + (if (isToggled) "§aEnabled" else "§cDisabled")
          + "§r"
      )
    }
    wasKeyPressed = isPressed
  }

  @SubscribeEvent
  fun onTick(event: TickEvent) {
    if (!isToggled) {
      return
    }
    
    if (!clock.passed()) return
    
    when (fusionState) {
      FusionState.OPENING_FUSION -> {
        // Open fusion box and wait for it to appear
        MouseUtils.rightClick()
        val randomVariation = Random.nextInt(-100, 100)
        clock.schedule(openFusionDelay.toInt() + randomVariation)
        fusionState = FusionState.SELECTING_COD
      }

      FusionState.SELECTING_COD -> {
        // Press cod slot
        typeNumber(codSlot.toIntOrNull()?.toString() ?: "11")
        val randomVariation = Random.nextInt(-100, 100)
        clock.schedule(selectionDelay.toInt() + randomVariation)
        fusionState = FusionState.SELECTING_SALMON
      }

      FusionState.SELECTING_SALMON -> {
        // Press salmon slot
        typeNumber(salmonSlot.toIntOrNull()?.toString() ?: "32")
        val randomVariation = Random.nextInt(-100, 100)
        clock.schedule(selectionDelay.toInt() + randomVariation)
        fusionState = FusionState.SELECTING_FUSION
      }

      FusionState.SELECTING_FUSION -> {
        // Press fusion slot
        typeNumber(fusionSlot.toIntOrNull()?.toString() ?: "15")
        val randomVariation = Random.nextInt(-100, 100)
        clock.schedule(selectionDelay.toInt() + randomVariation)
        fusionState = FusionState.CONFIRMING
      }

      FusionState.CONFIRMING -> {
        // Press confirm slot
        typeNumber(confirmSlot.toIntOrNull()?.toString() ?: "33")
        val randomVariation = Random.nextInt(-100, 100)
        clock.schedule(confirmDelay.toInt() + randomVariation)
        fusionState = FusionState.WAITING
      }

      FusionState.WAITING -> {
        // Wait before starting next fusion cycle
        val randomVariation = Random.nextInt(-200, 200)
        clock.schedule(cycleDelay.toInt() + randomVariation)
        fusionState = FusionState.OPENING_FUSION
      }

      FusionState.IDLE -> {
      }
    }
  }

  fun isToggled(): Boolean {
    return isToggled
  }
}
