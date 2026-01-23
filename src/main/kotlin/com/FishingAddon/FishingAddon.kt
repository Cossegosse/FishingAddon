package com.FishingAddon

import org.cobalt.api.addon.Addon
import org.cobalt.api.event.EventBus
import org.cobalt.api.module.Module
import com.FishingAddon.module.Main

object FishingAddon : Addon() {

  override fun onLoad() {
    EventBus.register(Main)
    println("FishingAddon loaded!")
  }

  override fun onUnload() {
    println("FishingAddon unloaded!")
  }

  override fun getModules(): List<Module> {
    return listOf(Main)
  }
}
