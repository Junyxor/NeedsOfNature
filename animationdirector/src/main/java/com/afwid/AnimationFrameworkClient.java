/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.util.Formatting
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.util.hit.HitResult
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.option.KeyBinding
 *  net.minecraft.client.option.KeyBinding$Category
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.util.InputUtil
 *  net.minecraft.client.util.InputUtil$Type
 *  net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.client.option.Perspective
 *  net.minecraft.client.network.ClientPlayerEntity
 *  net.minecraft.registry.Registries
 *  net.minecraft.network.packet.CustomPayload
 */
package com.afwid;

import com.afwid.AfwDebugChatCategory;
import com.afwid.client.camera.AfwAnimatedCameraPoseTracker;
import com.afwid.client.camera.AfwAnimationCameraZoom;
import com.afwid.client.config.AfwClientConfig;
import com.afwid.client.diagnostics.AfwAnimationAssetDiagnostics;
import com.afwid.client.render.gecko.AfwBoneTextureOverrides;
import com.afwid.client.render.gecko.AfwVanillaTextureResolver;
import com.afwid.client.runtime.AfwClientAnimationRuntime;
import com.afwid.client.sound.AfwSoundEffects;
import com.afwid.network.AdjustAnimationSpeedC2SPayload;
import com.afwid.network.AdvanceAnimationStageS2CPayload;
import com.afwid.network.AnimationSpeedUpdateS2CPayload;
import com.afwid.network.DebugAdvanceStageC2SPayload;
import com.afwid.network.DebugChatPreferenceC2SPayload;
import com.afwid.network.DebugJoinAnimationC2SPayload;
import com.afwid.network.DebugStartAnimationC2SPayload;
import com.afwid.network.DebugStopAllAnimationsC2SPayload;
import com.afwid.network.DebugStopAnimationC2SPayload;
import com.afwid.network.StartAnimationS2CPayload;
import com.afwid.network.StopAllAnimationsS2CPayload;
import com.afwid.network.StopAnimationS2CPayload;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.CustomPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Mod(value = AnimationFramework.MOD_ID, dist = Dist.CLIENT)
public class AnimationFrameworkClient {
    private static final String KEY_CATEGORY = "key.categories.animationframework";
    private static final KeyBinding SELECT_ACTOR_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.select_actor");
    private static final KeyBinding SELECT_SELF_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.select_self");
    private static final KeyBinding START_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.start_debug_animation");
    private static final KeyBinding STOP_ALL_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.stop_all_debug");
    private static final KeyBinding STOP_INSTANCE_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.stop_instance_debug");
    private static final KeyBinding NEXT_STAGE_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.next_stage_debug");
    private static final KeyBinding SPEED_UP_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.speed_up_debug");
    private static final KeyBinding SPEED_DOWN_DEBUG_KEY = AnimationFrameworkClient.createUnboundDebugKey("key.animationframework.speed_down_debug");
    private static final IntArrayList SELECTED_ACTOR_IDS = new IntArrayList();
    private static UUID SELECTED_INSTANCE_ID = null;
    private static boolean wasSelfAnimating = false;
    private static boolean ownsCameraPerspective = false;
    private static Perspective preAnimationPerspective = null;
    private static final double PLAYER_ANCHOR_EPSILON_SQ = 4.0E-4;

    private static boolean shouldShow(AfwDebugChatCategory category) {
        return AfwClientConfig.get().allowsDebugChat(category);
    }

    public static void sendClientDebugChat(AfwDebugChatCategory category, Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        if (!AnimationFrameworkClient.shouldShow(category)) {
            return;
        }
        client.player.sendMessage((Text)message.copy().formatted(AnimationFrameworkClient.chatColor(category)), false);
    }

    public static void sendClientSetupWarning(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        AnimationFrameworkClient.sendClientDebugChat(AfwDebugChatCategory.SETUP, (Text)Text.literal((String)message));
    }

