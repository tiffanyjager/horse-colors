package sekelsta.horse_colors.entity;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.horse.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BookItem;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import sekelsta.horse_colors.breed.*;
import sekelsta.horse_colors.config.HorseConfig;
import sekelsta.horse_colors.entity.ai.*;
import sekelsta.horse_colors.entity.genetics.*;
import sekelsta.horse_colors.entity.genetics.HorseGenome.Gene;
import sekelsta.horse_colors.HorseColors;
import sekelsta.horse_colors.item.ModItems;
import sekelsta.horse_colors.item.GeneBookItem;
import sekelsta.horse_colors.util.Util;

public abstract class AbstractHorseGenetic extends AbstractChestedHorseEntity implements IGeneticEntity {
    protected HorseGenome genes = new HorseGenome(this.getSpecies(), this);
    protected static final DataParameter<String> GENES = EntityDataManager.<String>defineId(AbstractHorseGenetic.class, DataSerializers.STRING);

    protected static final DataParameter<Integer> HORSE_RANDOM = EntityDataManager.<Integer>defineId(AbstractHorseGenetic.class, DataSerializers.INT);
    protected static final DataParameter<Integer> DISPLAY_AGE = EntityDataManager.<Integer>defineId(AbstractHorseGenetic.class, DataSerializers.INT);
    protected static final DataParameter<Boolean> GENDER = EntityDataManager.<Boolean>defineId(AbstractHorseGenetic.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Integer> PREGNANT_SINCE = EntityDataManager.<Integer>defineId(AbstractHorseGenetic.class, DataSerializers.INT);
    protected static final DataParameter<Float> MOTHER_SIZE = EntityDataManager.<Float>defineId(AbstractHorseGenetic.class, DataSerializers.FLOAT);
    protected int trueAge;

    protected static final UUID CSNB_SPEED_UUID = UUID.fromString("84ca527a-5c70-4336-a737-ae3f6d40ef45");
    protected static final UUID CSNB_JUMP_UUID = UUID.fromString("72323326-888b-4e46-bf52-f669600642f7");
    protected static final AttributeModifier CSNB_SPEED_MODIFIER = new AttributeModifier(CSNB_SPEED_UUID, "CSNB speed penalty", -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL);
    protected static final AttributeModifier CSNB_JUMP_MODIFIER = new AttributeModifier(CSNB_JUMP_UUID, "CSNB jump penalty", -0.6, AttributeModifier.Operation.MULTIPLY_TOTAL);

    protected static final int HORSE_GENETICS_VERSION = 3;

    protected List<AbstractHorseGenetic> unbornChildren = new ArrayList<>();

    // field_110282_bM = standAnim0
    private Field rearingAmountField = ObfuscationReflectionHelper.findField(AbstractHorseEntity.class, "field_110282_bM");

    public AbstractHorseGenetic(EntityType<? extends AbstractHorseGenetic> entityType, World worldIn)
    {
        super(entityType, worldIn);
        this.setSeed(this.random.nextInt());
        this.setMale(this.random.nextBoolean());
        this.entityData.set(PREGNANT_SINCE, -1);
        // Trying to do this in writeAdditional would be too late, as the
        // persistent data is already written from Entity.write before that is
        // called (at least in Minecraft 1.16.3)
        this.getPersistentData().putInt("HorseGeneticsVersion", HORSE_GENETICS_VERSION);
    }

    public HorseGenome getGenome() {
        return genes;
    }

    public abstract boolean fluffyTail();
    public abstract boolean longEars();
    public abstract boolean thinMane();
    public abstract Species getSpecies();

    public boolean canEquipChest() {
        return true;
    }

    @Override
    public int getSeed() {
        return this.entityData.get(HORSE_RANDOM).intValue();
    }

    @Override
    public void setSeed(int seed) {
        this.entityData.set(HORSE_RANDOM, seed);
    }

    @Override
    public Random getRand() {
        return super.getRandom();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        if (HorseConfig.COMMON.spookyHorses.get()) {
            this.goalSelector.addGoal(1, new SpookGoal(this, MonsterEntity.class, 8.0F, 1.5, 1.5));
        }
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorseEntity.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomWalkGroundTie(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.addBehaviourGoals();
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(GENES, "");
        this.entityData.define(HORSE_RANDOM, 0);
        this.entityData.define(DISPLAY_AGE, 0);
        this.entityData.define(GENDER, false);
        this.entityData.define(PREGNANT_SINCE, -1);
        this.entityData.define(MOTHER_SIZE, 1f);
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    @Override
    public void addAdditionalSaveData(CompoundNBT compound)
    {
        super.addAdditionalSaveData(compound);
        CompoundNBT genetic = new CompoundNBT();
        writeGeneticData(genetic);
        compound.put("HorseGeneticsData", genetic);
    }

    private void writeGeneticData(CompoundNBT compound) {
        compound.putString("Genes", this.getGenome().getBase64());
        compound.putInt("Random", this.getSeed());
        compound.putInt("true_age", trueAge);
        compound.putBoolean("gender", this.isMale());
        compound.putInt("pregnant_since", this.getPregnancyStart());
        if (this.unbornChildren != null) {
            ListNBT unbornChildrenTag = new ListNBT();
            for (AbstractHorseGenetic child : this.unbornChildren) {
                CompoundNBT childNBT = new CompoundNBT();
                childNBT.putString("species", child.getSpecies().toString());
                childNBT.putString("genes", child.getGenome().genesToString());
                unbornChildrenTag.add(childNBT);
            }
            compound.put("unborn_children", unbornChildrenTag);
        }   
        compound.putFloat("mother_size", this.getMotherSize());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound)
    {
        super.readAdditionalSaveData(compound);
        // Get save format version
        int version = 0;
        if (this.getPersistentData().contains("HorseGeneticsVersion")) {
            version = this.getPersistentData().getInt("HorseGeneticsVersion");
        }
        // Read the main part of the data
        if (version < 3) {
            readGeneticData(compound, version);
        }
        else {
            readGeneticData(compound.getCompound("HorseGeneticsData"), version);
        }
        // Ensure the true age matches the age
        if (this.trueAge < 0 != this.age < 0) {
            this.trueAge = this.age;
        }

        // Set any genes that were specified in a human-readable format
        readExtraGenes(compound);

        this.useGeneticAttributes();
        this.updateContainerEquipment();

        if (this instanceof HorseGeneticEntity) {
            int spawndata = compound.getInt("VillageSpawn");
            if (spawndata != 0) {
                this.initFromVillageSpawn();
            }
        }

        // Done reading, so update format version to the one that will be written
        this.getPersistentData().putInt("HorseGeneticsVersion", HORSE_GENETICS_VERSION);
    }


    // A helper function for reading the data either from the main entity
    // tag in save format versions 2 and under, or for reading it from its own tag
    // in versions 3+
    private void readGeneticData(CompoundNBT compound, int version) {
        // Set genes if they exist
        if (compound.contains("Genes")) {
            if (version < 3) {
                // Read genes using the 1:1 conversion of chars to nums
                this.setGeneData(compound.getString("Genes"));
            }
            else {
                this.getGenome().setFromBase64(compound.getString("Genes"));
            }
        }
        // Otherwise, use a breed for a base if given one
        else if (compound.contains("Breed")) {
            this.randomize(getBreed(compound.getString("Breed")));
        }
        else {
            // If we haven't been given color data, we should randomize
            // anything not specified
            if (!compound.contains("Variant")
                    && !compound.contains("Variant2")
                    && !compound.contains("Variant3")
                    && !compound.contains("Variant4")) {
                randomize(getRandomBreed());
            }
            readLegacyAdditional(compound);
        }

        // Replace saddle reading functionality from AbstractHorseEntity with
        // one that accepts alternate saddles
        if (compound.contains("SaddleItem", 10)) {
            ItemStack itemstack = ItemStack.of(compound.getCompound("SaddleItem"));
            if (isSaddle(itemstack)) {
                this.inventory.setItem(0, itemstack);
            }
        }

        this.setSeed(compound.getInt("Random"));
        this.trueAge = compound.getInt("true_age");
        if (compound.contains("gender")) {
            this.setMale(compound.getBoolean("gender"));
        }
        else {
            this.setMale(this.random.nextBoolean());
        }

        int pregnantSince = -1;
        if (compound.contains("pregnant_since")) {
            pregnantSince = compound.getInt("pregnant_since");
        }
        this.entityData.set(PREGNANT_SINCE, pregnantSince);
        if (compound.contains("unborn_children")) {
            INBT nbt = compound.get("unborn_children");
            if (nbt instanceof ListNBT) {
                ListNBT childListTag = (ListNBT)nbt;
                for (int i = 0; i < childListTag.size(); ++i) {
                    INBT cnbt = childListTag.get(i);
                    if (!(cnbt instanceof CompoundNBT)) {
                        continue;
                    }
                    CompoundNBT childNBT = (CompoundNBT)cnbt;
                    Species species = Species.valueOf(childNBT.getString("species"));
                    AbstractHorseGenetic child = null;
                    switch(species) {
                        case HORSE:
                            child = ModEntities.HORSE_GENETIC.create(this.level);
                            break;
                        case DONKEY:
                            child = ModEntities.DONKEY_GENETIC.create(this.level);
                            break;
                        case MULE:
                        case HINNY:
                            child = ModEntities.MULE_GENETIC.create(this.level);
                            ((MuleGeneticEntity)child).setSpecies(species);
                            break;
                    }
                    if (child != null) {
                        HorseGenome genome = new HorseGenome(child.getSpecies(), child);
                        genome.genesFromString(childNBT.getString("genes"));
                        this.unbornChildren.add(child);
                    }
                }
            }
        }

        float motherSize = 1f;
        if (compound.contains("mother_size")) {
            motherSize = compound.getFloat("mother_size");
        }
        setMotherSize(motherSize);
    }

    private void readExtraGenes(CompoundNBT compound) {
        for (Enum gene : this.getGenome().listGenes()) {
            if (compound.contains(gene.toString())) {
                int alleles[] = compound.getIntArray(gene.toString());
                getGenome().setAllele(gene, 0, alleles[0]);
                getGenome().setAllele(gene, 1, alleles[1]);
            }
        }
    }

    public void readLegacyAdditional(CompoundNBT compound) {
        Map<String, Integer> map = new HashMap<>();
        if (compound.contains("Variant")) {
            map.put("0", compound.getInt("Variant"));
        }
        if (compound.contains("Variant2")) {
            map.put("1", compound.getInt("Variant2"));
        }
        if (compound.contains("Variant3")) {
            map.put("2", compound.getInt("Variant3"));
        }
        if (compound.contains("Variant4")) {
            map.put("3", compound.getInt("Variant4"));
        }
        if (compound.contains("Variant5")) {
            map.put("4", compound.getInt("Variant5"));
        }
        else if (!this.getPersistentData().contains("HorseGeneticsVersion")) {
            this.getPersistentData().putInt("HorseGeneticsVersion", HORSE_GENETICS_VERSION);
            this.getGenome().datafixAddingFourthChromosome(map);
        }
        if (compound.contains("SpeedGenes")) {
            map.put("speed", compound.getInt("SpeedGenes"));
        }
        if (compound.contains("JumpGenes")) {
            map.put("jump", compound.getInt("JumpGenes"));
        }
        if (compound.contains("HealthGenes")) {
            map.put("health", compound.getInt("HealthGenes"));
        }
        if (compound.contains("MHC1")) {
            map.put("mhc1", compound.getInt("MHC1"));
            map.put("mhc2", compound.getInt("MHC2"));
        }
        if (compound.contains("Immune")) {
            map.put("immune", compound.getInt("Immune"));
        }
        this.genes.setLegacyGenes(map);
    }

    public int getDisplayAge() {
        return this.entityData.get(DISPLAY_AGE);
    }

    public void setDisplayAge(int age) {
        this.entityData.set(DISPLAY_AGE, age);
    }

    @Override
    public int getTrueAge() {
        if (isBaby() && getDisplayAge() >= 0) {
            return getBirthAge();
        }
        return getDisplayAge();
    }

    public void setGeneData(String genes) {
        this.entityData.set(GENES, genes);
    }

    public String getGeneData() {
        return (String)this.entityData.get(GENES);
    }

    public void setMotherSize(float size) {
        this.entityData.set(MOTHER_SIZE, size);
    }

    public float getMotherSize() {
        return ((Float)this.entityData.get(MOTHER_SIZE)).floatValue();
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        if (GENES.equals(key)) {
            this.getGenome().resetTexture();
            this.useGeneticAttributes();
            this.refreshDimensions();
        }
        else if (HORSE_RANDOM.equals(key)
            || GENDER.equals(key)
            || MOTHER_SIZE.equals(key)) {
            this.refreshDimensions();
        }

        super.onSyncedDataUpdated(key);
    }

    @Override
    public boolean isMale() {
        return ((Boolean)this.entityData.get(GENDER)).booleanValue();
    }

    @Override
    public void setMale(boolean gender) {
        if (gender) {
            // Prepare to become male
            this.unbornChildren = new ArrayList<>();
            this.entityData.set(PREGNANT_SINCE, -1);
        }
        this.entityData.set(GENDER, gender);
    }

    public boolean isPregnant() {
        return this.getPregnancyStart() >= 0;
    }

    public int getPregnancyStart() {
        return this.entityData.get(PREGNANT_SINCE);
    }

    public float getPregnancyProgress() {
        int passed = getDisplayAge() - getPregnancyStart();
        int total = HorseConfig.getHorsePregnancyLength();
        return (float)passed / (float)total;
    }

    public int getRebreedTicks() {
        return HorseConfig.getHorseRebreedTicks(this.isMale());
    }

    public int getBirthAge() {
        return HorseConfig.getHorseBirthAge();
    }

    // Since miniaure horses are too small to ride, they can't be tamed the usual way
    @Override
    public boolean isTamed() {
        return getGenome().isMiniature() || super.isTamed();
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        // Don't stop and rear in response to suffocation or cactus damage
        if (damageSourceIn != DamageSource.IN_WALL && damageSourceIn != DamageSource.CACTUS) {
            // Chance to rear up
            super.getHurtSound(damageSourceIn);
        }
        return null;
    }

    /**
     * Set whether this creature is a child.
     */
    @Override
    public void setBaby(boolean isBaby) {
        this.setAge(isBaby ? this.getBirthAge() : 0);
    }

    private double getRiderWeight(Entity rider) {
        double weight = rider.getBoundingBox().getXsize()
                            * rider.getBoundingBox().getYsize()
                            * rider.getBoundingBox().getZsize();
        if (rider instanceof AnimalEntity) {
            weight *= 2;
        }
        // Adjust to avoid baby villagers being interpreted as weighing 19 pounds
        if (rider instanceof AgeableEntity
                && ((AgeableEntity)rider).isBaby()) {
            weight *= 2;
        }
        return weight;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        // Riders must be peaceful
        if (!passenger.getType().getCategory().isFriendly()) {
            return false;
        }
        // Ignore the size stuff if its disabled
        if (!HorseConfig.COMMON.enableSizes.get()) {
            return super.canAddPassenger(passenger);
        }
        // Max two riders
        if (this.getPassengers().size() >= 2) {
            return false;
        }
        // Calculate size of current passengers
        double riderweight = 0;
        for (Entity rider : this.getPassengers()) {
            riderweight += getRiderWeight(rider);
        }
        // Calculate size of mounting entity
        double weight = getRiderWeight(passenger);
        // The player's hitbox is 0.6 * 0.6 * 1.8, so this will almost exactly allow
        // them to ride any horse heavier than the miniature cutoff
        return riderweight + weight < 0.648001 
            * this.getGenome().getGeneticWeightKg() / HorseGenome.MINIATURE_CUTOFF;
    }

    public Inventory getHorseChest() {
        return this.inventory;
    }

    private boolean itemInteract(PlayerEntity player, ItemStack itemstack, Hand hand) {
        // Enter genetic test results
        if (itemstack.getItem() == Items.BOOK
                && (HorseConfig.GENETICS.bookShowsGenes.get()
                    || HorseConfig.GENETICS.bookShowsTraits.get())
                && (this.isTamed() || player.abilities.instabuild)) {
            ItemStack book = new ItemStack(ModItems.geneBookItem);
            if (book.getTag() == null) {
                book.setTag(new CompoundNBT());
            }
            book.getTag().putString("species", this.getSpecies().name());
            book.getTag().putString("genes", this.getGenome().genesToString());
            book.getTag().putUUID("EntityUUID", this.getUUID());
            if (this.hasCustomName()) {
                book.setHoverName(this.getCustomName());
            }
            if (!player.addItem(book)) {
                this.spawnAtLocation(book);
            }
            if (!player.abilities.instabuild) {
                itemstack.shrink(1);
            }
            return true;
        }
        // Only allow taming with an empty hand
        if (!this.isTamed()) {
            this.makeMad();
            return true;
        }
        // If tame, equip chest
        if (!this.hasChest() && itemstack.getItem() == Blocks.CHEST.asItem()
                && this.canEquipChest()) {
            this.setChest(true);
            this.playChestEquipsSound();
            if (!player.abilities.instabuild) {
                itemstack.shrink(1);
            }

            this.createInventory();
            return true;
        }
        // If tame, equip saddle
        if (!this.isSaddled() && isSaddle(itemstack) && this.isSaddleable()) {
             if (HorseConfig.COMMON.autoEquipSaddle.get()) {
                if (!this.level.isClientSide) {
                    ItemStack saddle = itemstack.split(1);
                    this.inventory.setItem(0, saddle);
                }
            }
            else {
                this.openInventory(player);
            }
            return true;
        }
        // If tame, equip armor
        if (this.isArmor(itemstack) && this.canWearArmor()) {
             if (HorseConfig.COMMON.autoEquipSaddle.get() && this.inventory.getItem(1).isEmpty()) {
                if (!this.level.isClientSide) {
                    ItemStack armor = itemstack.split(1);
                    this.inventory.setItem(1, armor);
                }
            }
            else {
                this.openInventory(player);
            }
            return true;
        }
        // Nothing left
        return false;
    }

    @Override
    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!this.isBaby()) {
            if (this.isTamed() && player.isSecondaryUseActive()) {
                this.openInventory(player);
                return ActionResultType.sidedSuccess(this.level.isClientSide);
            }
        }

        // Only interact items with horses that aren't being ridden by another player
        if (!itemstack.isEmpty() && !this.isVehicle()) {
            // Try to eat it
            if (this.isFood(itemstack)) {
                // Eat the item
                return this.fedFood(player, itemstack);
            }
            // See if the item interacts with us
            ActionResultType actionresulttype = itemstack.interactLivingEntity(player, this, hand);
            if (actionresulttype.consumesAction()) {
                return actionresulttype;
            }
            // See if we interact with the item
            if (itemInteract(player, itemstack, hand)) {
                return ActionResultType.sidedSuccess(this.level.isClientSide);
            }
        }

        if (!this.isBaby() && canAddPassenger(player)) {
            this.doPlayerRide(player);
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        // else
        return super.mobInteract(player, hand);
    }

    protected void useGeneticAttributes()
    {
        if (HorseConfig.GENETICS.useGeneticStats.get())
        {
            HorseGenome genes = this.getGenome();
            float maxHealth = this.getGenome().getHealth();
            float athletics = genes.sumGenes(Gene.class, "athletics", 0, 4) / 2f
                                + genes.sumGenes(Gene.class, "athletics", 4, 8) / 2f;
            // Vanilla horse speed ranges from 0.1125 to 0.3375, as does ours
            float speedStat = genes.sumGenes(Gene.class, "speed", 0, 4)
                                + genes.sumGenes(Gene.class, "speed", 4, 8)
                                + genes.sumGenes(Gene.class, "speed", 8, 12)
                                + athletics;
            double movementSpeed = 0.1125D + speedStat * (0.225D / 32.0D);
            // Vanilla horse jump strength ranges from 0.4 to 1.0, as does ours
            float jumpStat = genes.sumGenes(Gene.class, "jump", 0, 4)
                                + genes.sumGenes(Gene.class, "jump", 4, 8)
                                + genes.sumGenes(Gene.class, "jump", 8, 12)
                                + athletics;
            double jumpStrength = 0.4D + jumpStat * (0.6D / 32.0D);

            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jumpStrength);
        }
    }

    @Override
    public void spawnChildFromBreeding(ServerWorld world, AnimalEntity mate) {
        // If vanilla mate, handle the vanilla way
        if (!(mate instanceof IGeneticEntity)) {
            super.spawnChildFromBreeding(world, mate);
            return;
        }
        // If two animals go for the same mate at the same time this function
        // can be called twice. This check makes sure it only works the first time.
        if (!(this.isInLove() && mate.isInLove())) {
            return;
        }

        IGeneticEntity geneticMate = (IGeneticEntity)mate;

        // Call this on the female's side, if possible
        if (this.isMale() && !geneticMate.isMale()) {
            mate.spawnChildFromBreeding(world, this);
            return;
        }

        // For use later in triggering the achievement
        ServerPlayerEntity serverplayerentity = this.getLoveCause();
        if (serverplayerentity == null && mate.getLoveCause() != null) {
            serverplayerentity = mate.getLoveCause();
        }

        int numFoals = this.getRandomLitterSize();
        List<AgeableEntity> foals = new ArrayList<>();
        for (int i = 0; i < numFoals; ++i) {

            AgeableEntity ageableentity = this.getBreedOffspring(world, mate);
            // If ageableentity is null, leave this and the mate in love mode to try again
            // Note posting an event with a null child could cause crashes with other
            // mods that do not check their input carefully, so we don't do that
            if (ageableentity == null) {
                continue;
            }
            final BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this, mate, ageableentity);
            final boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
            ageableentity = event.getChild();
            // Don't spawn if cancelled or a null entity
            if (cancelled || ageableentity == null) {
                continue;
            }

            if (HorseConfig.isPregnancyEnabled()) {
                if (this.setPregnantWith(ageableentity, mate)) {
                    ageableentity = null;
                }
            }
            // Spawn if pregnancy is not enabled
            else {
                spawnChild(ageableentity, world);
            }
            foals.add(ageableentity);

            if (serverplayerentity != null) {
                serverplayerentity.awardStat(Stats.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity, this, mate, ageableentity);
            }
        }
        // Reset love state
        this.setAge(this.getRebreedTicks());
        mate.setAge(geneticMate.getRebreedTicks());
        this.resetLove();
        mate.resetLove();

        
        if (foals.size() <= 0) {
            // Spawn smoke particles to indicate failure
            this.level.broadcastEntityEvent(this, (byte)6);
            // Only spawn XP and grant the achievement for successful births
            return;
        }
        // Make twins and triplets smaller as there is less space for them in the womb
        float multiplier = (float)Math.pow(1./foals.size(), 1./3.);
        for (AgeableEntity foal : foals) {
            if (foal instanceof IGeneticEntity) {
                IGeneticEntity gFoal = (IGeneticEntity)foal;
                gFoal.setMotherSize(gFoal.getMotherSize() * multiplier);
            }
        }

        if (HorseConfig.isPregnancyEnabled()) {
            // Spawn heart particles
            this.level.broadcastEntityEvent(this, (byte)18);
        }

        // Spawn XP orbs
        if (world.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            int xp = this.getRandom().nextInt(7) + 1;
            world.addFreshEntity(new ExperienceOrbEntity(world, this.getX(), this.getY(), this.getZ(), xp));
        }
    }

