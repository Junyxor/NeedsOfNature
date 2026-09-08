package com.afwid.client.render.gecko;

import com.afwid.AnimationFramework;
import com.afwid.client.config.AfwClientConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class AfwVanillaTextureResolver {
    private static final Map<Identifier, Identifier> REDIRECT_CACHE = new ConcurrentHashMap<>();
    private static final List<Identifier> UPLOADED_TEXTURES = new ArrayList<>();
    private static final Identifier RELOAD_ID =
            Identifier.of("animationframework", "vanilla_texture_resolver");

    private AfwVanillaTextureResolver() {
    }

    /** Registration is performed by AnimationFrameworkClient on NeoForge's client mod bus. */
    public static void registerReloadListener() {
    }

    public static final class Reloader implements SynchronousResourceReloader {
        @Override
        public void reload(ResourceManager manager) {
            clearCache();
        }
    }

    public static Identifier resolveTexture(Identifier textureId) {
        if (textureId == null || !AfwClientConfig.get().forceVanillaEntityTextures()) {
            return textureId;
        }
        return REDIRECT_CACHE.computeIfAbsent(textureId, AfwVanillaTextureResolver::copyVanillaTexture);
    }

    private static Identifier copyVanillaTexture(Identifier textureId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return textureId;
        }
        try {
            List<Resource> resources = client.getResourceManager().getAllResources(textureId);
            if (resources.isEmpty()) {
                return textureId;
            }
            Resource selected = resources.get(resources.size() - 1);
            for (Resource resource : resources) {
                String packName = resource.getPackId();
                if ("vanilla".equals(packName) || "minecraft".equals(packName)
                        || "default".equalsIgnoreCase(packName)) {
                    selected = resource;
                    break;
                }
            }
            try (InputStream stream = selected.getInputStream()) {
                NativeImage image = NativeImage.read(stream);
                Identifier redirected = Identifier.of("animationframework",
                        "vanilla_textures/" + textureId.getNamespace() + "/" + textureId.getPath());
                client.getTextureManager().registerTexture(
                        redirected, new NativeImageBackedTexture(image));
                synchronized (UPLOADED_TEXTURES) {
                    UPLOADED_TEXTURES.add(redirected);
                }
                return redirected;
            }
        } catch (IOException | RuntimeException exception) {
            AnimationFramework.LOGGER.warn(
                    "[AFW] Failed to copy vanilla texture {}, using active resource stack",
                    textureId, exception);
            return textureId;
        }
    }

    public static void clearCache() {
        REDIRECT_CACHE.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        synchronized (UPLOADED_TEXTURES) {
            for (Identifier id : UPLOADED_TEXTURES) {
                client.getTextureManager().destroyTexture(id);
            }
            UPLOADED_TEXTURES.clear();
        }
    }
}
