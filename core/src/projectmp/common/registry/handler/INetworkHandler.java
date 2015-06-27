package projectmp.common.registry.handler;

import com.esotericsoftware.kryo.Kryo;

/**
 * The game will call registerClasses when it's registering the things
 * 
 *
 */
public interface INetworkHandler {
	
	public void registerClasses(Kryo kryo);
	
}
