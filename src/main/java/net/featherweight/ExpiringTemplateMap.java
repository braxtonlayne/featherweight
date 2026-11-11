package net.featherweight;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import net.featherweight.mixin.StructureTemplateAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Replacement for StructureTemplateManager's template cache. Vanilla keeps every template it ever loads
 * (villages, towers, dungeons, ...) for the whole session. Here, templates loaded from data packs expire
 * after a few minutes without use and are re-read from disk when needed again. Templates created at runtime
 * (structure blocks, {@code getOrCreate}) are kept until removed, because they may not be saved yet.
 */
public final class ExpiringTemplateMap extends AbstractMap<ResourceLocation, Optional<StructureTemplate>> {
    private static final Set<ExpiringTemplateMap> INSTANCES = java.util.Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private final ConcurrentMap<ResourceLocation, Optional<StructureTemplate>> pinned = new ConcurrentHashMap<>();
    private final Cache<ResourceLocation, Optional<StructureTemplate>> loaded = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(3))
            .softValues()
            .build();

    public ExpiringTemplateMap() {
        INSTANCES.add(this);
    }

    static void cleanUpAll() {
        for (ExpiringTemplateMap map : INSTANCES) {
            map.loaded.cleanUp();
            map.logStats();
        }
    }

    private long lastLogged = -1;

    private void logStats() {
        long count = loaded.size() + pinned.size();
        if (count == lastLogged) {
            return;
        }
        lastLogged = count;
        long blocks = 0;
        for (Optional<StructureTemplate> t : entrySet().stream().map(Entry::getValue).toList()) {
            if (t.isPresent()) {
                for (StructureTemplate.Palette p : ((StructureTemplateAccessor) t.get()).featherweight$palettes()) {
                    blocks += p.blocks().size();
                }
            }
        }
        LOGGER.debug("Structure templates cached: {} ({} pinned), {} block entries", count, pinned.size(), blocks);
    }

    @Override
    public Optional<StructureTemplate> computeIfAbsent(ResourceLocation key,
            Function<? super ResourceLocation, ? extends Optional<StructureTemplate>> mappingFunction) {
        Optional<StructureTemplate> p = pinned.get(key);
        if (p != null) {
            return p;
        }
        try {
            return loaded.get(key, () -> mappingFunction.apply(key));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load structure template " + key, e.getCause());
        }
    }

    @Override
    public Optional<StructureTemplate> get(Object key) {
        Optional<StructureTemplate> p = pinned.get(key);
        return p != null ? p : loaded.getIfPresent(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Optional<StructureTemplate> put(ResourceLocation key, Optional<StructureTemplate> value) {
        Optional<StructureTemplate> old = get(key);
        loaded.invalidate(key);
        pinned.put(key, value);
        return old;
    }

    // ModernFix's dynamic_structure_manager loads templates outside the map lock and stores them with
    // putIfAbsent: those are loaded templates, so they go in the expiring cache, not the pinned map.
    @Override
    public Optional<StructureTemplate> putIfAbsent(ResourceLocation key, Optional<StructureTemplate> value) {
        Optional<StructureTemplate> p = pinned.get(key);
        if (p != null) {
            return p;
        }
        return loaded.asMap().putIfAbsent(key, value);
    }

    @Override
    public Optional<StructureTemplate> remove(Object key) {
        Optional<StructureTemplate> old = get(key);
        pinned.remove(key);
        loaded.invalidate(key);
        return old;
    }

    @Override
    public void clear() {
        pinned.clear();
        loaded.invalidateAll();
    }

    @Override
    public Set<Entry<ResourceLocation, Optional<StructureTemplate>>> entrySet() {
        Map<ResourceLocation, Optional<StructureTemplate>> snapshot = new HashMap<>(loaded.asMap());
        snapshot.putAll(pinned);
        return snapshot.entrySet();
    }
}
