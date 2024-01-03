package org.valkyrienskies.mod.mixin.mod_compat.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.mixinducks.mod_compat.sodium.RegionChunkRendererDuck;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public class MixinDefaultChunkRenderer implements RegionChunkRendererDuck {

    @Unique
    private static final Vector3d camInWorld = new Vector3d();

    @Unique
    private static final Vector3d camInShip = new Vector3d();


    @Redirect(method = "fillCommandBuffer", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;getVisibleFaces(IIIIII)I"))
    private static int redirectGetVisibleFaces(int cameraX, int cameraY, int cameraZ, int chunkX, int chunkY, int chunkZ, @Local(ordinal = 0) RenderRegion regionRegion, @Local(ordinal = 0) int sectionIndex) {

        final ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos(Minecraft.getInstance().level,
            chunkX, chunkZ);

        if (ship != null) {
            ship.getRenderTransform().getWorldToShip().transformPosition(camInWorld, camInShip);
            /*
            final ChunkRenderBounds originalBounds = getBounds.call(section);
            return new ChunkRenderBounds(originalBounds.x1 - 1.9f, originalBounds.y1 - 1.9f,
                originalBounds.z1 - 1.9f, originalBounds.x2 + 1.9f, originalBounds.y2 + 1.9f,
                originalBounds.z2 + 1.9f);

             */
        } else {
            camInShip.set(camInWorld);
            //return getBounds.call(section);
        }

        return ModelQuadFacing.ALL;
    }



    @Override
    public void setCameraForCulling(double x, double y, double z) {
        camInWorld.set(x, y, z);
    }
}
