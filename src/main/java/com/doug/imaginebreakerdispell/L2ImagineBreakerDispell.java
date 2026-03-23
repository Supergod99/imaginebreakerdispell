package com.doug.imaginebreakerdispell;

import com.doug.imaginebreakerdispell.compat.L2HostilityCompat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(L2ImagineBreakerDispell.MODID)
public final class L2ImagineBreakerDispell {
    public static final String MODID = "l2_imaginebreakerdispell";

    public L2ImagineBreakerDispell() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(L2HostilityCompat::init);
    }
}
