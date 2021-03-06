package pl.asie.charset.pipes.pipe;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.charset.lib.utils.RayTraceUtils;
import pl.asie.charset.lib.utils.RotationUtils;
import pl.asie.charset.pipes.PipeUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockPipe extends BlockContainer {
	private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[7];
	private AxisAlignedBB lastSelectionBox = BOXES[6];

	static {
		BOXES[6] = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
		for (int i = 0; i < 6; i++) {
			BOXES[i] = RotationUtils.rotateFace(new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.25, 0.75), EnumFacing.getFront(i));
		}
	}

	public BlockPipe() {
		super(Material.GLASS);
		setUnlocalizedName("charset.pipe");
		setHardness(0.3f);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[]{}, new IUnlistedProperty[]{TilePipe.PROPERTY});
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		TilePipe pipe = PipeUtils.getPipe(world, pos, null);
		IExtendedBlockState extendedBS = (IExtendedBlockState) super.getExtendedState(state, world, pos);
		if (pipe != null) {
			return extendedBS.withProperty(TilePipe.PROPERTY, pipe);
		} else {
			return extendedBS;
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
		TilePipe pipe = PipeUtils.getPipe(world, pos, null);
		if (pipe != null) {
			pipe.onNeighborBlockChange(neighborBlock, neighborPos);
		}
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TilePipe();
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185477_7_) {
		TilePipe tilePipe = PipeUtils.getPipe(worldIn, pos, null);
		if (tilePipe != null) {
			for (int i = 0; i < 7; i++) {
				if (i == 6 || tilePipe.connects(EnumFacing.getFront(i))) {
					AxisAlignedBB box = BOXES[i].offset(pos);
					if (entityBox.intersectsWith(box))
						collidingBoxes.add(box);
				}
			}
		}
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
		return lastSelectionBox.offset(pos);
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
		List<AxisAlignedBB> boxes = new ArrayList<>();

		TilePipe pipe = PipeUtils.getPipe(world, pos, null);
		for (int i = 0; i < 7; i++) {
			if (i == 6 || (pipe != null && pipe.connects(EnumFacing.getFront(i)))) {
				boxes.add(BOXES[i]);
			}
		}

		RayTraceUtils.Result raytraceresult = RayTraceUtils.getCollision(world, pos, start, end, boxes);
		if (raytraceresult != null) {
			lastSelectionBox = raytraceresult.box == null ? BOXES[6] : raytraceresult.box;
			return raytraceresult.hit == null ? null : new RayTraceResult(raytraceresult.hit.hitVec.addVector((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), raytraceresult.hit.sideHit, pos);
		} else {
			return null;
		}
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		return true;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
}
