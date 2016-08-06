package org.cyclops.everlastingabilities;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.command.ICommand;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.command.CommandMod;
import org.cyclops.cyclopscore.config.ConfigHandler;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfigReference;
import org.cyclops.cyclopscore.helper.EntityHelpers;
import org.cyclops.cyclopscore.helper.ItemStackHelpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.init.ItemCreativeTab;
import org.cyclops.cyclopscore.init.ModBaseVersionable;
import org.cyclops.cyclopscore.init.RecipeHandler;
import org.cyclops.cyclopscore.modcompat.capabilities.SimpleCapabilityConstructor;
import org.cyclops.cyclopscore.proxy.ICommonProxy;
import org.cyclops.everlastingabilities.ability.AbilityHelpers;
import org.cyclops.everlastingabilities.api.Ability;
import org.cyclops.everlastingabilities.api.AbilityTypes;
import org.cyclops.everlastingabilities.api.IAbilityType;
import org.cyclops.everlastingabilities.api.IAbilityTypeRegistry;
import org.cyclops.everlastingabilities.api.capability.DefaultMutableAbilityStore;
import org.cyclops.everlastingabilities.api.capability.IMutableAbilityStore;
import org.cyclops.everlastingabilities.capability.AbilityStoreConfig;
import org.cyclops.everlastingabilities.capability.MutableAbilityStoreConfig;
import org.cyclops.everlastingabilities.command.CommandModifyAbilities;
import org.cyclops.everlastingabilities.core.AbilityTypeRegistry;
import org.cyclops.everlastingabilities.core.SerializableCapabilityProvider;
import org.cyclops.everlastingabilities.core.helper.obfuscation.ObfuscationHelpers;
import org.cyclops.everlastingabilities.item.ItemAbilityBottle;
import org.cyclops.everlastingabilities.item.ItemAbilityBottleConfig;
import org.cyclops.everlastingabilities.item.ItemAbilityTotem;
import org.cyclops.everlastingabilities.item.ItemAbilityTotemConfig;
import org.cyclops.everlastingabilities.network.packet.SendPlayerCapabilitiesPacket;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * The main mod class of this mod.
 * @author rubensworks (aka kroeserr)
 *
 */
@Mod(
        modid = Reference.MOD_ID,
        name = Reference.MOD_NAME,
        useMetadata = true,
        version = Reference.MOD_VERSION,
        dependencies = Reference.MOD_DEPENDENCIES,
        guiFactory = "org.cyclops.everlastingabilities.GuiConfigOverview$ExtendedConfigGuiFactory"
)
public class EverlastingAbilities extends ModBaseVersionable {
    
    /**
     * The proxy of this mod, depending on 'side' a different proxy will be inside this field.
     * @see SidedProxy
     */
    @SidedProxy(clientSide = "org.cyclops.everlastingabilities.proxy.ClientProxy", serverSide = "org.cyclops.everlastingabilities.proxy.CommonProxy")
    public static ICommonProxy proxy;
    
    /**
     * The unique instance of this mod.
     */
    @Instance(value = Reference.MOD_ID)
    public static EverlastingAbilities _instance;

    public EverlastingAbilities() {
        super(Reference.MOD_ID, Reference.MOD_NAME, Reference.MOD_VERSION);
    }

    @Override
    protected RecipeHandler constructRecipeHandler() {
        return new RecipeHandler(this, "shaped.xml");
    }

    @Override
    protected ICommand constructBaseCommand() {
        Map<String, ICommand> commands = Maps.newHashMap();
        commands.put(CommandModifyAbilities.NAME, new CommandModifyAbilities(this));
        CommandMod command =  new CommandMod(this, commands);
        command.addAlias("ea");
        return command;
    }

    /**
     * The pre-initialization, will register required configs.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        getRegistryManager().addRegistry(IAbilityTypeRegistry.class, AbilityTypeRegistry.getInstance());

        super.preInit(event);
    }
    
    /**
     * Register the config dependent things like world generation and proxy handlers.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        AbilityTypes.load();

        getCapabilityConstructorRegistry().registerInheritableEntity(EntityPlayer.class, new SimpleCapabilityConstructor<IMutableAbilityStore, EntityPlayer>() {
            @Nullable
            @Override
            public ICapabilityProvider createProvider(EntityPlayer host) {
                return new SerializableCapabilityProvider<IMutableAbilityStore>(getCapability(), new DefaultMutableAbilityStore());
            }

            @Override
            public Capability<IMutableAbilityStore> getCapability() {
                return MutableAbilityStoreConfig.CAPABILITY;
            }
        });
    }
    
    /**
     * Register the event hooks.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
    
    /**
     * Register the things that are related to server starting, like commands.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void onServerStarting(FMLServerStartingEvent event) {
        super.onServerStarting(event);
    }

    /**
     * Register the things that are related to server starting.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void onServerStarted(FMLServerStartedEvent event) {
        super.onServerStarted(event);
    }

    /**
     * Register the things that are related to server stopping, like persistent storage.
     * @param event The Forge event required for this.
     */
    @EventHandler
    @Override
    public void onServerStopping(FMLServerStoppingEvent event) {
        super.onServerStopping(event);
    }

