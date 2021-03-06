package openblocks.common.tileentity;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import openblocks.common.TrophyHandler.Trophy;
import openmods.api.IAwareTileLite;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncableInt;
import openmods.tileentity.SyncedTileEntity;

import com.google.common.base.Preconditions;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityTrophy extends SyncedTileEntity implements IAwareTileLite {

	public static Trophy debugTrophy = Trophy.Wolf;
	private int sinceLastActivate = 0;
	private SyncableInt trophyIndex;

	public TileEntityTrophy() {}

	@Override
	protected void createSyncedFields() {
		trophyIndex = new SyncableInt();
	}

	public Trophy getTrophy() {
		Trophy t = Trophy.values()[trophyIndex.getValue()];
		if (t != null) { return t; }
		return t;
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!worldObj.isRemote) {
			Trophy trophy = getTrophy();
			if (trophy != null) {
				trophy.executeTickBehavior(this);
			}
			if (sinceLastActivate < Integer.MAX_VALUE) {
				sinceLastActivate++;
			}
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!worldObj.isRemote) {
			Trophy trophyType = getTrophy();
			if (trophyType != null) {
				trophyType.playSound(worldObj, xCoord, yCoord, zCoord);
				trophyType.executeActivateBehavior(this, player);
			}
		}
		return true;
	}

	@Override
	public void onBlockPlacedBy(EntityPlayer player, ForgeDirection side, ItemStack stack, float hitX, float hitY, float hitZ) {
		boolean set = false;
		if (stack.hasTagCompound()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag.hasKey("entity")) {
				String entityKey = tag.getString("entity");
				trophyIndex.setValue(Trophy.valueOf(entityKey).ordinal());
				set = true;
			}
		}
		if (!set) {
			int next = (debugTrophy.ordinal() + 1) % Trophy.values().length;
			debugTrophy = Trophy.values()[next];
			trophyIndex.setValue(debugTrophy.ordinal());
		}
		if (!worldObj.isRemote) {
			sync();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (tag.hasKey("sinceLastActivate")) {
			sinceLastActivate = tag.getInteger("sinceLastActivate");
		}
	}

	public int sinceLastActivate() {
		return sinceLastActivate;
	}

	public void resetActivationTimer() {
		sinceLastActivate = 0;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("sinceLastActivate", sinceLastActivate);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void prepareForInventoryRender(Block block, int metadata) {
		Preconditions.checkElementIndex(metadata, Trophy.VALUES.length);
		super.prepareForInventoryRender(block, metadata);
		trophyIndex.setValue(metadata);
	}

	@Override
	public void onSynced(Set<ISyncableObject> changes) {}

}
