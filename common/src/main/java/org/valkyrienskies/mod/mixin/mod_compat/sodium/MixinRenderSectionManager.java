package org.valkyrienskies.mod.mixin.mod_compat.sodium;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.mixinducks.mod_compat.sodium.RenderSectionManagerDuck;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class MixinRenderSectionManager implements RenderSectionManagerDuck {

    @Unique
    private final WeakHashMap<ClientShip, ChunkRenderListIterable> shipRenderLists = new WeakHashMap<>();

    @Override
    public WeakHashMap<ClientShip, ChunkRenderListIterable> getShipRenderLists() {
        return shipRenderLists;
    }

    @Shadow
    @Final
    private ClientLevel world;

    @Shadow
    protected abstract RenderSection getRenderSection(int x, int y, int z);

    @Shadow
    private @NotNull Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    @Shadow
    @Final
    private ReferenceSet<RenderSection> sectionsWithGlobalEntities;

    @Shadow
    private @Nullable BlockPos lastCameraPosition;

    @Shadow
    protected abstract float getSearchDistance();

    @Inject(at = @At("TAIL"), method = "update")
    private void afterIterateChunks(Camera camera, Viewport viewport, int frame, boolean spectator, CallbackInfo ci) {
        for (final ClientShip ship : VSGameUtilsKt.getShipObjectWorld(Minecraft.getInstance()).getLoadedShips()) {
            ship.getActiveChunksSet().forEach((x, z) -> {
                for (int y = world.getMinSection(); y < world.getMaxSection(); y++) {
                    final RenderSection section = getRenderSection(x, y, z);

                    if (section == null) {
                        continue;
                    }

                    if (section.getPendingUpdate() != null) {
                        final ArrayDeque<RenderSection> queue = this.rebuildLists.get(section.getPendingUpdate());
                        if (queue.size() < (2 << 4) - 1) {
                            queue.push(section);
                        }
                    }

                    if (section.getRegion().isEmpty()) {
                        if (OcclusionCuller.isOutsideFrustum(viewport, section)) {
                            continue;
                        }
                    }

                    shipRenderLists.computeIfAbsent(ship, k -> {
                        ChunkRenderList list = new ChunkRenderList(section.getRegion());
                        list.add(section);
                        // TODO Is this a valid cast?
                        return (ChunkRenderListIterable) list.sectionsWithEntitiesIterator();
                    });

                    if (section.isBuilt()) {
                        sectionsWithGlobalEntities.add(section);
                    }

                    // TODO Is this needed?
                    // addEntitiesToRenderLists(section);
                }
            });
        }
    }

    @Redirect(
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSectionManager;shouldPrioritizeRebuild(Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;)Z"
        ),
        method = "scheduleRebuild"
    )
    private boolean redirectIsChunkPrioritized(final RenderSectionManager instance, final RenderSection render) {
        if (this.lastCameraPosition == null) return false;

        return VSGameUtilsKt.squaredDistanceBetweenInclShips(world,
            render.getOriginX() + 8, render.getOriginY() + 8, render.getOriginZ() + 8,
            this.lastCameraPosition.getX(), this.lastCameraPosition.getY(), this.lastCameraPosition.getZ()) <= this.getSearchDistance();
    }

    @Inject(at = @At("TAIL"), method = "resetRenderLists")
    private void afterResetLists(final CallbackInfo ci) {
        shipRenderLists.replaceAll((s, v) -> SortedRenderLists.empty());
    }
}
