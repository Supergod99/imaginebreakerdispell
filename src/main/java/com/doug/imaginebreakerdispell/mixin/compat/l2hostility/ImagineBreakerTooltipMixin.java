package com.doug.imaginebreakerdispell.mixin.compat.l2hostility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.item.curio.misc.ImagineBreaker", remap = false)
abstract class ImagineBreakerTooltipMixin {
    private static final String MAGIC_BYPASS_TOOLTIP_KEY = "tooltip.l2_imaginebreakerdispell.imagine_breaker.magic_bypass";

    private static boolean tooltipMethodsResolved;
    private static Method componentTranslatableMethod;
    private static Method mutableComponentWithStyleMethod;

    @Inject(
            method = {
                    "appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
                    "m_7373_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            require = 1,
            remap = false
    )
    private void l2imaginebreakerdispell$addMagicBypassTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        Component component = this.l2imaginebreakerdispell$createMagicBypassTooltip();
        if (component != null) {
            tooltip.add(component);
        }
    }

    private Component l2imaginebreakerdispell$createMagicBypassTooltip() {
        try {
            l2imaginebreakerdispell$resolveTooltipMethods();
            if (componentTranslatableMethod == null) {
                return null;
            }

            Object component = componentTranslatableMethod.invoke(null, MAGIC_BYPASS_TOOLTIP_KEY, new Object[0]);
            if (component == null) {
                return null;
            }

            if (mutableComponentWithStyleMethod != null) {
                component = mutableComponentWithStyleMethod.invoke(component, ChatFormatting.GOLD);
            }
            return (Component) component;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void l2imaginebreakerdispell$resolveTooltipMethods() {
        if (tooltipMethodsResolved) {
            return;
        }
        componentTranslatableMethod = l2imaginebreakerdispell$findTranslatableMethod();
        mutableComponentWithStyleMethod = l2imaginebreakerdispell$findWithStyleMethod();
        tooltipMethodsResolved = true;
    }

    private static Method l2imaginebreakerdispell$findTranslatableMethod() {
        for (Method method : Component.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!MutableComponent.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[0] == String.class && params[1].isArray() && params[1].getComponentType() == Object.class) {
                return method;
            }
        }
        return null;
    }

    private static Method l2imaginebreakerdispell$findWithStyleMethod() {
        for (Method method : MutableComponent.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!MutableComponent.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == ChatFormatting.class) {
                return method;
            }
        }
        return null;
    }
}
