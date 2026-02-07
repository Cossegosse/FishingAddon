package com.FishingAddon.mixins;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public class MinecraftFishingbobber {

  @Inject(method = "tick", at = @At("TAIL"))
  private void onTick(CallbackInfo ci) {
  }
}
