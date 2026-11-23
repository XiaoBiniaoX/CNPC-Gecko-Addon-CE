package com.goodbird.cnpcgeckoaddon.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import noppes.npcs.CustomNpcs;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

/** Resource enumeration for the pre-listResources resource manager. */
final class EditorResources {
    static ResourceLocation parse(String value) {
        if (value == null || !value.matches("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+")) return null;
        return new ResourceLocation(value);
    }

    static List<String> textures() {
        Set<File> sources = new HashSet<>();
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        Map<String, FallbackResourceManager> domains = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "domainResourceManagers", "field_110548_a");
        for (FallbackResourceManager domain : domains.values()) {
            List<IResourcePack> packs = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, domain, "resourcePacks", "field_110540_a");
            for (IResourcePack pack : packs) {
                if (pack instanceof AbstractResourcePack) {
                    File source = ObfuscationReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack) pack, "resourcePackFile", "field_110597_b");
                    sources.add(source);
                }
            }
        }
        for (ModContainer mod : Loader.instance().getModList()) sources.add(mod.getSource());
        sources.add(CustomNpcs.Dir);
        Set<String> found = new TreeSet<>();
        for (File source : sources) {
            if (source == null || !source.exists()) continue;
            try {
                if (source.isDirectory()) {
                    Path root = source.toPath(), assets = root.resolve("assets");
                    if (!Files.isDirectory(assets)) continue;
                    try (Stream<Path> paths = Files.walk(assets)) {
                        paths.filter(Files::isRegularFile).forEach(path -> add(found, root.relativize(path).toString().replace('\\', '/')));
                    }
                } else {
                    try (ZipFile zip = new ZipFile(source)) {
                        Enumeration<? extends ZipEntry> entries = zip.entries();
                        while (entries.hasMoreElements()) add(found, entries.nextElement().getName());
                    }
                }
            } catch (IOException ignored) {
                // An unreadable pack contributes no candidates; manual paths still work.
            }
        }
        return new ArrayList<>(found);
    }

    private static void add(Set<String> found, String path) {
        if (!path.startsWith("assets/") || !path.endsWith(".png")) return;
        int slash = path.indexOf('/', 7);
        if (slash < 0 || !path.substring(slash + 1).startsWith("textures/")) return;
        String value = path.substring(7, slash) + ":" + path.substring(slash + 1);
        if (parse(value) != null) found.add(value);
    }

    static String displayName(String namespace) {
        ModContainer mod = Loader.instance().getIndexedModList().get(namespace);
        return mod == null ? namespace : mod.getName();
    }

    static String overlayRoot() {
        return Loader.isModLoaded("cnpclines") ? "textures/overlays/" : "textures/";
    }
}
