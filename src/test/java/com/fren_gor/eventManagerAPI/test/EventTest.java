/*
 * Copyright 2021 fren_gor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fren_gor.eventManagerAPI.test;

import com.fren_gor.eventManagerAPI.EventManager;
import com.fren_gor.eventManagerAPI.test.FakeEvent2.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class EventTest {

    private static final Field EventManager_events, INTERNAL_LISTENER;

    static {
        Field e;
        try {
            e = EventManager.class.getDeclaredField("events");
            e.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            e = null;
            fail("Couldn't instantiate EventManager_events field");
        }
        EventManager_events = e;
    }

    static {
        Field e;
        try {
            e = EventManager.class.getDeclaredField("INTERNAL_LISTENER");
            e.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            e = null;
            fail("Couldn't instantiate INTERNAL_LISTENER field");
        }
        INTERNAL_LISTENER = e;
    }

    private int ev1 = 0, ev2 = 0;

    @Test
    public void eventTest() throws Exception {

        EventManager api = new EventManager(new Pl());

        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent2.class, e -> ev2++);

        assertEquals(0, ev1);
        assertEquals(0, ev2);

        PluginManager impl = Bukkit.getPluginManager();

        FakeEvent1 f1 = new FakeEvent1();
        FakeEvent2 f2 = new FakeEvent2();

        impl.callEvent(f1);
        impl.callEvent(f1);
        impl.callEvent(f2);
        impl.callEvent(f1);

        assertEquals(3, ev1);
        assertEquals(1, ev2);

        api.clearEventListener(FakeEvent1.class);
        impl.callEvent(f1);
        impl.callEvent(f2);

        assertEquals(3, ev1);
        assertEquals(2, ev2);

        api.unregister(this);
        impl.callEvent(f1);
        impl.callEvent(f2);

        assertEquals(3, ev1);
        assertEquals(2, ev2);

        api.unregisterEvent(FakeEvent2.class);

        assertEquals(((Wrapper) FakeEvent2.getHandlerList()).getUnregisterCount(), 1);

        impl.callEvent(f1);
        impl.callEvent(f2);
        assertEquals(3, ev1);
        assertEquals(2, ev2);
    }

    @Test
    public void simpleDisableTest() throws Exception {
        EventManager api = new EventManager(new Pl());

        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent2.class, e -> ev2++);

        assertEquals(0, ev1);
        assertEquals(0, ev2);

        PluginManagerImpl impl = (PluginManagerImpl) Bukkit.getPluginManager();

        impl.callEvent(new PluginDisableEvent(api.getPlugin()));

        assertEquals(((Map<Class<?>, Object>) EventManager_events.get(api)).size(), 0);

        impl.callEvent(new FakeEvent1());
        impl.callEvent(new FakeEvent2());

        assertEquals(0, ev1);
        assertEquals(0, ev2);
    }

    @Test
    public void priorityTest() throws Exception {

        EventManager api = new EventManager(new Pl());

        api.register(this, FakeEvent1.class, EventPriority.LOWEST, e -> ev2++);
        api.register(this, FakeEvent1.class, EventPriority.LOWEST, e -> ev2++);
        api.register(this, FakeEvent1.class, EventPriority.LOW, e -> ev1++);
        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent1.class, EventPriority.HIGH, e -> ev2++);
        api.register(this, FakeEvent1.class, EventPriority.HIGHEST, e -> ev2++);

        Object eventGroup = ((Map<Class<?>, Object>) EventManager_events.get(api)).get(FakeEvent1.class);

        final Field eventListeners = eventGroup.getClass().getDeclaredField("eventListeners");
        eventListeners.setAccessible(true);

        Object[] listeners = ((Object[]) eventListeners.get(eventGroup));
        final Field map = listeners.getClass().getComponentType().getDeclaredField("map");
        map.setAccessible(true);

        // =================== LOWEST Check ===================

        Object listener = listeners[EventPriority.LOWEST.getSlot()];

        assertEquals(2, ((Map<Object, List<Consumer<?>>>) map.get(listener)).get(this).size());

        // =================== LOW Check ===================

        listener = listeners[EventPriority.LOW.getSlot()];

        assertEquals(1, ((Map<Object, List<Consumer<?>>>) map.get(listener)).get(this).size());

        // =================== NORMAL Check ===================

        listener = listeners[EventPriority.NORMAL.getSlot()];

        assertEquals(3, ((Map<Object, List<Consumer<?>>>) map.get(listener)).get(this).size());

        // =================== HIGH Check ===================

        listener = listeners[EventPriority.HIGH.getSlot()];

        assertEquals(1, ((Map<Object, List<Consumer<?>>>) map.get(listener)).get(this).size());

        // =================== HIGHEST Check ===================

        listener = listeners[EventPriority.HIGHEST.getSlot()];

        assertEquals(1, ((Map<Object, List<Consumer<?>>>) map.get(listener)).get(this).size());

        // =================== MONITOR Check ===================

        listener = listeners[EventPriority.MONITOR.getSlot()];

        assertNull(listener);

    }

    @Test
    public void disableTest() throws Exception {
        EventManager api = new EventManager(new Pl());

        api.register(this, FakeEvent1.class, e -> ev1++);
        api.register(this, FakeEvent2.class, e -> ev2++);

        assertEquals(ev1, 0);
        assertEquals(ev2, 0);

        // =================== Base Check ===================

        Object internalListener = INTERNAL_LISTENER.get(api);

        Object eventGroup = ((Map<Class<?>, Object>) EventManager_events.get(api)).get(PluginDisableEvent.class);

        final Field eventListeners = eventGroup.getClass().getDeclaredField("eventListeners");
        eventListeners.setAccessible(true);

        Object listener = ((Object[]) eventListeners.get(eventGroup))[EventPriority.MONITOR.getSlot()];

        final Field map = listener.getClass().getDeclaredField("map");
        map.setAccessible(true);
        Map<Object, List<Consumer<?>>> m = (Map<Object, List<Consumer<?>>>) map.get(listener);

        assertEquals(1, m.size());
        assertEquals(1, m.get(internalListener).size());

        // ========= After unregister Check =========

        api.unregister(INTERNAL_LISTENER.get(api));

        eventGroup = ((Map<Class<?>, Object>) EventManager_events.get(api)).get(PluginDisableEvent.class);
        listener = ((Object[]) eventListeners.get(eventGroup))[EventPriority.MONITOR.getSlot()];

        m = (Map<Object, List<Consumer<?>>>) map.get(listener);

        assertEquals(1, m.size());
        assertEquals(1, m.get(internalListener).size());

        // ========= After clearEventListener Check =========

        api.clearEventListener(PluginDisableEvent.class);

        eventGroup = ((Map<Class<?>, Object>) EventManager_events.get(api)).get(PluginDisableEvent.class);
        listener = ((Object[]) eventListeners.get(eventGroup))[EventPriority.MONITOR.getSlot()];

        m = (Map<Object, List<Consumer<?>>>) map.get(listener);

        assertEquals(1, m.size());
        assertEquals(1, m.get(internalListener).size());

        // ========= After unregisterEvent Check =========

        api.unregisterEvent(PluginDisableEvent.class);

        eventGroup = ((Map<Class<?>, Object>) EventManager_events.get(api)).get(PluginDisableEvent.class);
        listener = ((Object[]) eventListeners.get(eventGroup))[EventPriority.MONITOR.getSlot()];

        m = (Map<Object, List<Consumer<?>>>) map.get(listener);

        assertEquals(1, m.size());
        assertEquals(1, m.get(internalListener).size());

    }

    @Test
    public void disabledErrorTest() {
        Pl pl = new Pl();
        pl.enabled = false;
        assertThrows(IllegalArgumentException.class, () -> new EventManager(pl));
        pl.enabled = true;
        EventManager api = new EventManager(pl);
        assertTrue(api.isEnabled());
        pl.enabled = false;
        Consumer<FakeEvent1> consumer = FakeEvent1::hashCode; // Just a method
        assertThrows(IllegalArgumentException.class, () -> api.register(this, FakeEvent1.class, consumer));
        assertThrows(IllegalArgumentException.class, () -> api.register(this, FakeEvent1.class, EventPriority.HIGHEST, consumer));
        api.disable(); // Disable API
        pl.enabled = true; // Re-enable plugin
        assertFalse(api.isEnabled());
        assertThrows(IllegalStateException.class, () -> api.register(this, FakeEvent1.class, consumer));
        assertThrows(IllegalStateException.class, () -> api.register(this, FakeEvent1.class, EventPriority.HIGHEST, consumer));
        assertThrows(IllegalStateException.class, () -> api.unregister(this));
        assertThrows(IllegalStateException.class, () -> api.unregisterEvent(FakeEvent1.class));
        assertThrows(IllegalStateException.class, () -> api.clearEventListener(FakeEvent1.class));
        api.disable(); // Shouldn't throws errors
    }

    private static class Pl implements Plugin {

        public boolean enabled = true;

        @NotNull
        @Override
        public File getDataFolder() {
            return null;
        }

        @NotNull
        @Override
        public PluginDescriptionFile getDescription() {
            return null;
        }

        @NotNull
        @Override
        public FileConfiguration getConfig() {
            return null;
        }

        @Nullable
        @Override
        public InputStream getResource(@NotNull String s) {
            return null;
        }

        @Override
        public void saveConfig() {

        }

        @Override
        public void saveDefaultConfig() {

        }

        @Override
        public void saveResource(@NotNull String s, boolean b) {

        }

        @Override
        public void reloadConfig() {

        }

        @NotNull
        @Override
        public PluginLoader getPluginLoader() {
            return null;
        }

        @NotNull
        @Override
        public Server getServer() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void onDisable() {

        }

        @Override
        public void onLoad() {

        }

        @Override
        public void onEnable() {

        }

        @Override
        public boolean isNaggable() {
            return false;
        }

        @Override
        public void setNaggable(boolean b) {

        }

        @Nullable
        @Override
        public ChunkGenerator getDefaultWorldGenerator(@NotNull String s, @Nullable String s1) {
            return null;
        }

        @NotNull
        @Override
        public Logger getLogger() {
            return null;
        }

        @NotNull
        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return false;
        }

        @Nullable
        @Override
        public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            return null;
        }
    }

}
