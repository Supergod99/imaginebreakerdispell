package com.doug.imaginebreakerdispell.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class L2HostilityCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String L2HOSTILITY_MODID = "l2hostility";
    private static final int LISTENER_PRIORITY = 4501;

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
        private final Method subscribeCreateSource;
        private final Method register;
        private final Method createSourceGetAttacker;
        private final Method createSourceGetRegistry;
        private final Method createSourceGetResult;
        private final Method createSourceEnable;
        private final Method curioHasItemInCurioOrSlot;
        private final Method damageTypeWrapperType;
        private final Method registryEntryGet;

        private final Object bypassMagicState;
        private final Item imagineBreakerItem;
        private final TagKey<DamageType> magicTag;

        private Hook() throws ReflectiveOperationException {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            Class<?> customAttackListenerClass = Class.forName("dev.xkmc.l2damagetracker.compat.CustomAttackListener", false, loader);
            Class<?> createSourceEventClass = Class.forName("dev.xkmc.l2damagetracker.contents.attack.CreateSourceEvent", false, loader);
            Class<?> damageStateClass = Class.forName("dev.xkmc.l2damagetracker.contents.damage.DamageState", false, loader);
            Class<?> damageTypeWrapperClass = Class.forName("dev.xkmc.l2damagetracker.contents.damage.DamageTypeWrapper", false, loader);
            Class<?> curioCompatClass = Class.forName("dev.xkmc.l2hostility.compat.curios.CurioCompat", false, loader);
            Class<?> registryEntryClass = Class.forName("com.tterrag.registrate.util.entry.RegistryEntry", false, loader);
            Class<?> defaultDamageStateClass = Class.forName("dev.xkmc.l2damagetracker.contents.damage.DefaultDamageState", false, loader);

            this.customAttackListenerCtor = customAttackListenerClass.getConstructor();
            this.subscribeCreateSource = customAttackListenerClass.getMethod("subscribeCreateSource", Consumer.class);
            this.register = customAttackListenerClass.getMethod("register", int.class);
            this.createSourceGetAttacker = createSourceEventClass.getMethod("getAttacker");
            this.createSourceGetRegistry = createSourceEventClass.getMethod("getRegistry");
            this.createSourceGetResult = createSourceEventClass.getMethod("getResult");
            this.createSourceEnable = createSourceEventClass.getMethod("enable", damageStateClass);
            this.curioHasItemInCurioOrSlot = curioCompatClass.getMethod("hasItemInCurioOrSlot", LivingEntity.class, Item.class);
            this.damageTypeWrapperType = damageTypeWrapperClass.getMethod("type");
            this.registryEntryGet = registryEntryClass.getMethod("get");
            this.bypassMagicState = defaultDamageStateClass.getField("BYPASS_MAGIC").get(null);

            Class<?> lhItemsClass = Class.forName("dev.xkmc.l2hostility.init.registrate.LHItems", false, loader);
            Object imagineBreakerEntry = lhItemsClass.getField("IMAGINE_BREAKER").get(null);
            this.imagineBreakerItem = (Item) this.registryEntryGet.invoke(imagineBreakerEntry);

            Class<?> l2DamageTypesClass = Class.forName("dev.xkmc.l2damagetracker.init.data.L2DamageTypes", false, loader);
            this.magicTag = castTagKey(l2DamageTypesClass.getField("MAGIC").get(null));
        }

        private void register() throws ReflectiveOperationException {
            Object listener = this.customAttackListenerCtor.newInstance();
            this.subscribeCreateSource.invoke(listener, (Consumer<Object>) this::onCreateSource);
            this.register.invoke(listener, LISTENER_PRIORITY);
        }

        private void onCreateSource(Object createSourceEvent) {
            try {
                LivingEntity attacker = (LivingEntity) this.createSourceGetAttacker.invoke(createSourceEvent);
                if (attacker == null || !this.hasImagineBreaker(attacker)) {
                    return;
                }

                Object result = this.createSourceGetResult.invoke(createSourceEvent);
                if (result == null || !this.isMagicDamage(createSourceEvent, result)) {
                    return;
                }

                this.createSourceEnable.invoke(createSourceEvent, this.bypassMagicState);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to apply L2hostility compat source hook", e);
            }
        }

        private boolean hasImagineBreaker(LivingEntity attacker) throws ReflectiveOperationException {
            return (boolean) this.curioHasItemInCurioOrSlot.invoke(null, attacker, this.imagineBreakerItem);
        }

        private boolean isMagicDamage(Object createSourceEvent, Object damageTypeWrapper) throws ReflectiveOperationException {
            Registry<DamageType> registry = castDamageTypeRegistry(this.createSourceGetRegistry.invoke(createSourceEvent));
            ResourceKey<DamageType> type = castDamageTypeKey(this.damageTypeWrapperType.invoke(damageTypeWrapper));
            return registry.getHolder(type)
                    .map(holder -> holder.is(this.magicTag))
                    .orElse(false);
        }

        @SuppressWarnings("unchecked")
        private static TagKey<DamageType> castTagKey(Object value) {
            return (TagKey<DamageType>) value;
        }

        @SuppressWarnings("unchecked")
        private static Registry<DamageType> castDamageTypeRegistry(Object value) {
            return (Registry<DamageType>) value;
        }

        @SuppressWarnings("unchecked")
        private static ResourceKey<DamageType> castDamageTypeKey(Object value) {
            return (ResourceKey<DamageType>) value;
        }
    }
}
