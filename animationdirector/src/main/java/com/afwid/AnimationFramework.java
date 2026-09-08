package com.afwid;

import com.afwid.api.AfwDamageBehavior;
import com.afwid.data.AfwAnimationDefinitions;
import com.afwid.network.AdjustAnimationSpeedC2SPayload;
import com.afwid.network.AdvanceAnimationStageS2CPayload;
import com.afwid.network.AnimationSpeedUpdateS2CPayload;
import com.afwid.network.AnimationStageInfo;
import com.afwid.network.DebugAdvanceStageC2SPayload;
import com.afwid.network.DebugChatPreferenceC2SPayload;
import com.afwid.network.DebugJoinAnimationC2SPayload;
import com.afwid.network.DebugStartAnimationC2SPayload;
import com.afwid.network.DebugStopAllAnimationsC2SPayload;
import com.afwid.network.DebugStopAnimationC2SPayload;
import com.afwid.network.StartAnimationS2CPayload;
import com.afwid.network.StopAllAnimationsS2CPayload;
import com.afwid.network.StopAnimationS2CPayload;
import com.afwid.network.AfwServerNetworking;
import com.afwid.server.AfwServerAnimationController;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AnimationFramework.MOD_ID)
public class AnimationFramework {
    public static final String MOD_ID = "animationframework";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<UUID, AfwDebugChatMode> DEBUG_CHAT_PREF = new ConcurrentHashMap<>();
    private static final int MAX_PENDING_SETUP_CHAT_MESSAGES = 100;
    private static final List<PendingSetupChatMessage> PENDING_SETUP_CHAT = new ArrayList<>();