    private void spawnChild(AgeableEntity child, ServerWorld world) {
        child.setBaby(true);
        child.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
        world.addFreshEntity(child);
        // Spawn heart particles
        world.broadcastEntityEvent(this, (byte)18);
    }

    // Helper function for createChild that creates and spawns an entity of the 
    // correct species
    abstract AbstractHorseEntity getChild(ServerWorld world, AgeableEntity otherparent);

    // Returns the number of conceptions that survive pregnancy
    // This can return different numbers when called on the same animal at
    // different times.
    protected int getRandomLitterSize() {
        // Since this is Minecraft, skip twin births where one or both twins die,
        // and make twin births much more rare
        // If the frequency of double_ovulation is 0.2, the probability of triplets
        // works out to within an order of magnitude of the expected 1 in 300,000.
        double chance = 1 / 10000;
        if (getGenome().countAlleles(Gene.double_ovulation, 1) == 1) {
            chance = 1 / 5000;
        }
        else if (getGenome().isHomozygous(Gene.double_ovulation, 1)) {
            chance = 1 / 1000;
        }

        int litterSize = 1;
        if (getRandom().nextDouble() < chance) {
            litterSize += 1;
        }
        if (getRandom().nextDouble() < chance) {
            litterSize += 1;
        }
        return litterSize;
    }

