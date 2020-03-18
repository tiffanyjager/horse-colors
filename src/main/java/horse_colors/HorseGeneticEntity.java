package sekelsta.horse_colors;

import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.block.SoundType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.horse.MuleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.HorseArmorItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import sekelsta.horse_colors.ComplexLayeredTexture.Layer;


public class HorseGeneticEntity extends AbstractHorseGenetic
{
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
    private static final DataParameter<Integer> HORSE_RANDOM = EntityDataManager.<Integer>createKey(HorseGeneticEntity.class, DataSerializers.VARINT);


    // See the function that sets this to find what each of  the layers are for
    private final Layer[] horseTexturesArray = new Layer[16];

    public HorseGeneticEntity(EntityType<? extends HorseGeneticEntity> entityType, World worldIn)
    {
        super(entityType, worldIn);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorseEntity.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.initExtraAI();
    }

    @Override
    protected void initExtraAI() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
    }

    @Override
    protected void registerData()
    {
        super.registerData();
        this.dataManager.register(HORSE_RANDOM, Integer.valueOf(0));
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    @Override
    public void writeAdditional(CompoundNBT compound)
    {
        super.writeAdditional(compound);
        compound.putInt("Random", this.getHorseVariant("random"));

        if (!this.horseChest.getStackInSlot(1).isEmpty())
        {
            compound.put("ArmorItem", this.horseChest.getStackInSlot(1).write(new CompoundNBT()));
        }
    }

   private void setArmor(ItemStack itemStackIn) {
      this.setItemStackToSlot(EquipmentSlotType.CHEST, itemStackIn);
      this.setDropChance(EquipmentSlotType.CHEST, 0.0F);
   }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditional(CompoundNBT compound)
    {
        super.readAdditional(compound);
        this.setHorseVariant(compound.getInt("Random"), "random");

        if (compound.contains("ArmorItem", 10))
        {
            ItemStack itemstack = ItemStack.read(compound.getCompound("ArmorItem"));

            if (!itemstack.isEmpty() && this.isArmor(itemstack))
            {
                this.horseChest.setInventorySlotContents(1, itemstack);
            }
        }

        this.updateHorseSlots();
    }

    public void setHorseVariant(int variant, String type)
    {
        switch(type) {
            case "random":
                this.dataManager.set(HORSE_RANDOM, Integer.valueOf(variant));
                break;
            default:
                super.setHorseVariant(variant, type);
        }
        this.resetTexturePrefix();
    }

    public int getHorseVariant(String type)
    {
        switch(type) {
            case "random":
                return ((Integer)this.dataManager.get(HORSE_RANDOM)).intValue();
            default:
                return super.getHorseVariant(type);
        }
        
    }


    public int getPhenotype(String name)
    {
        switch(name)
        {
            /* Simple dominant or recessive genes. */
            case "silver":
            case "liver":
            case "flaxen1":
            case "flaxen2":
            case "dapple":
            case "sooty1":
            case "sooty2":
            case "sooty3":
            case "mealy1":
            case "mealy2":
            case "mealy3":
            case "white_suppression":
            case "PATN2":
            case "PATN3":
            case "gray_suppression":
            case "slow_gray1":
                return getMaxAllele(name);

            /* Incomplete dominant. */
            case "leopard":
            case "gray":
            case "cream":
            case "frame":
            case "PATN1":
            case "gray_mane":
            case "slow_gray2":
            case "white_star":
            case "white_forelegs":
            case "white_hindlegs":
                /* Low bit plus high bit. */
                return (getGene(name) & 1) + (getGene(name) >> 1);
                
            
            /* Polygenetic traits. */
            case "flaxen":
                return 2 - getPhenotype("flaxen1") - getPhenotype("flaxen2");
            case "sooty":
                // sooty1 and 2 dominant, 3 recessive
                return 1 + getPhenotype("sooty1") + getPhenotype("sooty2") 
                        - getPhenotype("sooty3");
            case "mealy":
                return 2 - getPhenotype("mealy1") - getPhenotype("mealy2");
            case "splash":
                // TODO
                return countAlleles("MITF", HorseAlleles.MITF_SW1);

            /* Genes with multiple alleles. */
            case "extension":
                return Math.max(getAllele(name, 0), getAllele(name, 1));
            case "agouti":
                if (getGene("agouti") == 1 || getGene("agouti") == 4)
                {
                    return 1;
                }
                int allele = Math.max(getGene("agouti") & 3, getGene("agouti") >> 2);
                return allele == 0? 0 : allele + 1;

            case "dun":
                if (getGene(name) <= 1) 
                {
                    // 0 for ND2 (no dorsal stripe), 1 for ND1/+ (faint 
                    // dorsal stripe), 2 for ND1/ND1 (dorsal stripe), 3 for dun
                    return getGene(name);
                }
                else if (getGene(name) == 4)
                {
                    return 1;
                }
                else if (getGene(name) == 5)
                {
                    return 2;
                }
                else
                {
                    return 3;
                }
            // Don't give useful info when asked for KIT, but also don't
            // give an error
            case "KIT":
                return -1;
            /* KIT mappings:
               0: wildtype
               1 to 6: contribute to white markings
               7: W20 (strip and socks typical as heterozygous, 
                    when homozygous, irregular draft sabino with some belly white)
               8: reserved in case I add rabicano to KIT
               9: flashy white (tends towards stockings and blaze)
               10: wildtype for now
               11: tobiano
               12: sabino1
               13: tobiano + W20
               14: roan
               15: white
            */
            // W20 is incomplete dominant
            case "W20":
                return countAlleles("KIT", HorseAlleles.KIT_W20) 
                    + countAlleles("KIT", HorseAlleles.KIT_TOBIANO_W20);
            // Sabino1 and tobiano are also incomplete dominant
            case "sabino1":
                if (getGene("KIT") == (12 << 4) + 12)
                {
                    return 2;
                }
                else
                {
                    return ((getGene("KIT") & 15) == 12) 
                            || ((getGene("KIT") >> 4) == 12)? 1 : 0;
                }
            case "tobiano":
                boolean tob1 = getAllele("KIT", 0) == 13 
                                || getAllele("KIT", 0) == 11;
                boolean tob2 = getAllele("KIT", 1) == 13 
                                || getAllele("KIT", 1) == 11;
                if (tob1 && tob2)
                {
                    return 2;
                }
                else
                {
                    return (tob1 || tob2)? 1 : 0;
                }
            case "roan":
                return hasAllele("KIT", HorseAlleles.KIT_ROAN)? 1 : 0;
            case "dominant_white":
                if (getGene("KIT") == (15 << 4) + 15)
                {
                    return 2;
                }
                return ((getGene("KIT") & 15) == 15
                        || (getGene("KIT") >> 4) == 15)? 1 : 0;
            case "white":
                return (getPhenotype("dominant_white") != 0 // dominant white
                        || getPhenotype("frame") == 2  // lethal white overo
                        || getPhenotype("sabino1") == 2 // sabino white
                        || (getPhenotype("sabino1") != 0 
                            && getPhenotype("frame") != 0
                            && getPhenotype("tobiano") != 0))
                                ? 1 : 0;
            // other KIT: TODO
            case "PATN":
                int base = 5 * getPhenotype("PATN1") + getPhenotype("PATN2")
                           + getPhenotype("PATN3");
                return base == 0? 0 : base + getPhenotype("W20");
            case "slow_gray":
                // Larger numbers make a darker horse.
                int val = getPhenotype("slow_gray1") + getPhenotype("slow_gray2")
                        + (getPhenotype("gray") == 2? -2 : 0)
                        + (getPhenotype("gray_mane") == 0? 0 : 1);
                return Math.min(Math.max(val, 0), 3);
            case "MITF": return -1;
            case "PAX3": return -1;         
        }
        System.out.println("[horse_colors]: Phenotype for " + name + " not found.");
        return -1;
    }

    public boolean showsLegMarkings()
    {
        return getPhenotype("tobiano") == 0 && getPhenotype("splash") != 2
                && getPhenotype("white") == 0;
    }

    public String getAbv(Layer layer) {
        if (layer == null || layer.name == null) {
            return "";
        }        
        String abv = layer.name;
        int i = abv.lastIndexOf("/");
        if (i > -1) {
            abv = abv.substring(i + 1);
        }
        if (abv.endsWith(".png")) {
            abv = abv.substring(0, abv.length() - 4);
        }
        abv += "-" + Integer.toHexString(layer.alpha);
        abv += Integer.toHexString(layer.red);
        abv += Integer.toHexString(layer.green);
        abv += Integer.toHexString(layer.blue) + "_";
        return abv;
    }

    @OnlyIn(Dist.CLIENT)
    private void setHorseTexturePaths()
    {
        this.horseTexturesArray[0] = HorseColorCalculator.getRedBody(this);
        this.horseTexturesArray[1] = HorseColorCalculator.getRedManeTail(this);
        this.horseTexturesArray[2] = HorseColorCalculator.getBlackBody(this);
        this.horseTexturesArray[3] = HorseColorCalculator.getBlackManeTail(this);


        Layer common = new Layer();
        common.name = HorseColorCalculator.fixPath("", "common");
        this.horseTexturesArray[4] = common;

        this.texturePrefix = "horse/cache_";

        for (int i = 0; i < 4; ++i) {
            this.texturePrefix += getAbv(this.horseTexturesArray[i]);
        }



        this.horseTexturesArray[5] = new Layer();
        this.horseTexturesArray[6] = new Layer();
        this.horseTexturesArray[7] = new Layer();
        this.horseTexturesArray[8] = new Layer();
        this.horseTexturesArray[9] = new Layer();
        this.horseTexturesArray[10] = new Layer();
        this.horseTexturesArray[11] = new Layer();
        this.horseTexturesArray[12] = new Layer();
        this.horseTexturesArray[13] = new Layer();
        this.horseTexturesArray[14] = new Layer();
        this.horseTexturesArray[15] = new Layer();

        String base_texture = HorseColorCalculator.getBaseTexture(this);

        String roan = hasAllele("KIT", HorseAlleles.KIT_ROAN)? "roan" : null;
        String face_marking = HorseColorCalculator.getFaceMarking(this);
        String sooty = HorseColorCalculator.getSooty(this);
        String legs = HorseColorCalculator.getLegs(this);
        String gray_mane = HorseColorCalculator.getGrayMane(this);
        String[] leg_markings = new String[4];
        
        String pinto = HorseColorCalculator.getPinto(this);
        if (showsLegMarkings())
        {
            leg_markings = HorseColorCalculator.getLegMarkings(this);
        }

        this.horseTexturesArray[5].name = HorseColorCalculator.fixPath("base", base_texture);
        this.horseTexturesArray[6].name = HorseColorCalculator.fixPath("sooty", sooty);
        this.horseTexturesArray[7].name = HorseColorCalculator.fixPath("legs", legs);
        this.horseTexturesArray[8].name = HorseColorCalculator.fixPath("roan", roan);
        this.horseTexturesArray[9].name = HorseColorCalculator.fixPath("roan", gray_mane);
        this.horseTexturesArray[10].name = HorseColorCalculator.fixPath("face", face_marking);
        this.horseTexturesArray[11].name = HorseColorCalculator.fixPath("socks", leg_markings[0]);
        this.horseTexturesArray[12].name = HorseColorCalculator.fixPath("socks", leg_markings[1]);
        this.horseTexturesArray[13].name = HorseColorCalculator.fixPath("socks", leg_markings[2]);
        this.horseTexturesArray[14].name = HorseColorCalculator.fixPath("socks", leg_markings[3]);
        this.horseTexturesArray[15].name = HorseColorCalculator.fixPath("pinto", pinto);

        String base_abv = base_texture == null? "" : base_texture;
        String sooty_abv = sooty == null? "" : sooty;
        String legs_abv = legs == null? "" : legs;
        String roan_abv = roan == null? "" : roan;
        String gray_mane_abv = gray_mane == null? "" : gray_mane;
        String face_marking_abv = face_marking == null? "" : face_marking;
        String leg_marking_abv = 
            (leg_markings[0] == null? "-" : leg_markings[0]) 
            + (leg_markings[1] == null? "-" : leg_markings[1]) 
            + (leg_markings[2] == null? "-" : leg_markings[2]) 
            + (leg_markings[3] == null? "-" : leg_markings[3]);
        String pinto_abv = pinto == null? "" : pinto;
        this.texturePrefix += base_abv + sooty_abv 
            + roan_abv + gray_mane_abv + face_marking_abv 
            + leg_marking_abv + pinto_abv;
    }

    @OnlyIn(Dist.CLIENT)
    public String getHorseTexture()
    {
        if (this.texturePrefix == null)
        {
            this.setHorseTexturePaths();
        }

        return this.texturePrefix;
    }

    @OnlyIn(Dist.CLIENT)
    public ComplexLayeredTexture.Layer[] getVariantTexturePaths()
    {
        if (this.texturePrefix == null)
        {
            this.setHorseTexturePaths();
        }

        return this.horseTexturesArray;
    }

    /**
     * Updates the items in the saddle and armor slots of the horse's inventory.
     */
    @Override
    protected void updateHorseSlots()
    {
        super.updateHorseSlots();
        this.setHorseArmorStack(this.horseChest.getStackInSlot(1));
    }

    /**
     * Set horse armor stack (for example: new ItemStack(Items.iron_horse_armor))
     */
    public void setHorseArmorStack(ItemStack itemStackIn)
    {
      this.setArmor(itemStackIn);
      if (!this.world.isRemote) {
         this.getAttribute(SharedMonsterAttributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
         if (this.isArmor(itemStackIn)) {
            // getProtection, possibly
            int i = ((HorseArmorItem)itemStackIn.getItem()).func_219977_e();
            if (i != 0) {
               this.getAttribute(SharedMonsterAttributes.ARMOR).applyModifier((new AttributeModifier(ARMOR_MODIFIER_UUID, "Horse armor bonus", (double)i, AttributeModifier.Operation.ADDITION)).setSaved(false));
            }
         }
      }
    }

    public ItemStack getHorseArmor() {
        return this.getItemStackFromSlot(EquipmentSlotType.CHEST);
    }

    /**
     * Called by InventoryBasic.onInventoryChanged() on a array that is never filled.
     */
    @Override
    public void onInventoryChanged(IInventory invBasic)
    {
        ItemStack itemstack = this.getHorseArmor();
        super.onInventoryChanged(invBasic);
        ItemStack itemstack1 = this.getHorseArmor();

        if (this.ticksExisted > 20 && this.isArmor(itemstack1) && itemstack != itemstack1) {
            this.playSound(SoundEvents.ENTITY_HORSE_ARMOR, 0.5F, 1.0F);
        }
    }

    @Override
    protected void playGallopSound(SoundType p_190680_1_)
    {
        super.playGallopSound(p_190680_1_);

        if (this.rand.nextInt(10) == 0)
        {
            this.playSound(SoundEvents.ENTITY_HORSE_BREATHE, p_190680_1_.getVolume() * 0.6F, p_190680_1_.getPitch());
        }
    }

    private void useGeneticAttributes()
    {
        if (HorseConfig.COMMON.useGeneticStats.get())
        {
            // Default horse health ranges from 15 to 30, but ours goes from
            // 15 to 31
            float maxHealth = 15.0F + getStat("health") * 0.5F;
            // Vanilla horse speed ranges from 0.1125 to 0.3375, as does ours
            double movementSpeed = 0.1125D + getStat("speed") * (0.225D / 32.0D);
            // Vanilla horse jump strength ranges from 0.4 to 1.0, as does ours
            double jumpStrength = 0.4D + getStat("jump") * (0.6D / 32.0D);

            this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(maxHealth);
            this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(movementSpeed);
            this.getAttribute(JUMP_STRENGTH).setBaseValue(jumpStrength);
        }
    }

    @Override
    protected void registerAttributes()
    {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue((double)this.getModifiedMaxHealth());
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.getModifiedMovementSpeed());
        this.getAttribute(JUMP_STRENGTH).setBaseValue(this.getModifiedJumpStrength());
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick()
    {
        super.tick();

        if (this.world.isRemote && this.dataManager.isDirty())
        {
            this.dataManager.setClean();
            this.resetTexturePrefix();
        }
        ItemStack armor = this.horseChest.getStackInSlot(1);
        if (isArmor(armor)) armor.onHorseArmorTick(world, this);
        // Overo lethal white syndrome
        if ((!this.world.isRemote || true)
            && this.getPhenotype("frame") == 2
            && this.ticksExisted > 80)
        {
            if (!this.isPotionActive(Effects.POISON))
            {
                this.addPotionEffect(new EffectInstance(Effects.POISON, 100, 3));
            }
            if (this.getHealth() < 2)
            {
                this.addPotionEffect(new EffectInstance(Effects.INSTANT_DAMAGE, 1, 3));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        super.getAmbientSound();
        return SoundEvents.ENTITY_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        super.getDeathSound();
        return SoundEvents.ENTITY_HORSE_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        super.getHurtSound(damageSourceIn);
        return SoundEvents.ENTITY_HORSE_HURT;
    }

    @Override
    protected SoundEvent getAngrySound()
    {
        super.getAngrySound();
        return SoundEvents.ENTITY_HORSE_ANGRY;
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand)
    {
        ItemStack itemstack = player.getHeldItem(hand);
        boolean notEmpty = !itemstack.isEmpty();

        if (notEmpty && itemstack.getItem() instanceof SpawnEggItem)
        {
            return super.processInteract(player, hand);
        }
        else
        {
            if (!this.isChild())
            {
                //func_226563_dT_() == isSneaking()
                if (this.isTame() && player.func_226563_dT_())
                {
                    this.openGUI(player);
                    return true;
                }

                if (this.isBeingRidden())
                {
                    return super.processInteract(player, hand);
                }
            }

            if (notEmpty)
            {
                if (this.handleEating(player, itemstack))
                {
                    if (!player.abilities.isCreativeMode)
                    {
                        itemstack.shrink(1);
                    }

                    return true;
                }

                if (itemstack.interactWithEntity(player, this, hand))
                {
                    return true;
                }

                if (!this.isTame())
                {
                    this.makeMad();
                    return true;
                }

                boolean saddle = !this.isChild() && !this.isHorseSaddled() && itemstack.getItem() == Items.SADDLE;

                if (this.isArmor(itemstack) || saddle)
                {
                    this.openGUI(player);
                    return true;
                }
            }

            if (this.isChild())
            {
                return super.processInteract(player, hand);
            }
            else
            {
                this.mountTo(player);
                return true;
            }
        }
    }

    /**
     * Returns true if the mob is currently able to mate with the specified mob.
     */
    @Override
    public boolean canMateWith(AnimalEntity otherAnimal)
    {
        if (otherAnimal == this)
        {
            return false;
        }
        // Mate with other horses or donkeys
        else if (otherAnimal instanceof HorseGeneticEntity)
        {
            return this.canMate() && ((HorseGeneticEntity)otherAnimal).canMate();
        }
        else if (otherAnimal instanceof DonkeyEntity)
        {
            AbstractHorseEntity other = (AbstractHorseEntity)otherAnimal;
            // This is the same as calling other.canMate() but doesn't require
            // reflection
            boolean otherCanMate = !other.isBeingRidden() && !other.isPassenger() && other.isTame() && !other.isChild() && other.getHealth() >= other.getMaxHealth() && other.isInLove();
            return this.canMate() && otherCanMate;
        }
        else
        {
            return false;
        }
    }

    private int inheritStats(AbstractHorseGenetic other, String chromosome) {
            int mother = this.getRandomGenericGenes(1, this.getHorseVariant(chromosome));
            int father = other.getRandomGenericGenes(0, other.getHorseVariant(chromosome));
            return mother | father;
    }

    @Override
    public AgeableEntity createChild(AgeableEntity ageable)
    {
        AbstractHorseEntity abstracthorse;

        if (ageable instanceof DonkeyEntity)
        {
            abstracthorse = EntityType.MULE.create(this.world);
        }
        else
        {
            HorseGeneticEntity entityHorse = (HorseGeneticEntity)ageable;
            abstracthorse = ModEntities.HORSE_GENETIC.create(this.world);

            int mother = this.getRandomGenes(1, 0);
            int father = entityHorse.getRandomGenes(0, 0);
            int i = mother | father;
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(i, "0");

            mother = this.getRandomGenes(1, 1);
            father = entityHorse.getRandomGenes(0, 1);
            i = mother | father;
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(i, "1");


            ((HorseGeneticEntity)abstracthorse).setHorseVariant(rand.nextInt(), "2");
            mother = this.getRandomGenes(1, 2);
            father = entityHorse.getRandomGenes(0, 2);
            i = mother | father;
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(i, "2");

            // speed, health, and jump
            int speed = inheritStats(entityHorse, "speed");
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(speed, "speed");
            int health = inheritStats(entityHorse, "health");
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(health, "health");
            int jump = inheritStats(entityHorse, "jump");
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(jump, "jump");


            i =  this.rand.nextInt();
            ((HorseGeneticEntity)abstracthorse).setHorseVariant(i, "random");

            // Dominant white is homozygous lethal early in pregnancy. No child
            // is born.
            if (((HorseGeneticEntity)abstracthorse).getPhenotype("dominant_white")
                    == 2)
            {
                return null;
            }
        }

        this.setOffspringAttributes(ageable, abstracthorse);
        if (abstracthorse instanceof HorseGeneticEntity)
        {
            ((HorseGeneticEntity)abstracthorse).mutate();
            ((HorseGeneticEntity)abstracthorse).useGeneticAttributes();
        }
        return abstracthorse;
    }

    @Override
    public boolean wearsArmor()
    {
        return true;
    }

    @Override
    public boolean isArmor(ItemStack stack)
    {
        return stack.getItem() instanceof HorseArmorItem;
    }

    // with 1/odds probability gets the gene to 0 or 1, whichever common isn't
    private void setGeneRandom(String name, int n, int odds, int common)
    {
            int i = this.rand.nextInt();
            int rare = common == 0? 1 : 0;
            setGene(name, (i % odds == 0? rare : common) 
                            << (n * getGeneSize(name)));
    }

    /* This function changes the variant and then puts it back to what it was
    before. */
    private int getRandomVariant(int n, String type)
    {
        int answer = 0;
        int startVariant = getHorseVariant(type);

        if (type == "0")
        {
            // logical bitshift to make unsigned
            int i = this.rand.nextInt() >>> 1;
            setGene("extension", (i & 7) << (n * getGeneSize("extension")));
            i >>= 3;
            setGeneRandom("gray", n, 20, 0);
            int dun = (this.rand.nextInt() % 7 == 0? 2 : 0) + (i % 4 == 0? 1: 0);
            setGene("dun", dun << (n * getGeneSize("dun")));
            i >>= 2;

            int ag = i % 16;
            int agouti = ag == 0? HorseAlleles.A_BAY_MEALY 
                       : ag == 1? HorseAlleles.A_BAY_WILD
                       : ag < 4? HorseAlleles.A_BAY_LIGHT
                       : ag < 6? HorseAlleles.A_BAY
                       : ag < 8? HorseAlleles.A_BAY_DARK
                       : ag == 8? HorseAlleles.A_BROWN
                       : ag == 9? HorseAlleles.A_SEAL
                       : HorseAlleles.A_BLACK;
            setGene("agouti", agouti << (n * getGeneSize("agouti")));
            i >>= 4;

            setGeneRandom("silver", n, 32, 0);
            int cr = i % 32;
            int cream = cr == 0? HorseAlleles.CREAM
                      : cr == 1? HorseAlleles.PEARL
                      : cr == 2? HorseAlleles.NONCREAM2
                      : HorseAlleles.NONCREAM;
            setGene("cream", cream << (n * getGeneSize("cream")));
            i >>= 5;
            setGeneRandom("liver", n, 3, 1);
            setGeneRandom("flaxen1", n, 5, 1);
            setGeneRandom("flaxen2", n, 5, 1);

            setGene("dapple", (i % 2) << (n * getGeneSize("dapple")));
            i >>= 1;
        }
        else if (type == "1")
        {
            // logical bitshift to make unsigned
            int i = this.rand.nextInt() >>> 1;

            setGeneRandom("sooty1", n, 4, 1);
            setGeneRandom("sooty2", n, 4, 1);
            setGeneRandom("sooty3", n, 2, 1);
            setGeneRandom("mealy1", n, 4, 1);
            setGeneRandom("mealy2", n, 4, 1);
            setGeneRandom("mealy3", n, 4, 1);
            setGeneRandom("white_suppression", n, 32, 0);

            int kit = i % 4 != 0? 0
//                                : (i >> 2) % 2 == 0? (i >> 3) % 8
                                : (i >> 3) % 16;
            setGene("KIT", kit << (n * getGeneSize("KIT")));
            i >>= 7;

            setGeneRandom("frame", n, 32, 0);
            int mitf = i % 4 == 0? HorseAlleles.MITF_WILDTYPE
                : (i >> 2) % 2 == 0? (i >> 3) % 4
                : HorseAlleles.MITF_WILDTYPE;
            setGene("MITF", mitf << (n * getGeneSize("MITF")));
            i >>= 5;
            int pax3 = i % 4 != 0? HorseAlleles.PAX3_WILDTYPE
                : (i >> 2) % 4;
            setGene("PAX3", pax3 << (n * getGeneSize("PAX3")));
        }
        else if (type == "2")
        {
            // Initialize any bits currently unused to random values
            setHorseVariant(this.rand.nextInt(), "2");
            int i = this.rand.nextInt();
            setGeneRandom("leopard", n, 32, 0);
            setGeneRandom("PATN1", n, 16, 0);
            setGeneRandom("PATN2", n, 16, 0);
            setGeneRandom("PATN3", n, 16, 0);
            setGeneRandom("gray_suppression", n, 40, 0);
            setGeneRandom("gray_mane", n, 4, 0);
            setGeneRandom("slow_gray1", n, 8, 0);
            setGeneRandom("slow_gray2", n, 4, 0);
            setGeneRandom("white_star", n, 4, 0);
            setGeneRandom("white_forelegs", n, 4, 0);
            setGeneRandom("white_hindlegs", n, 4, 0);
        }

        answer = getHorseVariant(type);
        setHorseVariant(startVariant, type);
        return answer;
    }

    private void randomizeSingleVariant(String variant)
    {
        int i = getRandomVariant(0, variant);
        int j = getRandomVariant(1, variant);
        setHorseVariant(i | j, variant);
    }

    /* Make the horse have random genetics. */
    public void randomize()
    {
        randomizeSingleVariant("0");
        randomizeSingleVariant("1");
        randomizeSingleVariant("2");

        // Replace lethal white overos with heterozygotes
        if (getPhenotype("frame") == 2)
        {
            setGene("frame", 1);
        }

        // Homozygote dominant whites will be replaced with heterozygotes
        if (getPhenotype("dominant_white") == 2)
        {
            setGene("KIT", 15);
        }

        setHorseVariant(this.rand.nextInt(), "speed");
        setHorseVariant(this.rand.nextInt(), "jump");
        setHorseVariant(this.rand.nextInt(), "health");
        setHorseVariant(this.rand.nextInt(), "random");
        useGeneticAttributes();
    }

    /**
     * Called only once on an entity when first time spawned, via egg, mob spawner, natural spawning etc, but not called
     * when entity is reloaded from nbt. Mainly used for initializing attributes and inventory
     */
    @Nullable
    @Override
    public ILivingEntityData onInitialSpawn(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag)
    {
        spawnDataIn = super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        /*
        int i;
        if (spawnDataIn instanceof HorseGeneticEntity.HerdData) {
            i = ((HorseGeneticEntity.HerdData)spawnDataIn).variant;
        } else {
         i = this.rand.nextInt(7);
         spawnDataIn = new HorseGeneticEntity.HerdData(i);
        }

        this.setHorseVariant(i | this.rand.nextInt(5) << 8);
        */
        this.randomize();
        return spawnDataIn;
    }

    public static class HerdData implements ILivingEntityData
        {
            public int variant;

            public HerdData(int variantIn)
            {
                this.variant = variantIn;
            }
        }
}