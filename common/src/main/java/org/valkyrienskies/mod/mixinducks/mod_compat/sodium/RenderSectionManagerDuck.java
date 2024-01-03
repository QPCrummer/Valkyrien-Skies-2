package org.valkyrienskies.mod.mixinducks.mod_compat.sodium;

import java.util.WeakHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import org.valkyrienskies.core.api.ships.ClientShip;

public interface RenderSectionManagerDuck {

    WeakHashMap<ClientShip, ChunkRenderListIterable> getShipRenderLists();

}