    public AnimationFramework(IEventBus modEventBus) {
        modEventBus.addListener(AnimationFramework::registerPayloads);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onServerStarted);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onServerTick);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onLevelTick);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(AnimationFramework::onPlayerLoggedOut);
        AfwServerAnimationController.init();
        LOGGER.info("[{}] Initialized for NeoForge", MOD_ID);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(DebugStartAnimationC2SPayload.ID, DebugStartAnimationC2SPayload.CODEC, AnimationFramework::handleDebugStart);
        registrar.playToServer(DebugStopAllAnimationsC2SPayload.ID, DebugStopAllAnimationsC2SPayload.CODEC, AnimationFramework::handleDebugStopAll);
        registrar.playToServer(DebugStopAnimationC2SPayload.ID, DebugStopAnimationC2SPayload.CODEC, AnimationFramework::handleDebugStop);
        registrar.playToServer(DebugChatPreferenceC2SPayload.ID, DebugChatPreferenceC2SPayload.CODEC, AnimationFramework::handleDebugChatPreference);
        registrar.playToServer(DebugJoinAnimationC2SPayload.ID, DebugJoinAnimationC2SPayload.CODEC, AnimationFramework::handleDebugJoin);
        registrar.playToServer(DebugAdvanceStageC2SPayload.ID, DebugAdvanceStageC2SPayload.CODEC, AnimationFramework::handleDebugAdvance);
        registrar.playToServer(AdjustAnimationSpeedC2SPayload.ID, AdjustAnimationSpeedC2SPayload.CODEC, AnimationFramework::handleAdjustSpeed);

        registrar.playToClient(StartAnimationS2CPayload.ID, StartAnimationS2CPayload.CODEC, AnimationFrameworkClient::handleStartPayload);
        registrar.playToClient(StopAllAnimationsS2CPayload.ID, StopAllAnimationsS2CPayload.CODEC, AnimationFrameworkClient::handleStopAllPayload);
        registrar.playToClient(AdvanceAnimationStageS2CPayload.ID, AdvanceAnimationStageS2CPayload.CODEC, AnimationFrameworkClient::handleAdvanceStagePayload);
        registrar.playToClient(AnimationSpeedUpdateS2CPayload.ID, AnimationSpeedUpdateS2CPayload.CODEC, AnimationFrameworkClient::handleSpeedUpdatePayload);
        registrar.playToClient(StopAnimationS2CPayload.ID, StopAnimationS2CPayload.CODEC, AnimationFrameworkClient::handleStopPayload);
    }

    private static ServerPlayerEntity serverPlayer(IPayloadContext context) {
        return context.player() instanceof ServerPlayerEntity player ? player : null;
    }

    private static void handleDebugStart(DebugStartAnimationC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player == null || !requireDebugControlPermission(player, "start")) return;
        AfwDamageBehavior behavior = AfwDamageBehavior.fromId(payload.damageBehaviorId(), AfwDamageBehavior.STOP_ON_DAMAGE);
        handleDebugStartRequest(player, payload.actorEntityIds(), behavior, payload.ignoreAttackers(), payload.anchorEntityId());
    }

    private static void handleDebugStopAll(DebugStopAllAnimationsC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player == null || !requireDebugControlPermission(player, "stop_all") || !(player.getWorld() instanceof ServerWorld world)) return;
        long stopTick = world.getTime() + 1L;
        MinecraftServer server = world.getServer();
        for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
            if (target.getEntityWorld() == world) {
                AfwServerNetworking.send(target, new StopAllAnimationsS2CPayload(stopTick));
            }
        }
        AfwServerAnimationController.clearAllInstancesInWorld(world);
        sendDebugChat(player, AfwDebugChatCategory.ALWAYS, Text.translatable("debug.animationframework.stop_all_broadcast_queued"), true);
    }

    private static void handleDebugStop(DebugStopAnimationC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player != null && requireDebugControlPermission(player, "stop_instance")) {
            AfwServerAnimationController.stopInstanceAndBroadcast(player, payload.instanceId());
        }
    }

    private static void handleDebugChatPreference(DebugChatPreferenceC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player != null) {
            DEBUG_CHAT_PREF.put(player.getUuid(), AfwDebugChatMode.fromId(payload.modeId(), AfwDebugChatMode.SETUP_ERRORS));
        }
    }

    private static void handleDebugJoin(DebugJoinAnimationC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player != null && requireDebugControlPermission(player, "join")) {
            handleDebugJoinRequest(player, payload.instanceId(), payload.actorEntityIds());
        }
    }

    private static void handleDebugAdvance(DebugAdvanceStageC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player != null && requireDebugControlPermission(player, "advance_stage") && player.getWorld() instanceof ServerWorld world) {
            AfwServerAnimationController.advanceStage(world, payload.instanceId());
        }
    }

    private static void handleAdjustSpeed(AdjustAnimationSpeedC2SPayload payload, IPayloadContext context) {
        ServerPlayerEntity player = serverPlayer(context);
        if (player != null && requireDebugControlPermission(player, "adjust_speed") && player.getWorld() instanceof ServerWorld world) {
            AfwServerAnimationController.adjustSpeed(world, payload.instanceId(), payload.multiplier(), player);
        }
    }

    private static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AfwAnimationDefinitions.Reloader());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        AfwServerAnimationController.onServerStarted(event.getServer());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        flushPendingSetupChat(event.getServer());
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerWorld world) {
            AfwServerAnimationController.onWorldTick(world);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            AfwServerAnimationController.sendActiveInstancesToPlayer(player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            AfwServerAnimationController.onPlayerDisconnected(player);
            DEBUG_CHAT_PREF.remove(player.getUuid());
        }
    }

    public static void logSetupWarning(String template, Object ... args) {
        LOGGER.warn(template, args);
        AnimationFramework.queueSetupChat(AnimationFramework.formatLogTemplate(template, args));
    }

    public static void logSetupError(String template, Object ... args) {
        LOGGER.error(template, args);
        AnimationFramework.queueSetupChat(AnimationFramework.formatLogTemplate(template, args));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void queueSetupChat(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        List<PendingSetupChatMessage> list = PENDING_SETUP_CHAT;
        synchronized (list) {
            if (PENDING_SETUP_CHAT.size() >= 100) {
                PENDING_SETUP_CHAT.remove(0);
            }
            PENDING_SETUP_CHAT.add(new PendingSetupChatMessage((Text)Text.literal((String)message), new HashSet<UUID>()));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void flushPendingSetupChat(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return;
        }
        List<PendingSetupChatMessage> list = PENDING_SETUP_CHAT;
        synchronized (list) {
            PENDING_SETUP_CHAT.removeIf(message -> {
                for (ServerPlayerEntity player : players) {
                    if (player == null || message.sentTo().contains(player.getUuid())) continue;
                    if (AnimationFramework.shouldSendDebugChat(player, AfwDebugChatCategory.SETUP)) {
                        AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.SETUP, message.message(), false);
                    }
                    message.sentTo().add(player.getUuid());
                }
                return !message.sentTo().isEmpty() && message.sentTo().containsAll(players.stream().map(Entity::getUuid).toList());
            });
        }
    }

    public static String formatLogTemplate(String template, Object ... args) {
        if (template == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        StringBuilder out = new StringBuilder(template.length() + args.length * 8);
        int argIndex = 0;
        for (int i = 0; i < template.length(); ++i) {
            char c = template.charAt(i);
            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}' && argIndex < args.length) {
                Object arg;
                if (!((arg = args[argIndex++]) instanceof Throwable)) {
                    out.append(String.valueOf(arg));
                }
                ++i;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public static boolean shouldSendDebugChat(ServerPlayerEntity player, AfwDebugChatCategory category) {
        if (player == null) {
            return false;
        }
        AfwDebugChatMode mode = DEBUG_CHAT_PREF.getOrDefault(player.getUuid(), AfwDebugChatMode.SETUP_ERRORS);
        return mode.allows(category);
    }

    public static boolean shouldSendDebugChat(ServerPlayerEntity player, AfwDebugChatCategory category, boolean force) {
        return force || AnimationFramework.shouldSendDebugChat(player, category);
    }

    public static void sendDebugChat(ServerPlayerEntity player, AfwDebugChatCategory category, String message, boolean force) {
        AnimationFramework.sendDebugChat(player, category, (Text)Text.literal((String)message), force);
    }

    public static void sendDebugChat(ServerPlayerEntity player, AfwDebugChatCategory category, Text message, boolean force) {
        if (!AnimationFramework.shouldSendDebugChat(player, category, force)) {
            return;
        }
        player.sendMessage((Text)message.copy().formatted(AnimationFramework.chatColor(category)), false);
    }

    private static Formatting chatColor(AfwDebugChatCategory category) {
        if (category == null) {
            return Formatting.WHITE;
        }
        return switch (category) {
            case ALWAYS -> Formatting.DARK_GRAY;
            case SETUP -> Formatting.YELLOW;
            case WARNING -> Formatting.LIGHT_PURPLE;
            case ERROR -> Formatting.RED;
            case INFO -> Formatting.WHITE;
        };
    }

    private static boolean requireDebugControlPermission(ServerPlayerEntity player, String action) {
        if (player != null && player.getServerWorld().getServer().getPlayerManager()
                .isOperator(player.getGameProfile())) {
            return true;
        }
        if (player != null) {
            LOGGER.warn("Denied AFW debug control '{}' from non-OP player {}", (Object)action, (Object)player.getName().getString());
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.literal((String)"[AFW] Debug controls require operator permissions."), true);
        }
        return false;
    }

    private static void handleDebugStartRequest(ServerPlayerEntity player, List<Integer> requestedActorIds, AfwDamageBehavior damageBehavior, boolean ignoreAttackers, int anchorEntityId) {
        if (requestedActorIds.isEmpty()) {
            return;
        }
        ArrayList<Entity> resolved = new ArrayList<Entity>();
        Entity anchorEntity = null;
        for (int entityId : requestedActorIds) {
            Entity entity = player.getEntityWorld().getEntityById(entityId);
            if (!(entity instanceof LivingEntity)) continue;
            resolved.add(entity);
            if (anchorEntity != null || entityId != anchorEntityId) continue;
            anchorEntity = entity;
        }
        if (resolved.isEmpty()) {
            return;
        }
        resolved.sort(Comparator.comparingInt(Entity::getId));
        AfwDamageBehavior safeBehavior = damageBehavior == null ? AfwDamageBehavior.STOP_ON_DAMAGE : damageBehavior;
        UUID anchorUuid = anchorEntity != null ? anchorEntity.getUuid() : null;
        AfwServerAnimationController.MatchedStartRequest start = AfwServerAnimationController.startEligibleMatchedNow(player.getServerWorld(), player, resolved, Set.of(), safeBehavior, ignoreAttackers, anchorUuid, Map.of(), true);
        String chosenStr = start == null ? "(none)" : start.animationId().getPath();
        List<String> actorKeys = start == null ? List.of() : start.actorKeys();
        String keysStr = actorKeys.isEmpty() ? "none" : actorKeys.toString();
        MutableText msg = Text.translatable((String)"debug.animationframework.server_eval", (Object[])new Object[]{resolved.size(), chosenStr, keysStr});
        AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.ALWAYS, (Text)msg, true);
        if (start == null) {
            return;
        }
        LOGGER.info("Debug request: player={} chosen={} actors={}", new Object[]{player.getName().getString(), start.animationId(), resolved.size()});
    }

    private static void handleDebugJoinRequest(ServerPlayerEntity player, UUID instanceId, List<Integer> requestedActorIds) {
        if (player == null || requestedActorIds == null || requestedActorIds.isEmpty()) {
            return;
        }
        ServerWorld class_32182 = player.getServerWorld();
        if (!(class_32182 instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = class_32182;
        AfwServerAnimationController.ActiveInstanceSnapshot snapshot = AfwServerAnimationController.getActiveInstanceSnapshot(world, instanceId);
        if (snapshot == null) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_failed_no_active_instance"), true);
            return;
        }
        AnimationStageInfo currentStage = AfwServerAnimationController.getCurrentStage(world, instanceId);
        if (currentStage == null || !currentStage.allowJoin()) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_denied_stage_disallows"), true);
            return;
        }
        ArrayList<Entity> combined = new ArrayList<Entity>();
        HashSet<UUID> existingActorUuids = new HashSet<UUID>(snapshot.actorUuids());
        for (UUID uUID : snapshot.actorUuids()) {
            Entity actor = world.getEntity(uUID);
            if (actor == null) continue;
            combined.add(actor);
        }
        if (combined.size() != snapshot.actorUuids().size()) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_failed_no_active_instance"), true);
            return;
        }
        ArrayList<Entity> additions = new ArrayList<Entity>();
        for (int id : requestedActorIds) {
            Entity e = world.getEntityById(id);
            if (!(e instanceof MobEntity) || !existingActorUuids.add(e.getUuid())) continue;
            additions.add(e);
        }
        if (additions.isEmpty()) {
            return;
        }
        combined.addAll(additions);
        combined.sort(Comparator.comparingInt(Entity::getId));
        AfwAnimationDefinitions.MatchResult matchResult = AfwAnimationDefinitions.match(combined);
        AfwAnimationDefinitions.Definition definition = matchResult.chosen();
        if (definition == null) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_failed_no_expanded_definition"), true);
            return;
        }
        List<String> actorKeys = AfwAnimationDefinitions.resolveActorKeys(definition, combined, world.getRandom());
        if (actorKeys == null || actorKeys.size() != combined.size()) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_failed_no_expanded_definition"), true);
            return;
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<String, String>(snapshot.metadata());
        metadata.put("afw.join_replace", "true");
        metadata.put("afw.join_replace_from", snapshot.animationId().toString());
        if (!AfwServerAnimationController.stopInstance(world, instanceId, false)) {
            AnimationFramework.sendDebugChat(player, AfwDebugChatCategory.WARNING, (Text)Text.translatable((String)"debug.animationframework.join_failed_no_active_instance"), true);
            return;
        }
        AfwServerAnimationController.startNow(world, player, definition.id(), combined, actorKeys, definition.stages(), snapshot.damageBehavior(), snapshot.ignoreAttackers(), null, metadata, true);
    }

    private record PendingSetupChatMessage(Text message, Set<UUID> sentTo) {
    }
}

