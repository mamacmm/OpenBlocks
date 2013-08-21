package openblocks.common.tileentity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import openblocks.OpenBlocks;
import openblocks.OpenBlocks.Config;

public class TileEntityElevator extends OpenTileEntity {

	private int lowerLevel = 0;
	private int upperLevel = 0;

	/**
	 * How far a player must be looking in a direction to be teleported
	 */
	private static final float DIRECTION_MAGNITUDE = 0.95f;

	private HashMap<String, Integer> cooldown = new HashMap<String, Integer>();

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (!worldObj.isRemote) {

			Iterator<Entry<String, Integer>> cooldownIter = cooldown.entrySet().iterator();
			while (cooldownIter.hasNext()) {
				Entry<String, Integer> entry = cooldownIter.next();
				int less = entry.getValue() - 1;
				entry.setValue(less);
				if (less == 0) {
					cooldownIter.remove();
				}
			}

			List<EntityPlayer> playersInRange = (List<EntityPlayer>)worldObj.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getAABBPool().getAABB(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 3, zCoord + 1));

			if (playersInRange.size() > 0) {
				ForgeDirection teleportDirection = ForgeDirection.UNKNOWN;

				for (EntityPlayer player : playersInRange) {
					if (cooldown.containsKey(player.username)) {
						continue;
					}
					/*
					 * Don't activate when a player is flying around in
					 * creative, it's annoying
					 */

					if (player.capabilities.isCreativeMode
							&& player.capabilities.isFlying) continue;
					if (player.isSneaking()
							&& player.ridingEntity == null
							&& (!Config.elevatorBlockMustFaceDirection || player.getLookVec().yCoord < -DIRECTION_MAGNITUDE)) {
						teleportDirection = ForgeDirection.DOWN;
						/* player.isJumping doesn't seem to work server side ? */
					} else if (player.posY > yCoord + 1.2
							&& player.ridingEntity == null
							&& (!Config.elevatorBlockMustFaceDirection || player.getLookVec().yCoord > DIRECTION_MAGNITUDE)) {
						teleportDirection = ForgeDirection.UP;
					}
					if (teleportDirection != ForgeDirection.UNKNOWN) {
						try {
							int level = findLevel(teleportDirection);
							if (level != 0) {
								player.setPositionAndUpdate(player.posX, level + 1.1, player.posZ);
								worldObj.playSoundAtEntity(player, "openblocks.teleport", 1F, 1F);
								addPlayerCooldownToTargetAndNeighbours(player, xCoord, level, zCoord);
							}
						} catch (Exception ex) {
							/* Teleport failed */
						}
					}

				}
			}

			lowerLevel = 0;
			upperLevel = 0;
		}

	}

	private void addPlayerCooldownToTargetAndNeighbours(EntityPlayer player, int xCoord, int level, int zCoord) {
		for (int x = xCoord - 1; x <= xCoord + 1; x++) {
			for (int z = zCoord - 1; z <= zCoord + 1; z++) {
				TileEntity targetTile = worldObj.getBlockTileEntity(x, level, z);
				if (targetTile instanceof TileEntityElevator) {
					((TileEntityElevator)targetTile).addPlayerCooldown(player);
				}
			}
		}
	}

	private void addPlayerCooldown(EntityPlayer player) {
		cooldown.put(player.username, 6);
	}
	
	private boolean isPassable(int x, int y, int z, boolean canStandHere) {
		int blockId = worldObj.getBlockId(x, y, z);
		if(canStandHere) {
			return worldObj.isAirBlock(x, y, z) || Block.blocksList[blockId] == null || ( OpenBlocks.Config.irregularBlocksArePassable && Block.blocksList[blockId].getCollisionBoundingBoxFromPool(worldObj,x,y,z) == null );
		} else {
			/* Ugly logic makes NC sad :( */
			return !(blockId == 0
					|| OpenBlocks.Config.elevatorMaxBlockPassCount == -1
					|| OpenBlocks.Config.elevatorIgnoreHalfBlocks
					&& !Block.isNormalCube(blockId));
		}
	}

	private int findLevel(ForgeDirection direction) throws Exception {
		if (direction != ForgeDirection.UP && direction != ForgeDirection.DOWN) { throw new Exception("Must be either up or down... for now"); }

		int blocksInTheWay = 0;
		for (int y = 2; y < Config.elevatorTravelDistance; y++) {
			int yPos = yCoord + (y * direction.offsetY);
			if (worldObj.blockExists(xCoord, yPos, zCoord)) {
				int blockId = worldObj.getBlockId(xCoord, yPos, zCoord);
				if (blockId == OpenBlocks.Config.blockElevatorId) {
					TileEntity otherBlock = worldObj.getBlockTileEntity(xCoord, yPos, zCoord);
					// Check that it is a drop block and that it has the same
					// color index.
					if (!(otherBlock instanceof TileEntityElevator)) continue;
					if (((TileEntityElevator)otherBlock).getBlockMetadata() != this.getBlockMetadata()) continue;

					if (isPassable(xCoord, yPos + 1, zCoord, true) && isPassable(xCoord, yPos + 2, zCoord, true)) { return yPos; }
					return 0;
				} else if (isPassable(xCoord, yPos, zCoord, false) && ++blocksInTheWay > OpenBlocks.Config.elevatorMaxBlockPassCount) { 
					return 0; 
				}
			} else {
				return 0;
			}
		}
		return 0;
	}

	public boolean onActivated(EntityPlayer player) {
		ItemStack stack = player.getHeldItem();
		if (stack != null) {
			Item item = stack.getItem();
			if (item instanceof ItemDye) {
				int dmg = stack.getItemDamage();
				// temp hack, dont tell anyone.
				if (dmg == 15) {
					dmg = 0;
				} else if (dmg == 0) {
					dmg = 15;
				}
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, dmg, 3);
				worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
				return true;
			}
		}
		return false; // Don't update the block and don't block placement if
						// it's not dye we're using
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub

	}

}
