package projectmp.common.entity;

import projectmp.client.WorldRenderer;
import projectmp.common.world.World;


public class EntityPlayer extends Entity{

	public String username = "UNKNOWN PLAYER NAME RAWR";
	
	public EntityPlayer(World w, float posx, float posy) {
		super(w, posx, posy);
	}

	@Override
	public void prepare() {
	}

	@Override
	public void render(WorldRenderer renderer) {
	}

}