    public boolean isOppositeGender(AbstractHorseGenetic other) {
        if (!HorseConfig.isGenderEnabled()) {
            return true;
        }
        return this.isMale() != other.isMale();
    }

    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity ageable)
    {
        if (!(ageable instanceof AnimalEntity)) {
            return null;
        }
        AnimalEntity otherAnimal = (AnimalEntity)ageable;
        // Have the female create the child if possible
        if (this.isMale() 
                && ageable instanceof AbstractHorseGenetic
                && !((AbstractHorseGenetic)ageable).isMale()) {
            return ageable.getBreedOffspring(world, this);
        }
        AbstractHorseEntity child = this.getChild(world, ageable);
        if (child != null) {
            this.setOffspringAttributes(ageable, child);
        }
        if (child instanceof AbstractHorseGenetic) {
            AbstractHorseGenetic foal = (AbstractHorseGenetic)child;
            if (ageable instanceof AbstractHorseGenetic) {
                AbstractHorseGenetic other = (AbstractHorseGenetic)ageable;
                foal.getGenome().inheritGenes(this.getGenome(), other.getGenome());
            }
            // Dominant white is homozygous lethal early in pregnancy. No child
            // is born.
            if (foal.getGenome().isEmbryonicLethal())
            {
                return null;
            }
            foal.setMotherSize(this.getGenome().getAdultScale());
            foal.setMale(this.random.nextBoolean());
            foal.useGeneticAttributes();
            foal.setAge(HorseConfig.GROWTH.getMinAge());
        }
        return child;
    }

    @Override
    public boolean setPregnantWith(AgeableEntity child, AgeableEntity otherParent) {
        if (otherParent instanceof IGeneticEntity) {
            IGeneticEntity otherGenetic = (IGeneticEntity)otherParent;
            if (this.isMale() == otherGenetic.isMale()) {
                return false;
            }
            else if (this.isMale() && !otherGenetic.isMale()) {
                return otherGenetic.setPregnantWith(child, this);
            }
        }
        if (this.isMale()) {
            return false;
        }

        if (child instanceof AbstractHorseGenetic) {
            unbornChildren.add((AbstractHorseGenetic)child);
            if (!this.level.isClientSide) {
                // Can't be a child
                this.trueAge = Math.max(0, this.trueAge);
                this.entityData.set(PREGNANT_SINCE, this.trueAge);
            }
            return true;
        }
        return false;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick()
    {
        super.tick();
        // Keep track of age
        if (!this.level.isClientSide) {
            // For children, align with growing age in case they have been fed
            if (this.age < 0) {
                this.trueAge = this.age;
            }
            else {
                this.trueAge = Math.max(0, this.trueAge + 1);
            }
            // Allow imprecision
            final int c = 400;
            if (this.trueAge / c != this.getDisplayAge() / c
                    || (this.trueAge < 0 != this.getDisplayAge() < 0)) {
                this.setDisplayAge(this.trueAge);
            }
        }

        // Pregnancy
        if (!this.level.isClientSide && this.isPregnant()) {
            // Check pregnancy
            if (this.unbornChildren == null
                    || this.unbornChildren.size() == 0) {
                this.entityData.set(PREGNANT_SINCE, -1);
            }
            // Handle birth
            int totalLength = HorseConfig.getHorsePregnancyLength();
            int currentLength = this.trueAge - this.getPregnancyStart();
            if (currentLength >= totalLength) {
                for (AbstractHorseGenetic child : unbornChildren) {
                    if (this.level instanceof ServerWorld) {
                        this.spawnChild(child, (ServerWorld)this.level);
                    }
                }
                this.unbornChildren = new ArrayList<>();
                this.entityData.set(PREGNANT_SINCE, -1);
            }
        }

        // Overo lethal white syndrome
        if (this.getGenome().isLethalWhite()
            && this.tickCount > 80)
        {
            if (!this.hasEffect(Effects.WITHER))
            {
                this.addEffect(new EffectInstance(Effects.WITHER, 100, 3));
            }
        }
    }

    public void aiStep() {
        if (this.unbornChildren != null && this.unbornChildren.size() > 0
                && this.getPregnancyStart() < 0) {
            this.entityData.set(PREGNANT_SINCE, 0);
        }

        if (this.getGenome().isHomozygous(Gene.leopard, HorseAlleles.LEOPARD) && !this.level.isClientSide()) {
            ModifiableAttributeInstance speedAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
            ModifiableAttributeInstance jumpAttribute = this.getAttribute(Attributes.JUMP_STRENGTH);
            float brightness = this.getBrightness();
            if (brightness > 0.5f) {
                if (speedAttribute.getModifier(CSNB_SPEED_UUID) != null) {
                    speedAttribute.removeModifier(CSNB_SPEED_MODIFIER);
                }
                if (jumpAttribute.getModifier(CSNB_JUMP_UUID) != null) {
                    jumpAttribute.removeModifier(CSNB_JUMP_MODIFIER);
                }
            }
            else {
                if (speedAttribute.getModifier(CSNB_SPEED_UUID) == null) {
                    speedAttribute.addTransientModifier(CSNB_SPEED_MODIFIER);
                }
                if (jumpAttribute.getModifier(CSNB_JUMP_UUID) == null) {
                    jumpAttribute.addTransientModifier(CSNB_JUMP_MODIFIER);
                }
            }
        }

        super.aiStep();
    }

    // Returns the Y offset from the entity's position for any entity riding this one.
    @Override
    public double getPassengersRidingOffset() {
        return (double)this.getBbHeight() * 0.833 - 0.295;
    }

    /**
     * returns true if all the conditions for steering the entity are met. For pigs, this is true if it is being ridden
     * by a player and the player is holding a carrot-on-a-stick
     */
    @Override
    public boolean canBeControlledByRider() {
        return this.getControllingPassenger() instanceof LivingEntity
            && !(this.getControllingPassenger() instanceof AnimalEntity)
            && (this.getControllingPassenger() instanceof PlayerEntity 
                || !this.isLeashed());
    }

    @Override
    // Overriden so passenger position while rearing depends on the horse's size,
    // also to support multiple passengers.
    public void positionRider(Entity passenger) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        // Do not call super.positionRider. AbstractHorseEntity's implementation
        // sets this's yBodyRot to that of the passenger without
        // checking that the passenger can steer.
        // Setting yRot happens in AbstractHorseEntity.travel

        float xzOffset = -0.1f;
        if (this.getPassengers().size() > 1) {
            int i = this.getPassengers().indexOf(passenger);
            if (i == 0) {
                xzOffset = 0.1f;
            } else {
                xzOffset = -0.5f;
            }
        }
        xzOffset *= this.getGenome().getAdultScale();

        double yOffset = this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
        // Compensate for saddle for players
        if (passenger instanceof PlayerEntity && this.isSaddled()) {
            yOffset += 0.04 * this.getGenome().getAdultScale();
        }
        float standAnim0 = 0;
        try {
            standAnim0 = (Float)rearingAmountField.get(this);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (standAnim0 > 0.0F) {
            float xLoc = this.getBbWidth() + xzOffset;
            float facingX = MathHelper.sin(this.yBodyRot * ((float)Math.PI / 180F));
            float facingZ = MathHelper.cos(this.yBodyRot * ((float)Math.PI / 180F));
            // A rearing amount of 1 corresponds to 45 degrees up
            float rearAngle = standAnim0 * (float)Math.PI / 4F;
            float rearXZ = -1f * (1F - MathHelper.cos(rearAngle)) * xLoc;
            float rearY = MathHelper.sin(rearAngle) * xLoc / 2F;
            xzOffset += rearXZ;
            yOffset += rearY;
            if (passenger instanceof LivingEntity) {
                ((LivingEntity)passenger).yBodyRot = this.yBodyRot;
            }
        }


        // Here boats use this.yRot, but we use this.yBodyRot,
        // because this.yRot doesn't change when the unsaddled horse moves around
        Vector3d vector3d = new Vector3d((double)xzOffset, 0.0D, 0.0D);
        vector3d = vector3d.yRot(-this.yBodyRot * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
        passenger.setPos(this.getX() + vector3d.x, this.getY() + yOffset, this.getZ() + vector3d.z);
        this.applyYaw(passenger);
        if (passenger instanceof AnimalEntity && this.getPassengers().size() > 1) {
            int degrees = passenger.getId() % 2 == 0 ? 90 : 270;
            passenger.setYBodyRot(((AnimalEntity)passenger).yBodyRot + (float)degrees);
            passenger.setYHeadRot(passenger.getYHeadRot() + (float)degrees);
        }
    }

    private void applyYaw(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            entity.setYBodyRot(this.yBodyRot);
            entity.yRot = this.yBodyRot;
            entity.setYHeadRot(this.yBodyRot);
        }
    }

    @Override
    protected ITextComponent getTypeName() {
        String species = this.getSpecies().toString().toLowerCase();
        String s = "entity." + HorseColors.MODID + "." + species + ".";
        if (this.isBaby()) {
            // Foal
            if (!HorseConfig.BREEDING.enableGenders.get()) {
                return new TranslationTextComponent(s + "foal");
            }
            // Colt
            if (this.isMale()) {
                return new TranslationTextComponent(s + "colt");
            }
            // Filly
            return new TranslationTextComponent(s + "filly");
        }

        // Horse
        if (!HorseConfig.BREEDING.enableGenders.get()) {
            return super.getTypeName();
        }
        // Stallion
        if (this.isMale()) {
            return new TranslationTextComponent(s + "male");
        }
        // Mare
        return new TranslationTextComponent(s + "female");
    }

    public boolean isSaddle(ItemStack stack) {
        if (stack.getItem() == Items.SADDLE) {
            return true;
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        return registryName.getNamespace().equals("eanimod") && registryName.getPath().contains("saddle");
    }

    // Override to allow alternate saddles to be equipped
    @Override
    public boolean setSlot(int slot, ItemStack stack) {
        if (super.setSlot(slot, stack)) {
            return true;
        }
        int num = slot - 400;
        if (num == 0 && isSaddle(stack)) {
            this.inventory.setItem(num, stack);
        }
        return false;
    }

    @Override
    // This is needed so when the mutation chance is high, mules bred
    // with spawn eggs do not produce all splashed white foals.
    public Breed getDefaultBreed() {
        return BaseEquine.breed;
    }

    // Randomize only health, for mules and donkeys
    @Override
    protected void randomizeAttributes() {
        // Set stats for vanilla-like breeding
        if (!HorseConfig.GENETICS.useGeneticStats.get()) {
            float maxHealth = this.generateRandomMaxHealth() + this.getGenome().getBaseHealth();
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)maxHealth);
        }
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, 
                                            DifficultyInstance difficultyIn, 
                                            SpawnReason reason, 
                                            @Nullable ILivingEntityData spawnDataIn, 
                                            @Nullable CompoundNBT dataTag)
    {
        if (!(spawnDataIn instanceof GeneticData)) {
            Breed breed = this.getRandomBreed();
            spawnDataIn = new GeneticData(breed);
        }
        Breed breed = ((GeneticData)spawnDataIn).breed;
        this.randomize(breed);
        // super.finalizeSpawn will call randomizeAttributes
        ILivingEntityData data = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.useGeneticAttributes();
        return data;
    }

    private void randomize(Breed breed) {
        this.getGenome().randomize(breed);
        // Choose a random age
        this.trueAge = this.random.nextInt(HorseConfig.GROWTH.getMaxAge());
        // This preserves the ratio of child/adult
        if (this.random.nextInt(5) == 0) {
            // Foals pick a random age within the younger half
            this.trueAge = this.getBirthAge() + this.random.nextInt(-this.getBirthAge() / 2);
        }
        this.setMale(this.random.nextBoolean());
        // Don't set the growing age to a positive value, that would be bad
        this.setAge(Math.min(0, this.trueAge));
        this.useGeneticAttributes();
        // Assume mother was the same size
        this.setMotherSize(this.getGenome().getGeneticScale());
        // Size depends on mother size so call again to stabilize somewhat
        this.setMotherSize(this.getGenome().getGeneticScale());
    }

    public void initFromVillageSpawn() {
        this.randomize(getRandomBreed());
        // All village horses are easier to tame
        this.modifyTemper(this.getMaxTemper() / 2);
        if (!this.isBaby() && this.random.nextInt(16) == 0) {
            // Tame and saddle
            this.setTamed(true);
            ItemStack saddle = new ItemStack(Items.SADDLE);
            this.inventory.setItem(0, saddle);
        }
    }

    // Total size change based on age that does not change proportions
    public float getProportionalAgeScale() {
        return getGenome().getCurrentScale() / getGangliness();
    }

    // The horse model uses this number to decide how foal-shaped to make the
    // horse. 0.5 is the most foal-shaped and 1 is the most adult-shaped.
    public float getGangliness() {
        return 0.5f + 0.5f * getFractionGrown() * getFractionGrown();
    }

    // Affects hitbox size.
    @Override
    public float getScale() {
        // This is different from LivingEntity.getScale which uses
        // 0.5 for children
        float base = isBaby()? 0.6f : 1.0f;
        return this.getGenome().getAdultScale() * base;
    }

    @Override
    public boolean isSaddleable() {
        return (!HorseConfig.COMMON.enableSizes.get() || !this.getGenome().isMiniature()) 
                && super.isSaddleable();
    }

    @Override
    public boolean isPushable() {
        return !(this.isVehicle() 
            && this.getControllingPassenger() instanceof PlayerEntity);
    }

    // For holding spawn data
    public static class GeneticData extends AgeableEntity.AgeableData {
        public final Breed breed;

        public GeneticData(Breed breed) {
            super(true);
            this.breed = breed;
        }
    }
}
