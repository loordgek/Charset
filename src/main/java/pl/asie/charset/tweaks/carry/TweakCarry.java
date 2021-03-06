package pl.asie.charset.tweaks.carry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.commons.lang3.tuple.Pair;
import pl.asie.charset.lib.CharsetIMC;
import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.capability.CapabilityProviderFactory;
import pl.asie.charset.tweaks.ModCharsetTweaks;
import pl.asie.charset.tweaks.Tweak;
import pl.asie.charset.tweaks.carry.transforms.CarryTransformerEntityMinecart;
import pl.asie.charset.tweaks.carry.transforms.CarryTransformerEntityMinecartDayBarrel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TweakCarry extends Tweak {
    public static final ResourceLocation CAP_IDENTIFIER = new ResourceLocation("charsettweaks:carry");

    @CapabilityInject(CarryHandler.class)
    public static Capability<CarryHandler> CAPABILITY;

    private static Set<String> whitelist = new HashSet<>();
    private static Set<String> blacklist = new HashSet<>();

    public TweakCarry() {
        super("tweaks", "blockCarrying", "Allow players to carry blocks by shift-pickblock.", true);
    }

    @Override
    protected void initConfig(Configuration config) {
        super.initConfig(config);

        whitelist.clear();
        blacklist.clear();
        for (String s : config.get("blockCarry", "whitelist", new String[]{}, "Accepts block/tile entity registry names and classes.").getStringList())
            whitelist.add(s);
        for (String s : config.get("blockCarry", "blacklist", new String[]{}, "Accepts block/tile entity registry names and classes.").getStringList())
            blacklist.add(s);
    }

    @Override
    public boolean init() {
        CarryTransformerRegistry.INSTANCE.registerEntityTransformer(new CarryTransformerEntityMinecart());
        if (Loader.isModLoaded("charsetstorage")) {
            CarryTransformerRegistry.INSTANCE.registerEntityTransformer(new CarryTransformerEntityMinecartDayBarrel());
        }

        CarryHandler.register();

        MinecraftForge.EVENT_BUS.register(new TweakCarryEventHandler());
        if (ModCharsetLib.proxy.isClient()) {
            MinecraftForge.EVENT_BUS.register(new TweakCarryRender());
        }

        return true;
    }

    protected static boolean canCarry(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if ("minecraft".equals(state.getBlock().getRegistryName().getResourceDomain())) {
            return true;
        }

        boolean hasTileEntity = state.getBlock().hasTileEntity(state);

        Set<String> names = new HashSet<>();
        Set<ResourceLocation> locs = new HashSet<>();

        locs.add(state.getBlock().getRegistryName());
        names.add(state.getBlock().getClass().getName());

        if (hasTileEntity) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                Class<? extends TileEntity> tileClass = tile.getClass();
                locs.add(TileEntity.getKey(tileClass));
                names.add(tileClass.getName());
            }
        }

        for (ResourceLocation r : locs)
            names.add(r.toString());

        CharsetIMC.Result allowedIMC = CharsetIMC.INSTANCE.allows("carry", locs);

        if (!Collections.disjoint(blacklist, names) || allowedIMC == CharsetIMC.Result.NO) {
            return false;
        } else if (!hasTileEntity) {
            return true;
        } else {
            return !Collections.disjoint(whitelist, names) || allowedIMC == CharsetIMC.Result.YES;
        }
    }

    protected static boolean canCarry(Entity entity) {
        Class<? extends Entity> entityClass = entity.getClass();
        String name = EntityRegistry.getEntry(entityClass).getName();
        String clsName = entityClass.getName();

        return !blacklist.contains(name) && !blacklist.contains(clsName);
    }

    public static void grabBlock(EntityPlayer player, World world, BlockPos pos) {
        if (!(player instanceof EntityPlayerMP)) {
            ModCharsetTweaks.packet.sendToServer(new PacketCarryGrab(world, pos));
        }

        CarryHandler carryHandler = player.getCapability(TweakCarry.CAPABILITY, null);
        if (carryHandler != null && !carryHandler.isCarrying()) {
            if (canCarry(world, pos)) {
                carryHandler.grab(world, pos);
            } else {
                // Sync in case the client said "yes".
                syncCarryWithClient(player, player);
            }
        }
    }

    public static void grabEntity(EntityPlayer player, World world, Entity entity) {
        if (!(player instanceof EntityPlayerMP)) {
            ModCharsetTweaks.packet.sendToServer(new PacketCarryGrab(world, entity));
        }

        CarryHandler carryHandler = player.getCapability(TweakCarry.CAPABILITY, null);
        if (carryHandler != null && !carryHandler.isCarrying()) {
            if (canCarry(entity)) {
                for (ICarryTransformer<Entity> transformer : CarryTransformerRegistry.INSTANCE.getEntityTransformers()) {
                    if (transformer.extract(entity, true) != null) {
                        Pair<IBlockState, TileEntity> pair = transformer.extract(entity, false);
                        carryHandler.put(pair.getLeft(), pair.getRight());
                        return;
                    }
                }
            } else {
                // Sync in case the client said "yes".
                syncCarryWithClient(player, player);
            }
        }
    }

    protected static boolean dropCarriedBlock(EntityLivingBase entity, boolean must) {
        return dropCarriedBlock(entity, must, (must ? 4 : 2));
    }

    protected static boolean dropCarriedBlock(EntityLivingBase entity, boolean must, int maxRadius) {
        CarryHandler carryHandler = entity.getCapability(CAPABILITY, null);
        if (carryHandler != null && carryHandler.isCarrying()) {
            World world = entity.getEntityWorld();
            if (world.isRemote) {
                carryHandler.empty();
                return true;
            }

            BlockPos base = entity.getPosition();
            for (int method = 0; method <= (must ? 2 : 1); method++) {
                for (int radius = 0; radius <= maxRadius; radius++) {
                    Vec3i radiusVec = new Vec3i(radius, radius, radius);
                    for (BlockPos pos : BlockPos.getAllInBoxMutable(base.subtract(radiusVec), base.add(radiusVec))) {
                        if (world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                                && (method > 1 || !world.isAirBlock(pos.down()))
                                && (method > 0 || world.isSideSolid(pos.down(), EnumFacing.UP))
                                ) {
                            carryHandler.place(world, pos.toImmutable(), EnumFacing.UP);
                        }
                        if (!carryHandler.isCarrying()) break;
                    }
                    if (!carryHandler.isCarrying()) break;
                }
                if (!carryHandler.isCarrying()) break;
            }

            if (carryHandler.isCarrying()) {
                if (must) {
                    ModCharsetLib.logger.error("Could not drop carried block from player " + entity.getName() + "! This is a bug!");
                }
                return false;
            } else {
                if (entity instanceof EntityPlayer) {
                    TweakCarry.syncCarryWithClient(entity, (EntityPlayer) entity);
                }
                return true;
            }
        } else {
            return true;
        }
    }

    protected static void syncCarryWithClient(Entity who, EntityPlayer target) {
        if (who instanceof EntityPlayerMP && who.hasCapability(CAPABILITY, null)) {
            ModCharsetTweaks.packet.sendTo(new PacketCarrySync(who), target);
        }
    }
}
