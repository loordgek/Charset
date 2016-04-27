package pl.asie.charset.storage.locking;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;
import net.minecraft.world.World;
import pl.asie.charset.api.storage.IKeyItem;
import pl.asie.charset.storage.ModCharsetStorage;

public class EntityLock extends EntityHanging {
    private static final DataParameter<EnumFacing> HANGING_ROTATION = EntityDataManager.createKey(EntityLock.class, DataSerializers.FACING);
    private static final Predicate<Entity> IS_HANGING_ENTITY = new Predicate<Entity>() {
        public boolean apply(Entity entity) {
            return entity instanceof EntityHanging;
        }
    };

    private String lockKey = null;
    private String prefixedLockKey = null;
    private TileEntity tileCached;

    public EntityLock(World worldIn) {
        super(worldIn);
    }

    public EntityLock(World worldIn, ItemStack stack, BlockPos p_i45852_2_, EnumFacing p_i45852_3_) {
        super(worldIn, p_i45852_2_);
        this.setLockKey(((ItemLock) stack.getItem()).getRawKey(stack));
        this.updateFacingWithBoundingBox(p_i45852_3_);
    }

    private void setLockKey(String s) {
        this.lockKey = s;
        this.prefixedLockKey = s != null ? "charset:key:" + s : null;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setLockKey(compound.hasKey("key") ? compound.getString("key") : null);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (lockKey != null) {
            compound.setString("key", lockKey);
        }
    }

    public TileEntity getAttachedTile() {
        if (tileCached == null || tileCached.isInvalid()) {
            BlockPos pos = this.hangingPosition.offset(this.facingDirection.getOpposite());
            tileCached = worldObj.getTileEntity(pos);
        }

        return tileCached;
    }

    @Override
    protected void entityInit() {
        this.getDataManager().register(HANGING_ROTATION, null);
        this.setEntityInvulnerable(true);
    }

    @Override
    public boolean hitByEntity(Entity entityIn) {
        if (entityIn instanceof EntityPlayer && entityIn.isSneaking()) {
            if (!this.worldObj.isRemote) {
                ItemStack stack = ((EntityPlayer) entityIn).getHeldItemMainhand();
                if (stack == null || !(stack.getItem() instanceof ItemKey) || !(((ItemKey) stack.getItem()).canUnlock(prefixedLockKey, stack))) {
                    stack = ((EntityPlayer) entityIn).getHeldItemOffhand();
                    if (stack == null || !(stack.getItem() instanceof ItemKey) || !(((ItemKey) stack.getItem()).canUnlock(prefixedLockKey, stack))) {
                        return super.hitByEntity(entityIn);
                    }
                }

                if (!this.isDead) {
                    this.setDead();
                    this.onBroken(entityIn);
                }

                return true;
            } else {
                return super.hitByEntity(entityIn);
            }
        }

        return super.hitByEntity(entityIn);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (lockKey != null && lockKey.length() > 0) {
            if (getAttachedTile() instanceof ILockableContainer) {
                ILockableContainer container = (ILockableContainer) tileCached;
                if (!container.isLocked() || !prefixedLockKey.equals(container.getLockCode().getLock())) {
                    container.setLockCode(new LockCode(prefixedLockKey));
                }
            }
        }
    }

    @Override
    protected void updateFacingWithBoundingBox(EnumFacing facingDirectionIn) {
        super.updateFacingWithBoundingBox(facingDirectionIn);
        this.getDataManager().set(HANGING_ROTATION, facingDirectionIn);
        this.getDataManager().setDirty(HANGING_ROTATION);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (key == HANGING_ROTATION) {
            EnumFacing facing = getDataManager().get(HANGING_ROTATION);
            if (facing != null) {
                this.updateFacingWithBoundingBox(facing);
            }
        }
    }

    @Override
    public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, ItemStack stack, EnumHand hand) {
        if (lockKey != null) {
            boolean canUnlock = false;
            if (stack != null && stack.getItem() instanceof IKeyItem) {
                IKeyItem key = (IKeyItem) stack.getItem();
                canUnlock = key.canUnlock(prefixedLockKey, stack);
            }

            if (getAttachedTile() instanceof ILockableContainer) {
                ILockableContainer container = (ILockableContainer) tileCached;
                if (canUnlock) {
                    unlockContainer();
                }

                BlockPos pos = this.hangingPosition.offset(this.facingDirection.getOpposite());
                IBlockState state = worldObj.getBlockState(pos);
                if (state.getBlock().onBlockActivated(worldObj, pos, state, player, hand, stack, this.facingDirection,
                        0.5F + this.facingDirection.getFrontOffsetX() * 0.5F,
                        0.5F + this.facingDirection.getFrontOffsetY() * 0.5F,
                        0.5F + this.facingDirection.getFrontOffsetZ() * 0.5F
                )) {
                    if (canUnlock) {
                        container.setLockCode(new LockCode(prefixedLockKey));
                    }
                    return EnumActionResult.SUCCESS;
                } else {
                    if (canUnlock) {
                        container.setLockCode(new LockCode(prefixedLockKey));
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public float getCollisionBorderSize()
    {
        return 0.0F;
    }

    @Override
    public boolean onValidSurface() {
        if (!this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            return false;
        } else {
            BlockPos pos = this.hangingPosition.offset(this.facingDirection.getOpposite());
            TileEntity tile = worldObj.getTileEntity(pos);

            if (!(tile instanceof ILockableContainer)) {
                return false;
            }

            return this.worldObj.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox(), IS_HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public int getWidthPixels() {
        return 8;
    }

    @Override
    public int getHeightPixels() {
        return 8;
    }

    private void unlockContainer() {
        if (getAttachedTile() instanceof ILockableContainer) {
            unlockContainer((ILockableContainer) tileCached);
        }

        if (tileCached instanceof TileEntityChest) {
            // working around vanilla bugs as usual, this time large chests
            // seem to enjoy syncing codes but only when InventoryLargeChest
            // is instantiated, so we have to undo it ourselves.
            ((TileEntityChest) tileCached).checkForAdjacentChests();
            if (((TileEntityChest) tileCached).adjacentChestXNeg != null) {
                unlockContainer(((TileEntityChest) tileCached).adjacentChestXNeg);
            }
            if (((TileEntityChest) tileCached).adjacentChestXPos != null) {
                unlockContainer(((TileEntityChest) tileCached).adjacentChestXPos);
            }
            if (((TileEntityChest) tileCached).adjacentChestZNeg != null) {
                unlockContainer(((TileEntityChest) tileCached).adjacentChestZNeg);
            }
            if (((TileEntityChest) tileCached).adjacentChestZPos != null) {
                unlockContainer(((TileEntityChest) tileCached).adjacentChestZPos);
            }
        }
    }


    private void unlockContainer(ILockableContainer container) {
        if (container.isLocked()) {
            if (lockKey != null) {
                if (!prefixedLockKey.equals(container.getLockCode().getLock())) {
                    return;
                }
            }

            container.setLockCode(null);
        }
    }

    @Override
    public void onBroken(Entity brokenEntity) {
        unlockContainer();
        ItemStack lock = new ItemStack(ModCharsetStorage.lockItem);
        if (lockKey != null) {
            lock.setTagCompound(new NBTTagCompound());
            lock.getTagCompound().setString("key", lockKey);
        }
        this.entityDropItem(lock, 0.0F);
    }

    @Override
    public void playPlaceSound() {

    }
}
