package org.valkyrienskies.mod.mixin.mod_compat.sodium;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTracker;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkTracker.class, remap = false)
public class MixinChunkTracker {

    @Shadow
    @Final
    private Long2IntOpenHashMap chunkStatus;

    @Inject(method = "onChunkStatusAdded", at = @At("HEAD"), cancellable = true)
    private void cancelDataLight(int x, int z, int flags, CallbackInfo ci) {
        if (flags == ChunkStatus.FLAG_HAS_LIGHT_DATA || flags == ChunkStatus.FLAG_ALL) {
            final long key = ChunkPos.asLong(x, z);
            final int existingFlags = this.chunkStatus.get(key);
            if ((existingFlags & 1) == 0) {
                ci.cancel(); // Cancel instead of throwing an error for now
            }
        }
    }
}
