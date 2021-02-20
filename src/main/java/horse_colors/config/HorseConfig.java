package sekelsta.horse_colors.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

import java.util.List;
import java.util.*;

public class HorseConfig
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final Common COMMON = new Common(BUILDER);
    public static final Growth GROWTH = new Growth(BUILDER);
    public static final Breeding BREEDING = new Breeding(BUILDER);
    public static final Spawn SPAWN = new Spawn(BUILDER);
    public static final Genetics GENETICS = new Genetics(BUILDER);

    public static class Common {
        public static BooleanValue horseDebugInfo;
        public static BooleanValue enableGroundTie;
        public static BooleanValue autoEquipSaddle;
        public static BooleanValue spookyHorses;
        public static BooleanValue enableSizes;
        public static IntValue mountingTweaks;
        public static BooleanValue useGeneticAnimalsIcons;

        Common(final ForgeConfigSpec.Builder builder) {
            builder.comment("Common config settings")
                    .push("common");

            enableGroundTie = builder
                    .comment("If enabled, horses will not wander off if they are wearing a saddle.")
                    .translation("horse_colors.config.common.enableGroundTie")
                    .define("enableGroundTie", false);

            horseDebugInfo = builder
                    .comment("If enabled, debugging information will appear on the screen when the",
            "player is holding a stick in their left hand and looks at a horse.",
            "For most users, it is probably better to leave this as false.")
                    .translation("horse_colors.config.common.horseDebugInfo")
                    .define("horseDebugInfo", false);

            autoEquipSaddle = builder
                    .comment("If enabled, right clicking a horse while holding a saddle or horse armor", 
                             "will equip it (as long as the horse isn't already wearing something in that slot)",
                             "instead of opening the inventory.")
                    .define("autoEquipSaddle", true);

            spookyHorses = builder
                    .comment("If enabled, horses will be spooked by monsters and throw their rider.")
                    .define("spookyHorses", false);

            enableSizes = builder
                    .comment("If enabled, horses can be different sizes.")
                    .define("enableSizes", true);

            mountingTweaks = builder
                    .comment("Allows ctrl + right click to mount the clicked creature on a horse the player",
                             "is riding or leading, or dismount all passengers of the clicked horse if it",
                             "is not being ridden by another player.",
                             "Setting to 0 disables this entirely.",
                             "Setting to 2 allows this to work on any peaceful creature, not just horses.",
                             "Defaults to 1.")
                    .defineInRange("mountingTweaks", 1, 0, 2);

            useGeneticAnimalsIcons = builder
                    .comment("Whether to use the gray themed gender icons from Genetic Animals (by mokiyoki,",
                             "used by permission) in place of the default pink and blue icons.")
                    .define("useGeneticAnimalsIcons", false);

            builder.pop();
        }
    }

    public static class Growth {
        public static DoubleValue yearLength;
        public static BooleanValue grayGradually;
        public static BooleanValue growGradually;
        public static DoubleValue growTime;

        Growth(final ForgeConfigSpec.Builder builder) {
            builder.comment("Config settings related to growth and aging")
                    .push("growth");

            yearLength = builder
                    .comment("How long a year lasts in twenty minute Minecraft days, for age-dependent colors such as gray.")
                    .defineInRange("yearLength", 4.0, 2/24000., 10000);

            grayGradually = builder
                    .comment("If enabled, gray horses will be born colored and their fur will gradually turn white.",
                    "Otherwise, all gray horses will render as if they were about 6-8 years old.")
                    .define("grayGradually", true);

            growGradually = builder
                    .comment("If enabled, foals will slowly get bigger as they grow into adults.")
                    .define("foalsGrowGradually", false);

            growTime = builder
                    .comment("The number of twenty minute Minecraft days that it takes for a foal to become an adult.")
                    .defineInRange("growTime", 1.0, 2/24000., 10000);

            builder.pop();
        }

        public int getMinAge() {
            return (int)(growTime.get() * -24000);
        }

        public int getMaxAge() {
            return (int)(15f * 24000 * yearLength.get());
        }
    }

    public static class Breeding {
        public static BooleanValue enableGenders;
        public static IntValue genderlessBreedingCooldown;
        public static IntValue maleBreedingCooldown;
        public static IntValue femaleBreedingCooldown;
        public static IntValue pregnancyLength;

        public Breeding(final ForgeConfigSpec.Builder builder) {
            builder.comment("Config settings related to breeding and gender")
                    .push("breeding");

            enableGenders = builder
                    .comment("Enables or disables all features relating to gender.")
                    .define("enableGenders", false);

            genderlessBreedingCooldown = builder
                    .comment("The number of ticks until horses can breed again, when genders are disabled.",
                            "The vanilla value is 6000 (or at 20 ticks per second, 5 minutes,",
                            "or at 24000 ticks per minecraft day, 1/4 day)")
                    .defineInRange("genderlessBreedingCooldown", 6000, 0, Integer.MAX_VALUE);

            maleBreedingCooldown = builder
                    .comment("The number of ticks until male horses can breed again.",
                            "The default value is 240 ticks (12 seconds).")
                    .defineInRange("maleBreedingCooldown", 240, 0, Integer.MAX_VALUE);

            femaleBreedingCooldown = builder
                    .comment("The number of ticks until female horses can breed again.",
                            "The default value is 24000 ticks (20 minutes, or 1 minecraft day).",
                            "This must always be at least as long as pregnancyLength.")
                    .defineInRange("femaleBreedingCooldown", 24000, 0, Integer.MAX_VALUE);

            pregnancyLength = builder
                    .comment("If genders are enabled, females will be pregnant for this many ticks.",
                            "The default value is 24000 ticks (20 minutes, or 1 minecraft day).",
                            "To disable pregnancy altogether, set this number to 0.",
                            "Lowering this will not let female horses breed again sooner unless you",
                            "also lower femaleRebreedTicks")
                    .defineInRange("pregnancyLength", 24000, 0, Integer.MAX_VALUE);

            builder.pop();
        }
    }

    public static class Genetics {
        public static BooleanValue useGeneticStats;
        public static BooleanValue enableHealthEffects;
        public static DoubleValue mutationChance;
        public static BooleanValue bookShowsGenes;
        public static BooleanValue bookShowsTraits;

        Genetics(final ForgeConfigSpec.Builder builder) {
            builder.comment("Config settings for genetics")
                    .push("genetics");

            useGeneticStats = builder
                    .comment("If enabled, horses' speed, jump, and health will be determined",
            "through genetics instead of the vanilla Minecraft way")
                    .translation("horse_colors.config.common.useGeneticStats")
                    .define("useGeneticStats", true);

            enableHealthEffects = builder
                    .comment("If enabled, certain genes will have a small impact on health,",
                             "as they do in real life. This config option does not affect Overo",
                             "Lethal White Syndrome.")
                    .translation("horse_colors.config.common.enableHealthEffects")
                    .define("enableHealthEffects", true);

            mutationChance = builder
                    .comment("The chance for each allele to mutate. The recommended range",
                             "is between 0.0001 and 0.01.",
                              "To disable mutations, set this value to 0.")
                    .defineInRange("mutationChance", 0.0001, 0.0, 1.0);

            bookShowsGenes = builder
                    .comment("Enable or disable genetic testing.")
                    .define("bookShowsGenes", true);

            bookShowsTraits = builder
                    .comment("Enable or disable physical inspection (rough information about health, ",
                            "speed, and jump).")
                    .define("bookShowsTraits", true);

            builder.pop();
        }
    }

    public static class Spawn {
        public static DoubleValue horseSpawnMultiplier;
        public static DoubleValue donkeySpawnMultiplier;
        public static BooleanValue blockVanillaHorseSpawns;
        public static BooleanValue blockVanillaDonkeySpawns;
        public static BooleanValue convertVanillaHorses;

        Spawn(final ForgeConfigSpec.Builder builder) {
            builder.comment("Settings to configure spawning")
                   .push("spawn");
            horseSpawnMultiplier = builder
                    .comment("Larger numbers make horses more common, smaller numbers make them less common.",
                             "1.0 makes them as common as in vanilla.")
                    .defineInRange("horseSpawnMultiplier", 1.0, 0.0, 1000.0);
/*
            donkeySpawnMultiplier = builder
                    .comment("Larger numbers make donkeys more common, smaller numbers make them less common.",
                             "1.0 makes them as common as in vanilla.")
                    .defineInRange("donkeySpawnMultiplier", 1.0, 0.0, 1000.0);*/

            blockVanillaHorseSpawns =  builder
                    .comment("Whether to allow new vanilla horses to spawn. This will not affect any",
                             "horses that already exist.")
                    .define("blockVanillaHorseSpawns", true);
/*
            blockVanillaDonkeySpawns =  builder
                    .comment("Whether to allow new vanilla donkeys to spawn. This will not affect any",
                             "horses that already exist.")
                    .define("blockVanillaDonkeySpawns", true);*/

            convertVanillaHorses = builder
                    .comment("If this is set to true, existing horses will be turned into horses with genetics")
                    .define("convertVanillaHorses", false);

            builder.pop();
        }

    }

    public static final ForgeConfigSpec spec = BUILDER.build();

    public static boolean isGenderEnabled() {
        return BREEDING.enableGenders.get();
    }

    public static int getHorseRebreedTicks(boolean isMale) {
        if (!isGenderEnabled()) {
            return BREEDING.genderlessBreedingCooldown.get();
        }
        if (isMale) {
            return BREEDING.maleBreedingCooldown.get();
        }
        return Math.max(BREEDING.femaleBreedingCooldown.get(), getHorsePregnancyLength());
    }

    public static int getHorseBirthAge() {
        return GROWTH.getMinAge();
    }

    public static boolean isPregnancyEnabled() {
        return isGenderEnabled();
    }

    public static int getHorsePregnancyLength() {
        return BREEDING.pregnancyLength.get();
    }
}