    private static KeyBinding createUnboundDebugKey(String translationKey) {
        return new KeyBinding(translationKey, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), KEY_CATEGORY);
    }

    public AnimationFrameworkClient(IEventBus modEventBus) {
        AfwClientConfig.reload();
        modEventBus.addListener(AnimationFrameworkClient::registerKeyMappings);
        modEventBus.addListener(AnimationFrameworkClient::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(AnimationFrameworkClient::onClientTickEvent);
        NeoForge.EVENT_BUS.addListener(AnimationFrameworkClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(AnimationFrameworkClient::onLoggingOut);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SELECT_ACTOR_KEY);
        event.register(SELECT_SELF_KEY);
        event.register(START_DEBUG_KEY);
        event.register(STOP_ALL_DEBUG_KEY);
        event.register(STOP_INSTANCE_DEBUG_KEY);
        event.register(NEXT_STAGE_DEBUG_KEY);
        event.register(SPEED_UP_DEBUG_KEY);
        event.register(SPEED_DOWN_DEBUG_KEY);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new AfwVanillaTextureResolver.Reloader());
        event.registerReloadListener(new AfwBoneTextureOverrides.Reloader());
        event.registerReloadListener(new AfwSoundEffects.Reloader());
        event.registerReloadListener(new AfwAnimationAssetDiagnostics.Reloader());
    }

    private static void onClientTickEvent(ClientTickEvent.Post event) {
        onClientTick(MinecraftClient.getInstance());
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        sendDebugChatPreference();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MinecraftClient client = MinecraftClient.getInstance();
        AfwClientAnimationRuntime.stopAllNow();
        AfwAnimatedCameraPoseTracker.clear();
        AfwAnimationCameraZoom.reset();
        resetCameraPerspective(client);
    }

    public static void handleStartPayload(StartAnimationS2CPayload payload, IPayloadContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        AfwClientAnimationRuntime.queueStart(payload.animationId(), payload.instanceId(), payload.actorUuids(),
                payload.actorKeys(), payload.stages(), payload.startTick(), payload.speed(), payload.lockOrientation(),
                payload.lockedYaw(), payload.lockedHeadYaw(), payload.lockedPitch(), payload.cameraOrbitTarget());
        ClientPlayerEntity player = client.player;
        if (player != null && shouldShow(AfwDebugChatCategory.INFO)) {
            String keysStr = payload.actorKeys().isEmpty() ? "none" : payload.actorKeys().toString();
            player.sendMessage(tr("start_received", payload.animationId(), payload.actorUuids().size(), keysStr), false);
        }
    }

    public static void handleStopAllPayload(StopAllAnimationsS2CPayload payload, IPayloadContext context) {
        AfwClientAnimationRuntime.queueStopAll(payload.stopTick());
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && shouldShow(AfwDebugChatCategory.INFO)) {
            player.sendMessage(tr("stop_all_received", new Object[0]), false);
        }
    }

    public static void handleAdvanceStagePayload(AdvanceAnimationStageS2CPayload payload, IPayloadContext context) {
        AfwClientAnimationRuntime.queueStageAdvance(payload.instanceId(), payload.advanceTick(), payload.stageIndex());
    }

    public static void handleSpeedUpdatePayload(AnimationSpeedUpdateS2CPayload payload, IPayloadContext context) {
        AfwClientAnimationRuntime.queueSpeedUpdate(payload.instanceId(), payload.speed());
    }

    public static void handleStopPayload(StopAnimationS2CPayload payload, IPayloadContext context) {
        AfwClientAnimationRuntime.queueStop(payload.instanceId(), payload.stopTick());
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && shouldShow(AfwDebugChatCategory.INFO)) {
            player.sendMessage(tr("stop_instance_received", new Object[0]), false);
        }
    }

    public static void sendDebugChatPreference() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && canSendToServer()) {
            AnimationFrameworkClient.sendPacket(new DebugChatPreferenceC2SPayload(AfwClientConfig.get().debugChatMode().id()));
        }
    }

    private static void sendPacket(CustomPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    private static boolean canSendToServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getNetworkHandler() != null;
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (client.currentScreen == null) {
            while (SELECT_ACTOR_KEY.wasPressed()) {
                AnimationFrameworkClient.toggleTargetedActorSelection(client);
            }
            while (SELECT_SELF_KEY.wasPressed()) {
                AnimationFrameworkClient.toggleSelfSelection(client);
            }
            while (START_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestDebugStart(client);
            }
            while (STOP_ALL_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestStopAll(client);
            }
            while (STOP_INSTANCE_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestStopInstance(client);
            }
            while (NEXT_STAGE_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestNextStage(client);
            }
            while (SPEED_UP_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestSpeedAdjust(client, 1.1);
            }
            while (SPEED_DOWN_DEBUG_KEY.wasPressed()) {
                AnimationFrameworkClient.requestSpeedAdjust(client, 0.9);
            }
        }
        AfwClientAnimationRuntime.clientTick(client);
        AfwAnimationCameraZoom.clientTick(client);
        AfwAnimationAssetDiagnostics.validateLoadedDefinitionsOnce();
        AfwAnimatedCameraPoseTracker.prune(client.world.getTime());
        AnimationFrameworkClient.handleCompletedStages(client);
        AnimationFrameworkClient.syncCameraPerspective(client);
        AnimationFrameworkClient.syncLocalPlayerToAnchor(client);
    }

    private static void requestStopAll(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (!canSendToServer()) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("server_cannot_receive_stop_all", new Object[0]));
            AfwClientAnimationRuntime.stopAllNow();
            return;
        }
        AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("stop_all_requested", new Object[0]));
        AnimationFrameworkClient.sendPacket(new DebugStopAllAnimationsC2SPayload());
    }

    private static void requestStopInstance(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        UUID self = player.getUuid();
        UUID instanceToStop = AfwClientAnimationRuntime.findLatestActiveInstanceContaining(self);
        if (instanceToStop == null) {
            instanceToStop = AfwClientAnimationRuntime.findLatestActiveInstance();
        }
        if (instanceToStop == null) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_active_instance_to_stop", new Object[0]));
            return;
        }
        if (!canSendToServer()) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("server_cannot_receive_stop_instance", new Object[0]));
            return;
        }
        AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("stop_instance_requested", new Object[0]));
        AnimationFrameworkClient.sendPacket(new DebugStopAnimationC2SPayload(instanceToStop));
    }

    private static void toggleTargetedActorSelection(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_entity_targeted", new Object[0]));
            return;
        }
        EntityHitResult entityHit = (EntityHitResult)hit;
        Entity target = entityHit.getEntity();
        if (!(target instanceof LivingEntity)) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("target_not_living", new Object[0]));
            return;
        }
        int id = target.getId();
        Identifier typeId = Registries.ENTITY_TYPE.getId(target.getType());
        UUID instId = AfwClientAnimationRuntime.findLatestActiveInstanceContaining(target.getUuid());
        if (instId != null) {
            if (SELECTED_INSTANCE_ID != null && SELECTED_INSTANCE_ID.equals(instId)) {
                SELECTED_INSTANCE_ID = null;
                SELECTED_ACTOR_IDS.clear();
                AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("animation_deselected", new Object[0]));
                return;
            }
            SELECTED_INSTANCE_ID = instId;
            SELECTED_ACTOR_IDS.clear();
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("animation_selected_add_actors_for_join", new Object[0]));
            return;
        }
        int existingIndex = AnimationFrameworkClient.indexOfSelectedActor(id);
        if (existingIndex >= 0) {
            SELECTED_ACTOR_IDS.removeInt(existingIndex);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("entity_deselected_actor_count", typeId.getPath(), SELECTED_ACTOR_IDS.size()));
        } else {
            SELECTED_ACTOR_IDS.add(id);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("entity_selected_actor_count", typeId.getPath(), SELECTED_ACTOR_IDS.size()));
        }
    }

    private static void toggleSelfSelection(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        int id = player.getId();
        int existingIndex = AnimationFrameworkClient.indexOfSelectedActor(id);
        if (existingIndex >= 0) {
            SELECTED_ACTOR_IDS.removeInt(existingIndex);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("self_deselected_actor_count", SELECTED_ACTOR_IDS.size()));
        } else {
            SELECTED_ACTOR_IDS.add(id);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("self_selected_actor_count", SELECTED_ACTOR_IDS.size()));
        }
    }

    private static void requestDebugStart(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (SELECTED_INSTANCE_ID == null && SELECTED_ACTOR_IDS.isEmpty()) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_actors_selected", new Object[0]));
            return;
        }
        if (SELECTED_INSTANCE_ID != null) {
            ArrayList<Integer> actorIds = new ArrayList<Integer>(SELECTED_ACTOR_IDS.size());
            for (int i = 0; i < SELECTED_ACTOR_IDS.size(); ++i) {
                actorIds.add(SELECTED_ACTOR_IDS.getInt(i));
            }
            if (actorIds.isEmpty()) {
                AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_extra_actors_for_join", new Object[0]));
                return;
            }
            if (!canSendToServer()) {
                AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("server_cannot_receive_join", new Object[0]));
                return;
            }
            AnimationFrameworkClient.sendPacket(new DebugJoinAnimationC2SPayload(SELECTED_INSTANCE_ID, actorIds));
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("join_requested_instance_extras", SELECTED_INSTANCE_ID, actorIds.size()));
            SELECTED_INSTANCE_ID = null;
            SELECTED_ACTOR_IDS.clear();
            return;
        }
        if (!canSendToServer()) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("server_cannot_receive_debug_packets", new Object[0]));
            return;
        }
        ArrayList<Integer> actorIds = new ArrayList<Integer>(SELECTED_ACTOR_IDS.size());
        for (int i = 0; i < SELECTED_ACTOR_IDS.size(); ++i) {
            actorIds.add(SELECTED_ACTOR_IDS.getInt(i));
        }
        AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("start_requested_actor_count", actorIds.size()));
        int anchorId = -1;
        if (AfwClientConfig.get().anchorAtLastSelected() && actorIds.size() >= 2) {
            anchorId = (Integer)actorIds.get(actorIds.size() - 1);
        }
        String behaviorId = AfwClientConfig.get().debugDamageBehavior().id();
        boolean ignoreAttackers = AfwClientConfig.get().debugIgnoreAttackers();
        AnimationFrameworkClient.sendPacket(new DebugStartAnimationC2SPayload(actorIds, behaviorId, ignoreAttackers, anchorId));
        SELECTED_ACTOR_IDS.clear();
    }

    private static void requestNextStage(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        UUID instanceToAdvance = AfwClientAnimationRuntime.findLatestActiveInstance();
        if (instanceToAdvance == null) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_active_instance_to_advance", new Object[0]));
            return;
        }
        if (canSendToServer()) {
            AnimationFrameworkClient.sendPacket(new DebugAdvanceStageC2SPayload(instanceToAdvance));
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("next_stage_requested_server", new Object[0]));
        } else {
            AfwClientAnimationRuntime.requestStageAdvance(instanceToAdvance);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("next_stage_requested_local", new Object[0]));
        }
    }

    private static void requestSpeedAdjust(MinecraftClient client, double multiplier) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        UUID targetInstance = AfwClientAnimationRuntime.findLatestActiveInstanceContaining(player.getUuid());
        if (targetInstance == null) {
            targetInstance = AfwClientAnimationRuntime.findLatestActiveInstance();
        }
        if (targetInstance == null) {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("no_active_instance_to_adjust_speed", new Object[0]));
            return;
        }
        if (canSendToServer()) {
            AnimationFrameworkClient.sendPacket(new AdjustAnimationSpeedC2SPayload(targetInstance, multiplier));
            double percent = (multiplier - 1.0) * 100.0;
            String delta = (percent >= 0.0 ? "+" : "") + String.format(Locale.ROOT, "%.0f%%", percent);
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("speed_change_requested", delta));
        } else {
            AnimationFrameworkClient.sendManualDebug((PlayerEntity)player, AnimationFrameworkClient.tr("server_cannot_receive_speed", new Object[0]));
        }
    }

    private static void handleCompletedStages(MinecraftClient client) {
        List<UUID> completed = AfwClientAnimationRuntime.drainCompletedInstances();
        if (completed.isEmpty()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        boolean canSend = canSendToServer();
        for (UUID instanceId : completed) {
            if (canSend) {
                AnimationFrameworkClient.sendPacket(new DebugStopAnimationC2SPayload(instanceId));
                continue;
            }
            if (player == null || !AnimationFrameworkClient.shouldShow(AfwDebugChatCategory.ERROR)) continue;
            AnimationFrameworkClient.sendCategoryDebug((PlayerEntity)player, AfwDebugChatCategory.ERROR, AnimationFrameworkClient.tr("stage_sequence_ended_server_stop_unavailable", new Object[0]));
        }
    }

    private static void syncCameraPerspective(MinecraftClient client) {
        if (client.player == null || client.options == null || client.world == null) {
            return;
        }
        if (!AfwClientConfig.get().autoSwitchThirdPersonOnAnimationStart()) {
            AnimationFrameworkClient.restoreCameraPerspective(client);
            return;
        }
        UUID selfId = client.player.getUuid();
        boolean shouldThirdPerson = AfwClientAnimationRuntime.isActorPendingOrActive(selfId);
        if (shouldThirdPerson) {
            if (!wasSelfAnimating) {
                preAnimationPerspective = client.options.getPerspective();
                ownsCameraPerspective = true;
                if (client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
                    client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                }
            } else if (ownsCameraPerspective && client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
                preAnimationPerspective = null;
                ownsCameraPerspective = false;
            }
            wasSelfAnimating = true;
            return;
        }
        if (ownsCameraPerspective || wasSelfAnimating) {
            AnimationFrameworkClient.restoreCameraPerspective(client);
        }
    }

    private static void resetCameraPerspective(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }
        AnimationFrameworkClient.restoreCameraPerspective(client);
    }

    private static void restoreCameraPerspective(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }
        if (ownsCameraPerspective && preAnimationPerspective != null) {
            client.options.setPerspective(preAnimationPerspective);
        }
        wasSelfAnimating = false;
        ownsCameraPerspective = false;
        preAnimationPerspective = null;
    }

    private static void syncLocalPlayerToAnchor(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        UUID selfId = player.getUuid();
        if (AfwClientAnimationRuntime.isActorPendingOrActive(selfId)) {
            double dz;
            double dy;
            UUID anchorId = AfwClientAnimationRuntime.findAnchorUuidForActor(selfId);
            if (anchorId == null) {
                return;
            }
            Entity anchor = null;
            for (Entity candidate : client.world.getEntities()) {
                if (anchorId.equals(candidate.getUuid())) {
                    anchor = candidate;
                    break;
                }
            }
            if (anchor == null) {
                return;
            }
            double dx = player.getX() - anchor.getX();
            if (dx * dx + (dy = player.getY() - anchor.getY()) * dy + (dz = player.getZ() - anchor.getZ()) * dz > 4.0E-4) {
                player.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
                player.setVelocity(Vec3d.ZERO);
                player.fallDistance = 0.0f;
            }
        }
    }

    private static int indexOfSelectedActor(int value) {
        for (int i = 0; i < SELECTED_ACTOR_IDS.size(); ++i) {
            if (SELECTED_ACTOR_IDS.getInt(i) != value) continue;
            return i;
        }
        return -1;
    }

    private static Text tr(String key, Object ... args) {
        return Text.translatable((String)("debug.animationframework." + key), (Object[])AnimationFrameworkClient.sanitizeTranslatableArgs(args));
    }

    private static Object[] sanitizeTranslatableArgs(Object ... args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        Object[] safe = new Object[args.length];
        for (int i = 0; i < args.length; ++i) {
            Object arg = args[i];
            if (arg instanceof Identifier) {
                Identifier id = (Identifier)arg;
                safe[i] = id.getPath();
                continue;
            }
            safe[i] = arg instanceof Text || arg == null ? arg : String.valueOf(arg);
        }
        return safe;
    }

    private static void sendManualDebug(PlayerEntity player, Text message) {
        if (player == null) {
            return;
        }
        player.sendMessage((Text)message.copy().formatted(Formatting.DARK_GRAY), false);
    }

    private static void sendCategoryDebug(PlayerEntity player, AfwDebugChatCategory category, Text message) {
        if (player == null) {
            return;
        }
        player.sendMessage((Text)message.copy().formatted(AnimationFrameworkClient.chatColor(category)), false);
    }

    private static Formatting chatColor(AfwDebugChatCategory category) {
        return switch (category) {
            case ALWAYS -> Formatting.DARK_GRAY;
            case SETUP -> Formatting.YELLOW;
            case WARNING -> Formatting.LIGHT_PURPLE;
            case ERROR -> Formatting.RED;
            case INFO -> Formatting.WHITE;
        };
    }
}