    @Override
    public CreativeTabs constructDefaultCreativeTab() {
        return new ItemCreativeTab(this, new ItemConfigReference(ItemAbilityTotemConfig.class));
    }

    @Override
    public void onGeneralConfigsRegister(ConfigHandler configHandler) {
        configHandler.add(new GeneralConfig());
    }

    @Override
    public void onMainConfigsRegister(ConfigHandler configHandler) {
        super.onMainConfigsRegister(configHandler);

        configHandler.add(new AbilityStoreConfig());
        configHandler.add(new MutableAbilityStoreConfig());

        configHandler.add(new ItemAbilityTotemConfig());
        configHandler.add(new ItemAbilityBottleConfig());
    }

    @Override
    public ICommonProxy getProxy() {
        return proxy;
    }

    /**
     * Log a new info message for this mod.
     * @param message The message to show.
     */
    public static void clog(String message) {
        clog(Level.INFO, message);
    }
    
    /**
     * Log a new message of the given level for this mod.
     * @param level The level in which the message must be shown.
     * @param message The message to show.
     */
    public static void clog(Level level, String message) {
        EverlastingAbilities._instance.getLoggerHelper().log(level, message);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
            getPacketHandler().sendToPlayer(
                    new SendPlayerCapabilitiesPacket(ObfuscationHelpers.getEntityCapabilities(player)), player);
        }
    }

    private static final String NBT_TOTEM_SPAWNED = Reference.MOD_ID + ":totemSpawned";
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (GeneralConfig.totemMaximumSpawnRarity >= 0) {
            NBTTagCompound tag = event.player.getEntityData();
            if (!tag.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                tag.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
            }
            NBTTagCompound playerTag = tag.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            if (!playerTag.hasKey(NBT_TOTEM_SPAWNED)) {
                playerTag.setBoolean(NBT_TOTEM_SPAWNED, true);

                World world = event.player.worldObj;
                EntityPlayer player = event.player;
                IAbilityType abilityType = AbilityHelpers.getRandomAbility(world.rand, EnumRarity.values()[GeneralConfig.totemMaximumSpawnRarity]);
                if (abilityType != null) {
                    ItemStack itemStack = new ItemStack(ItemAbilityBottle.getInstance());
                    IMutableAbilityStore mutableAbilityStore = itemStack.getCapability(MutableAbilityStoreConfig.CAPABILITY, null);
                    mutableAbilityStore.addAbility(new Ability(abilityType, 1), true);

                    ItemStackHelpers.spawnItemStackToPlayer(world, player.getPosition(), itemStack, player);
                    EntityHelpers.spawnXpAtPlayer(world, player, abilityType.getBaseXpPerLevel());
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (GeneralConfig.dropAbilitiesOnPlayerDeath > 0
                && event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().worldObj.isRemote
                && (GeneralConfig.alwaysDropAbilities || (event.getSource() instanceof EntityDamageSource) && event.getSource().getEntity() instanceof EntityPlayer)) {
            int toDrop = GeneralConfig.dropAbilitiesOnPlayerDeath;
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            IMutableAbilityStore mutableAbilityStore = player.getCapability(MutableAbilityStoreConfig.CAPABILITY, null);

            ItemStack itemStack = new ItemStack(ItemAbilityTotem.getInstance());
            IMutableAbilityStore itemStackStore = itemStack.getCapability(MutableAbilityStoreConfig.CAPABILITY, null);

            Collection<Ability> abilities = Lists.newArrayList(mutableAbilityStore.getAbilities());
            for (Ability ability : abilities) {
                if (toDrop > 0) {
                    Ability toRemove = new Ability(ability.getAbilityType(), toDrop);
                    Ability removed = mutableAbilityStore.removeAbility(toRemove, true);
                    if (removed != null) {
                        toDrop -= removed.getLevel();
                        itemStackStore.addAbility(removed, true);
                        player.addChatMessage(new TextComponentTranslation(L10NHelpers.localize("chat.everlastingabilities.playerLostAbility",
                                player.getName(),
                                removed.getAbilityType().getRarity().rarityColor.toString() + TextFormatting.BOLD + L10NHelpers.localize(removed.getAbilityType().getUnlocalizedName()) + TextFormatting.RESET,
                                removed.getLevel())));
                    }
                }
            }

            if (!itemStackStore.getAbilities().isEmpty()) {
                ItemStackHelpers.spawnItemStack(player.worldObj, player.getPosition(), itemStack);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        IMutableAbilityStore oldStore = event.getOriginal().getCapability(MutableAbilityStoreConfig.CAPABILITY, null);
        IMutableAbilityStore newStore = event.getEntityPlayer().getCapability(MutableAbilityStoreConfig.CAPABILITY, null);
        newStore.setAbilities(Maps.newHashMap(oldStore.getAbilitiesRaw()));
    }
    
}
