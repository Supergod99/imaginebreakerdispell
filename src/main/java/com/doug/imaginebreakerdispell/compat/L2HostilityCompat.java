package com.doug.imaginebreakerdispell.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.lang.reflect.Method;

public final class L2HostilityCompat {
    private static boolean initialized;
    private static Method attackCacheGetAttacker;
    private static Method curioHasItemInCurioOrSlot;
    private static Item imagineBreakerItem;

    private L2HostilityCompat() {
    }

    private static void init() {
        if (initialized) {
            return;
        }

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> attackCacheClass = Class.forName("dev.xkmc.l2damagetracker.contents.attack.AttackCache", false, loader);
            Class<?> curioCompatClass = Class.forName("dev.xkmc.l2hostility.compat.curios.CurioCompat", false, loader);
            Class<?> registryEntryClass = Class.forName("com.tterrag.registrate.util.entry.RegistryEntry", false, loader);
            Class<?> lhItemsClass = Class.forName("dev.xkmc.l2hostility.init.registrate.LHItems", false, loader);
            Method registryEntryGet = registryEntryClass.getMethod("get");

            attackCacheGetAttacker = attackCacheClass.getMethod("getAttacker");
            curioHasItemInCurioOrSlot = curioCompatClass.getMethod("hasItemInCurioOrSlot", LivingEntity.class, Item.class);

            Object imagineBreakerEntry = lhItemsClass.getField("IMAGINE_BREAKER").get(null);
            imagineBreakerItem = (Item) registryEntryGet.invoke(imagineBreakerEntry);
            initialized = true;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize required L2hostility compat helper", e);
        }
    }

    public static boolean shouldIgnoreDispell(Object attackCache) {
        init();
        if (attackCache == null) {
            return false;
        }

        try {
            LivingEntity attacker = (LivingEntity) attackCacheGetAttacker.invoke(attackCache);
            return attacker != null && (boolean) curioHasItemInCurioOrSlot.invoke(null, attacker, imagineBreakerItem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to evaluate Imagine Breaker Dispell bypass state", e);
        }
    }
}
