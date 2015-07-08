package projectmp.server;

import java.io.File;
import java.io.IOException;

import projectmp.common.Main;
import projectmp.common.block.Blocks;
import projectmp.common.chunk.Chunk;
import projectmp.common.entity.Entity;
import projectmp.common.entity.EntityPlayer;
import projectmp.common.io.WorldNBTIO;
import projectmp.common.io.WorldSavingLoading;
import projectmp.common.packet.PacketBeginChunkTransfer;
import projectmp.common.packet.PacketEntities;
import projectmp.common.packet.PacketGuiState;
import projectmp.common.packet.PacketPositionUpdate;
import projectmp.common.packet.PacketSendChunk;
import projectmp.common.packet.repository.PacketRepository;
import projectmp.common.world.ServerWorld;
import projectmp.server.player.ServerPlayer;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

public class ServerLogic {

	public boolean isSingleplayer = false;

	public Main main;
	public Server server;

	public ServerWorld world = null;

	public int maxplayers = 2;

	public Array<ServerPlayer> players = new Array<>();

	public ServerLogic(Main m) {
		main = m;
		server = main.server;

		world = new ServerWorld(main, 1024, 512, System.nanoTime(), this);
		new Thread() {

			@Override
			public void run() {
				long ms = System.currentTimeMillis();
				Main.logger.debug("beginning generation");
				world.setSendingUpdates(false);
				world.generate();
				world.setSendingUpdates(true);
				Main.logger.debug("finished generating; took " + (System.currentTimeMillis() - ms)
						+ " ms");
				world.lightingEngine.updateLighting(0, 0, world.sizex, world.sizey);
				Main.logger.debug("Lighting update for entire world on init took "
						+ (world.lightingEngine.getLastUpdateLength() / 1000000f) + " ms");

				//				try {
				//					byte[] worldBytes = WorldSavingLoading.loadWorld(new File("saves/save0/world.dat"));
				//					world = (ServerWorld) WorldNBTIO.decode(world, worldBytes);
				//				} catch (IOException e) {
				//					e.printStackTrace();
				//				}

				try {
					new File("saves/save0/").mkdirs();
					File f = new File("saves/save0/world.dat");
					f.createNewFile();

					byte[] worldBytes = WorldNBTIO.encode(world);
					WorldSavingLoading.saveWorld(WorldNBTIO.encode(world), f);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}.start();
	}

	public void tickUpdate() {
		world.tickUpdate();

		if (server.getConnections().length > 0 && world.getNumberOfEntities() > 0) {
			PacketPositionUpdate positionUpdate = PacketRepository.instance().positionUpdate;

			if (positionUpdate.entityid.length < world.getNumberOfEntities()
					|| Math.abs(positionUpdate.entityid.length - world.getNumberOfEntities()) >= 32) {
				positionUpdate.resetTables(world.getNumberOfEntities());
			}

			boolean shouldSend = false;
			int iter = 0;
			for (int i = 0; i < world.getNumberOfEntities(); i++) {
				Entity e = world.getEntityByIndex(i);

				if (!e.hasMovedLastTick()) continue;
				positionUpdate.entityid[iter] = e.uuid;
				positionUpdate.x[iter] = e.x;
				positionUpdate.y[iter] = e.y;
				positionUpdate.velox[iter] = e.velox;
				positionUpdate.veloy[iter] = e.veloy;
				shouldSend = true;

				iter++;
			}

			if (shouldSend) server.sendToAllUDP(positionUpdate);
		}
	}

	public void save(String folder) throws IOException {
		new File(folder).mkdirs();
		
		File f = new File(folder + "world.dat");
		f.createNewFile();

		byte[] worldBytes = WorldNBTIO.encode(world);
		WorldSavingLoading.saveWorld(WorldNBTIO.encode(world), f);
		
		f = new File(folder + "players.dat");
		f.createNewFile();
	}

	public void sendEntireWorld(Connection connection) {
		Array<PacketSendChunk> queue = new Array<PacketSendChunk>(Math.max(1, world.sizex / 16)
				+ Math.max(1, world.sizey / 16));

		for (int x = 0; x < world.getWidthInChunks(); x++) {
			for (int y = 0; y < world.getHeightInChunks(); y++) {
				PacketSendChunk packet = new PacketSendChunk();

				packet.originx = x * Chunk.CHUNK_SIZE;
				packet.originy = y * Chunk.CHUNK_SIZE;

				for (int j = 0; j < Chunk.CHUNK_SIZE; j++) {
					for (int k = 0; k < Chunk.CHUNK_SIZE; k++) {
						Chunk currentChunk = world.getChunk(x, y);

						packet.blocks[j][k] = Blocks.instance().getKey(
								currentChunk.getChunkBlock(j, k));
						packet.meta[j][k] = currentChunk.getChunkMeta(j, k);
						packet.tileEntities[j][k] = currentChunk.getChunkTileEntity(j, k);
					}
				}

				queue.add(packet);
			}
		}

		connection.sendTCP(new PacketBeginChunkTransfer().setPercentage(1.0f / queue.size)
				.setSingleplayer(isSingleplayer));

		connection.addListener(new ChunkQueueSender(queue, connection));
	}

	public void sendEntities(Connection connection) {
		if (world.getNumberOfEntities() > 0) {
			PacketEntities packet = new PacketEntities();
			packet.entities = new Entity[world.getNumberOfEntities()];
			for (int i = 0; i < packet.entities.length; i++) {
				packet.entities[i] = world.getEntityByIndex(i);
			}

			connection.sendTCP(packet);
		}
	}

	public int getConnectionIDByName(String name) {
		for (int i = 0; i < server.getConnections().length; i++) {
			if (server.getConnections()[i].toString().equals(name)) return server.getConnections()[i]
					.getID();
		}
		return -1;
	}

	public String getConnectionNameByID(int id) {
		for (int i = 0; i < server.getConnections().length; i++) {
			if (server.getConnections()[i].getID() == id) return server.getConnections()[i]
					.toString();
		}
		return null;
	}

	public EntityPlayer getPlayerByName(String name) {
		for (int i = 0; i < world.getNumberOfEntities(); i++) {
			Entity e = world.getEntityByIndex(i);
			if (e instanceof EntityPlayer) {
				if (((EntityPlayer) e).username.equals(name)) {
					return (EntityPlayer) e;
				}
			}
		}

		return null;
	}

	protected void removePlayer(String name) {
		EntityPlayer p = getPlayerByName(name);

		if (p != null) {
			world.removeEntity(p.uuid);
		}
	}

	public void sendGuiState(EntityPlayer player, String guiId, int x, int y, boolean shouldOpen) {
		PacketGuiState guiStatePacket = PacketRepository.instance().guiState;

		guiStatePacket.guiId = guiId;
		guiStatePacket.shouldOpen = shouldOpen;
		guiStatePacket.x = x;
		guiStatePacket.y = y;

		server.sendToTCP(getConnectionIDByName(player.username), guiStatePacket);
	}

	public void openGui(EntityPlayer player, String guiId, int x, int y) {
		sendGuiState(player, guiId, x, y, true);
	}

	public void closeGui(EntityPlayer player, String guiId, int x, int y) {
		sendGuiState(player, guiId, x, y, false);
	}

}
