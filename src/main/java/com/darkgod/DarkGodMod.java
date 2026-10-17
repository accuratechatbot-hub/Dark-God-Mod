package com.darkgod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DarkGodMod.MODID)
public class DarkGodMod {
    public static final String MODID = "darkgod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DarkGodMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DarkGodConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(DarkGodCommand::onRegisterCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DarkGodNet::register);
    }
}
