package sekelsta.horse_colors;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sekelsta.horse_colors.breed.BreedManager;
import sekelsta.horse_colors.client.HorseGui;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.ModEntities;
import sekelsta.horse_colors.client.ClientEventHandler;
import sekelsta.horse_colors.item.ModItems;
import sekelsta.horse_colors.network.HorseColorsPacketHandler;
import sekelsta.horse_colors.world.EntityReplacer;
import sekelsta.horse_colors.world.Spawns;

@Mod(HorseColors.MODID)
public class HorseColors
{
    public static final String MODID = "horse_colors";

    public static HorseColors instance;

    public static Logger logger = LogManager.getLogger(MODID);

    public HorseColors()
    {
        instance = this;

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Spawns::onLoadComplete);
        MinecraftForge.EVENT_BUS.addListener(ContainerEventHandler::editContainer);
        MinecraftForge.EVENT_BUS.register(EntityReplacer.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HorseConfig.spec);
        HorseColorsPacketHandler.registerPackets();
        MinecraftForge.EVENT_BUS.addListener(BreedManager::addReloadListener);
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        MinecraftForge.EVENT_BUS.addListener(HorseGui::replaceGui);
        MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::handleInteract);
        ModEntities.registerRenders();
    }
}
