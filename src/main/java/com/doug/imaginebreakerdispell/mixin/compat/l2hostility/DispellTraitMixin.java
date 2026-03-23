package com.doug.imaginebreakerdispell.mixin.compat.l2hostility;

import com.doug.imaginebreakerdispell.compat.L2HostilityCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.DispellTrait", remap = false)
abstract class DispellTraitMixin {
    @Inject(
            method = "onDamaged(ILnet/minecraft/world/entity/LivingEntity;Ldev/xkmc/l2damagetracker/contents/attack/AttackCache;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void l2imaginebreakerdispell$ignoreDispellWithImagineBreaker(int level, LivingEntity target, @Coerce Object attackCache, CallbackInfo ci) {
        if (L2HostilityCompat.shouldIgnoreDispell(attackCache)) {
            ci.cancel();
        }
    }
}
