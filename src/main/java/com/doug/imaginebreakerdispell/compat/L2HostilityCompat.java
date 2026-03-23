package com.doug.imaginebreakerdispell.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public final class L2HostilityCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String L2HOSTILITY_MODID = "l2hostility";
    private static final int LISTENER_PRIORITY = 4501;
    private static final int DISPELL_PRIORITY = 7436;

    private static boolean initialized;
    private static Hook hook;

    private L2HostilityCompat() {
    }

    public static void init() {
        if (initialized || !ModList.get().isLoaded(L2HOSTILITY_MODID)) {
            return;
        }

        try {
            hook = new Hook();
            hook.register();
            initialized = true;
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to register L2hostility compat hook", e);
        }
    }

    private static final class Hook {
        private final Constructor<?> customAttackListenerCtor;
        private final Method subscribeDamage;
        private final Method register;
        private final Method attackCacheGetAttacker;
        private final Method attackCacheGetTarget;
        private final Method attackCacheGetPlayerAttackEvent;
        private final Method attackCacheGetLivingDamageEvent;
        private final Method attackCacheAddDealtModifier;
        private final Method l2hHelperOf;
        private final Method mobTraitCapHasTrait;
        private final Method curioHasItemInCurioOrSlot;
        private final Method registryEntryGet;
        private final Method doubleValueGet;
        private final Method damageModifierNonlinearPre;
        private final Class<?> float2FloatFunctionClass;

        private final Object dispellTrait;
        private final Item imagineBreakerItem;
        private final Object dispellDamageReductionBase;
        private final TagKey<DamageType> bypassTagA;
        private final TagKey<DamageType> bypassTagB;
        private final TagKey<DamageType> magicTag;

        private Hook() throws ReflectiveOperationException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            Class<?> customAttackListenerClass = Class.forName("dev.xkmc.l2damagetracker.compat.CustomAttackListener", false, loader);
            Class<?> attackCacheClass = Class.forName("dev.xkmc.l2damagetracker.contents.attack.AttackCache", false, loader);
            Class<?> damageModifierClass = Class.forName("dev.xkmc.l2damagetracker.contents.attack.DamageModifier", false, loader);
            Class<?> l2hHelperClass = Class.forName("dev.xkmc.l2hostility.compat.kubejs.L2HHelper", false, loader);
            Class<?> mobTraitCapClass = Class.forName("dev.xkmc.l2hostility.content.capability.mob.MobTraitCap", false, loader);
            Class<?> curioCompatClass = Class.forName("dev.xkmc.l2hostility.compat.curios.CurioCompat", false, loader);
            Class<?> lhTraitsClass = Class.forName("dev.xkmc.l2hostility.init.registrate.LHTraits", false, loader);
            Class<?> lhConfigClass = Class.forName("dev.xkmc.l2hostility.init.data.LHConfig", false, loader);
            Class<?> registryEntryClass = Class.forName("com.tterrag.registrate.util.entry.RegistryEntry", false, loader);
            Class<?> doubleValueClass = Class.forName("net.minecraftforge.common.ForgeConfigSpec$DoubleValue", false, loader);

            this.float2FloatFunctionClass = Class.forName("it.unimi.dsi.fastutil.floats.Float2FloatFunction", false, loader);
            this.customAttackListenerCtor = customAttackListenerClass.getConstructor();
            this.subscribeDamage = customAttackListenerClass.getMethod("subscribeDamage", Consumer.class);
            this.register = customAttackListenerClass.getMethod("register", int.class);
            this.attackCacheGetAttacker = attackCacheClass.getMethod("getAttacker");
            this.attackCacheGetTarget = attackCacheClass.getMethod("getAttackTarget");
            this.attackCacheGetPlayerAttackEvent = attackCacheClass.getMethod("getPlayerAttackEntityEvent");
            this.attackCacheGetLivingDamageEvent = attackCacheClass.getMethod("getLivingDamageEvent");
            this.attackCacheAddDealtModifier = attackCacheClass.getMethod("addDealtModifier", damageModifierClass);
            this.l2hHelperOf = l2hHelperClass.getMethod("of", Entity.class);
            this.mobTraitCapHasTrait = mobTraitCapClass.getMethod("hasTrait", Class.forName("dev.xkmc.l2hostility.content.traits.base.MobTrait", false, loader));
            this.curioHasItemInCurioOrSlot = curioCompatClass.getMethod("hasItemInCurioOrSlot", LivingEntity.class, Item.class);
            this.registryEntryGet = registryEntryClass.getMethod("get");
            this.doubleValueGet = doubleValueClass.getMethod("get");
            this.damageModifierNonlinearPre = damageModifierClass.getMethod("nonlinearPre", int.class, this.float2FloatFunctionClass);

            Object dispellEntry = lhTraitsClass.getField("DISPELL").get(null);
            this.dispellTrait = this.registryEntryGet.invoke(dispellEntry);

            Class<?> lhItemsClass = Class.forName("dev.xkmc.l2hostility.init.registrate.LHItems", false, loader);
            Object imagineBreakerEntry = lhItemsClass.getField("IMAGINE_BREAKER").get(null);
            this.imagineBreakerItem = (Item) this.registryEntryGet.invoke(imagineBreakerEntry);

            Object commonConfig = lhConfigClass.getField("COMMON").get(null);
            Field dispellBaseField = commonConfig.getClass().getField("dispellDamageReductionBase");
            this.dispellDamageReductionBase = dispellBaseField.get(commonConfig);

            Class<?> damageTypeTagsClass = Class.forName("net.minecraft.tags.DamageTypeTags", false, loader);
            this.bypassTagA = castTagKey(damageTypeTagsClass.getField("f_268738_").get(null));
            this.bypassTagB = castTagKey(damageTypeTagsClass.getField("f_268437_").get(null));

            Class<?> l2DamageTypesClass = Class.forName("dev.xkmc.l2damagetracker.init.data.L2DamageTypes", false, loader);
            this.magicTag = castTagKey(l2DamageTypesClass.getField("MAGIC").get(null));
        }

        private void register() throws ReflectiveOperationException {
            Object listener = this.customAttackListenerCtor.newInstance();
            this.subscribeDamage.invoke(listener, (Consumer<Object>) this::onDamage);
            this.register.invoke(listener, LISTENER_PRIORITY);
        }

        private void onDamage(Object attackCache) {
            try {
                LivingEntity attacker = (LivingEntity) this.attackCacheGetAttacker.invoke(attackCache);
                LivingEntity target = (LivingEntity) this.attackCacheGetTarget.invoke(attackCache);
                AttackEntityEvent playerAttackEvent = (AttackEntityEvent) this.attackCacheGetPlayerAttackEvent.invoke(attackCache);
                LivingDamageEvent damageEvent = (LivingDamageEvent) this.attackCacheGetLivingDamageEvent.invoke(attackCache);

                if (attacker == null || target == null || playerAttackEvent == null || damageEvent == null) {
                    return;
                }

                DamageSource source = damageEvent.getSource();
                if (!source.is(this.magicTag) || source.is(this.bypassTagA) || source.is(this.bypassTagB)) {
                    return;
                }

                if (!(playerAttackEvent.getTarget() instanceof LivingEntity)) {
                    return;
                }

                if (!this.hasImagineBreaker(attacker) || !this.hasDispell(target)) {
                    return;
                }

                double base = this.getDispellReductionBase();
                if (base <= 0.0D || base == 1.0D) {
                    return;
                }

                Object inverse = this.createInverseFunction(base);
                Object modifier = this.damageModifierNonlinearPre.invoke(null, DISPELL_PRIORITY, inverse);
                this.attackCacheAddDealtModifier.invoke(attackCache, modifier);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to apply L2hostility compat damage hook", e);
            }
        }

        private boolean hasImagineBreaker(LivingEntity attacker) throws ReflectiveOperationException {
            return (boolean) this.curioHasItemInCurioOrSlot.invoke(null, attacker, this.imagineBreakerItem);
        }

        private boolean hasDispell(LivingEntity target) throws ReflectiveOperationException {
            Object cap = this.l2hHelperOf.invoke(null, target);
            return cap != null && (boolean) this.mobTraitCapHasTrait.invoke(cap, this.dispellTrait);
        }

        private double getDispellReductionBase() throws ReflectiveOperationException {
            return ((Number) this.doubleValueGet.invoke(this.dispellDamageReductionBase)).doubleValue();
        }

        private Object createInverseFunction(double base) {
            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if (method.getDeclaringClass() == Object.class) {
                    return switch (name) {
                        case "toString" -> "l2_imaginebreakerdispell_inverse_dispell";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }

                if (args != null && args.length > 0 && args[0] instanceof Number number) {
                    float reduced = number.floatValue();
                    float restored = reduced < 1.0F ? (float) (reduced * base) : (float) Math.pow(base, reduced);
                    Class<?> returnType = method.getReturnType();
                    if (returnType == float.class || returnType == Float.class) {
                        return restored;
                    }
                    if (returnType == double.class || returnType == Double.class) {
                        return (double) restored;
                    }
                    if (returnType == int.class || returnType == Integer.class) {
                        return (int) restored;
                    }
                    return restored;
                }

                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class || returnType == Boolean.class) {
                    return false;
                }
                if (returnType == float.class || returnType == Float.class) {
                    return 0.0F;
                }
                if (returnType == double.class || returnType == Double.class) {
                    return 0.0D;
                }
                if (returnType == int.class || returnType == Integer.class) {
                    return 0;
                }
                return null;
            };

            return Proxy.newProxyInstance(
                    this.float2FloatFunctionClass.getClassLoader(),
                    new Class<?>[]{this.float2FloatFunctionClass},
                    handler
            );
        }

        @SuppressWarnings("unchecked")
        private static TagKey<DamageType> castTagKey(Object value) {
            return (TagKey<DamageType>) value;
        }
    }
}
