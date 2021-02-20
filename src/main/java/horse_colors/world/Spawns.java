package sekelsta.horse_colors.world;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.gen.feature.jigsaw.*;
import net.minecraft.world.gen.feature.structure.PlainsVillagePools;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.List;

import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.ModEntities;
import sekelsta.horse_colors.HorseColors;

@Mod.EventBusSubscriber(modid = HorseColors.MODID)
public class Spawns {

    @SubscribeEvent
    // This is not registered to the event queue by the Mod.EventBusSubscriber 
    // annotation because that only registers Forge events, not mod lifecycle 
    // events. Instead it is subscribed from the HorseColors class.
    public static void onLoadComplete(net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent e) {
        // This needs to happen after the config is read
        changeVillageAnimals();
    }

    // As the documentation to BiomeLoadingEvent describes, adding entity
    // spawns should use HIGH priority
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void addBiomeSpawns(BiomeLoadingEvent event) {
        int horsePlainsWeight = (int)Math.round(5 * HorseConfig.SPAWN.horseSpawnMultiplier.get());
        Spawners horsePlainsSpawner = new Spawners(ModEntities.HORSE_GENETIC, horsePlainsWeight, 2, 6);
        int horseSavannaWeight = (int)Math.round(1 * HorseConfig.SPAWN.horseSpawnMultiplier.get());
        Spawners horseSavannaSpawner = new Spawners(ModEntities.HORSE_GENETIC, horseSavannaWeight, 2, 6);
        //int donkeyWeight = (int)Math.round(1 * HorseConfig.SPAWN.donkeySpawnMultiplier.get());
        // It seems 1.16.2 has increased donkey max herd size in savannas from 1 to 3, to match the plains
        //Spawners donkeySpawner = new Spawners(ModEntities.DONKEY_GENETIC, donkeyWeight, 1, 3);

        // Add to the spawn list according to biome type
        List<Spawners> spawns = event.getSpawns().getSpawner(EntityClassification.CREATURE);
        if (event.getCategory() == Biome.Category.PLAINS) {
            spawns.add(horsePlainsSpawner);
            //spawns.add(donkeySpawner);
        }
        else if (event.getCategory() == Biome.Category.SAVANNA) {
            spawns.add(horseSavannaSpawner);
            //spawns.add(donkeySpawner);
        }
    }

    // And removing entity spawns should use NORMAL priority
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void removeBiomeSpawns(BiomeLoadingEvent event) {
        List<Spawners> entriesToRemove = new ArrayList<>();
        List<Spawners> originalSpawns = event.getSpawns().getSpawner(EntityClassification.CREATURE);
        for (Spawners entry : originalSpawns) {
            if (entry.type == EntityType.HORSE && HorseConfig.SPAWN.blockVanillaHorseSpawns.get()) {
                HorseColors.logger.debug("Removing vanilla horse spawn: " + entry);
                entriesToRemove.add(entry);
            }/*
            else if (entry.type == EntityType.DONKEY && HorseConfig.SPAWN.blockVanillaDonkeySpawns.get()) {
                HorseColors.logger.debug("Removing vanilla donkey spawn: " + entry);
                entriesToRemove.add(entry);
            }*/
        }

        for (Spawners entry : entriesToRemove) {
            originalSpawns.remove(entry);
        }
    }

    private static boolean isVanillaVillageHorsePiece(SingleJigsawPiece piece) {
        return piece.toString().contains("minecraft:village/common/animals/horses");
    }

    private static boolean keepJigsawPair(Pair<JigsawPiece, Integer> pair) {
        if (!HorseConfig.SPAWN.blockVanillaHorseSpawns.get()) {
            return true;
        }
        JigsawPiece piece = pair.getFirst();
        if (piece instanceof SingleJigsawPiece) {
            return !isVanillaVillageHorsePiece((SingleJigsawPiece)piece);
        }
        // This code normally won't be reached
        return true;
    }

    public static void changeVillageAnimals() {
        // Force the static block to run
        PlainsVillagePools.bootstrap();
        HorseColors.logger.debug("Modifying village animal spawns");
        ResourceLocation animalsLoc = new ResourceLocation("village/common/animals");
        java.util.Optional<JigsawPattern> animalsOpt = WorldGenRegistries.TEMPLATE_POOL.getOptional(animalsLoc);
        if (!animalsOpt.isPresent()) {
            System.err.println("Trying to overwrite village spawns too soon");
            return;
        }
        JigsawPattern animals = animalsOpt.get();
        List<Pair<JigsawPiece, Integer>> vanillaList =  ObfuscationReflectionHelper.getPrivateValue(JigsawPattern.class, animals, "field_214952_d");
        List<Pair<JigsawPiece, Integer>> keeperList = new ArrayList<>();
        for (Pair<JigsawPiece, Integer> p : vanillaList) {
            if (keepJigsawPair(p)) {
                keeperList.add(p);
            }
        }
        // Add my own pieces
        List<Pair<Function<JigsawPattern.PlacementBehaviour, ? extends JigsawPiece>, Integer>> customPieces = new ArrayList<>();
        String modloc = HorseColors.MODID + ":";
        customPieces.add(new Pair<>(JigsawPiece.legacy(modloc + "village/common/animals/horses_1"), 1));
        customPieces.add(new Pair<>(JigsawPiece.legacy(modloc + "village/common/animals/horses_2"), 1));
        customPieces.add(new Pair<>(JigsawPiece.legacy(modloc + "village/common/animals/horses_3"), 1));
        customPieces.add(new Pair<>(JigsawPiece.legacy(modloc + "village/common/animals/horses_4"), 1));
        customPieces.add(new Pair<>(JigsawPiece.legacy(modloc + "village/common/animals/horses_5"), 1));
        // Transform from function to jigsaw piece
        JigsawPattern.PlacementBehaviour placementBehaviour = JigsawPattern.PlacementBehaviour.RIGID;
        for(Pair<Function<JigsawPattern.PlacementBehaviour, ? extends JigsawPiece>, Integer> pair : customPieces) {
            JigsawPiece jigsawPiece = pair.getFirst().apply(placementBehaviour);
            keeperList.add(new Pair<>(jigsawPiece, pair.getSecond()));
        }
        // 1.16 moved jigsawManager.REGISTRY to JigsawPatternRegistry
        JigsawPatternRegistry.register(new JigsawPattern(animalsLoc, new ResourceLocation("empty"), keeperList));
        // I don't touch sheep so I can leave "village/common/sheep" alone
        // Likewise for "village/common/cats"
        // Also ignore the cows, pigs, and sheep in "village/common/butcher_animals"
    }
}
