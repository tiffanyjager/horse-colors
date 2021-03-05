package sekelsta.horse_colors.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.AbstractChestedHorseEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import sekelsta.horse_colors.CreativeTab;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.client.renderer.HorseGeneticRenderer;

@Mod.EventBusSubscriber(modid = HorseColors.MODID, bus = Bus.MOD)
public class ModEntities {
    public static EntityType<HorseGeneticEntity> HORSE_GENETIC = null;
    public static EntityType<DonkeyGeneticEntity> DONKEY_GENETIC = null;
    public static EntityType<MuleGeneticEntity> MULE_GENETIC = null;
    public static SpawnEggItem HORSE_SPAWN_EGG = null;
    public static SpawnEggItem DONKEY_SPAWN_EGG = null;
    public static SpawnEggItem MULE_SPAWN_EGG = null;

    public static final int horseEggPrimary = 0x7F4320;
    public static final int horseEggSecondary = 0x110E0D;
    static
    {
        // Default tracker is fine, or could use .tracker(64, 2, false)
        final ResourceLocation horseRegistryName = new ResourceLocation(HorseColors.MODID, "horse_felinoid");
        // Vanilla size in 1.12 was 1.3964844F, 1.6F
        // Size numbers are width, height
        HORSE_GENETIC = EntityType.Builder.of(HorseGeneticEntity::new, EntityClassification.CREATURE).sized(1.2F, 1.6F).build(horseRegistryName.toString());
        HORSE_GENETIC.setRegistryName(horseRegistryName);

        final ResourceLocation donkeyRegistryName = new ResourceLocation(HorseColors.MODID, "donkey");
        DONKEY_GENETIC = EntityType.Builder.of(DonkeyGeneticEntity::new, EntityClassification.CREATURE).sized(1.2F, 1.6F).build(donkeyRegistryName.toString());
        DONKEY_GENETIC.setRegistryName(donkeyRegistryName);

        final ResourceLocation muleRegistryName = new ResourceLocation(HorseColors.MODID, "mule");
        MULE_GENETIC = EntityType.Builder.of(MuleGeneticEntity::new, EntityClassification.CREATURE).sized(1.2F, 1.6F).build(muleRegistryName.toString());
        MULE_GENETIC.setRegistryName(muleRegistryName);

        HORSE_SPAWN_EGG = new SpawnEggItem(HORSE_GENETIC, horseEggPrimary, horseEggSecondary, (new Item.Properties()).tab(CreativeTab.instance));
        HORSE_SPAWN_EGG.setRegistryName(new ResourceLocation(HorseColors.MODID, "horse_spawn_egg"));

        DONKEY_SPAWN_EGG = new SpawnEggItem(DONKEY_GENETIC, 0x726457, 0xcdc0b5, (new Item.Properties()).tab(CreativeTab.instance));
        DONKEY_SPAWN_EGG.setRegistryName(new ResourceLocation(HorseColors.MODID, "donkey_spawn_egg"));

        MULE_SPAWN_EGG = new SpawnEggItem(MULE_GENETIC, 0x4b3a30, 0xcdb9a8, (new Item.Properties()).tab(CreativeTab.instance));
        MULE_SPAWN_EGG.setRegistryName(new ResourceLocation(HorseColors.MODID, "mule_spawn_egg"));
    }

    private static void registerAttributes() {
        GlobalEntityTypeAttributes.put(HORSE_GENETIC, AbstractHorseEntity.createBaseHorseAttributes().build());
        GlobalEntityTypeAttributes.put(DONKEY_GENETIC, AbstractChestedHorseEntity.createBaseChestedHorseAttributes().build());
        GlobalEntityTypeAttributes.put(MULE_GENETIC, AbstractChestedHorseEntity.createBaseChestedHorseAttributes().build());
    }

    @SubscribeEvent
    public static void registerEntities(final RegistryEvent.Register<EntityType<?>> event) {

        event.getRegistry().registerAll(
                HORSE_GENETIC/*,
                DONKEY_GENETIC,
                MULE_GENETIC*/
        );
        registerAttributes();
    }

    @SubscribeEvent
    public static void registerSpawnEggs(RegistryEvent.Register<Item> event) {

        event.getRegistry().register(HORSE_SPAWN_EGG);
        //event.getRegistry().register(DONKEY_SPAWN_EGG);
        //event.getRegistry().register(MULE_SPAWN_EGG);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerRenders()
    {
        RenderingRegistry.registerEntityRenderingHandler(HORSE_GENETIC, renderManager -> new HorseGeneticRenderer(renderManager));
        RenderingRegistry.registerEntityRenderingHandler(DONKEY_GENETIC, renderManager -> new HorseGeneticRenderer(renderManager));
        RenderingRegistry.registerEntityRenderingHandler(MULE_GENETIC, renderManager -> new HorseGeneticRenderer(renderManager));
    }

}
