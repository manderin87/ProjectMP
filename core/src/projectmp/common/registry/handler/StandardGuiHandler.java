package projectmp.common.registry.handler;

import projectmp.common.entity.EntityPlayer;
import projectmp.common.inventory.Inventory;
import projectmp.common.inventory.InventoryChessboard;
import projectmp.common.inventory.InventoryPlayer;
import projectmp.common.inventory.gui.Gui;
import projectmp.common.inventory.gui.GuiChessboard;
import projectmp.common.inventory.gui.GuiPlayerInventory;
import projectmp.common.tileentity.HasInventory;
import projectmp.common.util.Utils;
import projectmp.common.world.World;


public class StandardGuiHandler implements IGuiHandler{

	@Override
	public Gui getGuiObject(String id, World world, InventoryPlayer player, int x, int y) {
		switch(id){
		case("playerInv"):
			return new GuiPlayerInventory(world, player);
		case("chessboard"):
			return new GuiChessboard(world, player, id, x, y);
		default:
			return null;
		}
	}

	@Override
	public Inventory getInventoryObject(String id, World world, int x, int y) {
		switch(id){
		case("playerInv"):
			return ((EntityPlayer) (world.getEntityByUUID(Utils.packLong(x, y)))).getInventoryObject();
		case("chessboard"):
			return (InventoryChessboard) (((HasInventory) (world.getTileEntity(x, y))).getInventoryObject());
		default:
			return null;
		}
	}
	
}
