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

package com.fren_gor.eventManagerAPI;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Class to register many event listeners using only one listener per event.
 */
public final class EventManager {

    private static final int priorities;

    static {
        int max = 0;
        for (EventPriority p : EventPriority.values()) {
            if (max < p.getSlot()) {
                max = p.getSlot();
            }
        }
        priorities = max + 1;
    }

    @NotNull
    private final Plugin plugin;
    private final Object INTERNAL_LISTENER = new Object();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final Map<Class<? extends Event>, EventGroup<? extends Event>> events = new HashMap<>();

    /**
     * Create an EventManager.
     *
     * @param plugin The plugin which will be used to register events.
     * @throws IllegalArgumentException If {@link Plugin} is null or not enabled.
     */
    public EventManager(@NotNull Plugin plugin) throws IllegalArgumentException {
        Preconditions.checkNotNull(plugin, "Plugin cannot be null.");
        Preconditions.checkArgument(plugin.isEnabled(), "Plugin isn't enabled.");
        this.plugin = plugin;
        registerPluginDisableEvent();
    }

    /**
     * Register a listener to an {@link Event} with {@link EventPriority#NORMAL} priority.
     * <p>A new bukkit {@link Listener} will be registered iff no other event with the same type and priority has been registered before.
     *
     * @param listener The listener. It can be every object.
     * @param event The event to listen to.
     * @param consumer The code to be run when the event is called.
     * @throws IllegalStateException If the {@link EventManager} is disabled. See {@link EventManager#isEnabled()}.
     * @throws IllegalArgumentException If any argument is null or the {@link Plugin} is disabled. See {@link Plugin#isEnabled()}.
     */
    public <E extends Event> void register(@NotNull Object listener, @NotNull Class<E> event, @NotNull Consumer<E> consumer) throws IllegalStateException, IllegalArgumentException {
        register(listener, event, EventPriority.NORMAL, consumer);
    }

    /**
     * Register a listener to an {@link Event} with a certain priority.
     * <p>A new bukkit {@link Listener} will be registered iff no other event with the same type and priority has been registered before.
     *
     * @param listener The listener. It can be every object.
     * @param event The event to listen to.
     * @param priority The event priority.
     * @param consumer The code to be run when the event is called.
     * @throws IllegalStateException If the {@link EventManager} is disabled. See {@link EventManager#isEnabled()}.
     * @throws IllegalArgumentException If any argument is null or the {@link Plugin} is disabled. See {@link Plugin#isEnabled()}.
     */
    public <E extends Event> void register(@NotNull Object listener, @NotNull Class<E> event, @NotNull EventPriority priority, @NotNull Consumer<E> consumer) throws IllegalStateException, IllegalArgumentException {
        if (!enabled.get())
            throw new IllegalStateException("EventManager is disabled. Cannot register any event.");
        if (!plugin.isEnabled())
            throw new IllegalArgumentException("Plugin is disabled. Cannot register any event.");

        Preconditions.checkNotNull(listener, "Listener cannot be null.");
        Preconditions.checkNotNull(event, "Event class cannot be null.");
        Preconditions.checkNotNull(priority, "EventPriority cannot be null.");
        Preconditions.checkNotNull(consumer, "Consumer cannot be null.");

        EventGroup<E> el;
        synchronized (events) {
            el = (EventGroup<E>) events.computeIfAbsent(event, c -> new EventGroup<E>());
        }

        el.getListener(priority, event).register(listener, consumer);
    }

    /**
     * Unregister every event from a specified listener.
     *
     * @param listener The listener to be unregister.
     * @throws IllegalStateException If the {@link EventManager} is disabled. See {@link EventManager#isEnabled()}.
     * @throws IllegalArgumentException If listener is null.
     */
    public void unregister(@NotNull Object listener) throws IllegalStateException, IllegalArgumentException {
        checkInitialisation();
        Preconditions.checkNotNull(listener, "Listener cannot be null.");
        synchronized (events) {
            for (EventGroup<? extends Event> el : events.values()) {
                el.unregisterListener(listener);
            }
            if (listener == INTERNAL_LISTENER) {
                registerPluginDisableEvent();
            }
        }
    }

    /**
     * Remove every listener to an event without unregistering its bukkit {@link Listener}.
     *
     * @param event The event's class.
     * @throws IllegalStateException If the {@link EventManager} is disabled. See {@link EventManager#isEnabled()}.
     * @throws IllegalArgumentException If event class is null.
     */
    public void clearEventListener(@NotNull Class<? extends Event> event) throws IllegalStateException, IllegalArgumentException {
        checkInitialisation();
        Preconditions.checkNotNull(event, "Event class cannot be null.");
        synchronized (events) {
            EventGroup<? extends Event> el = events.get(event);
            if (el != null) {
                el.clearListeners();
                if (event == PluginDisableEvent.class) {
                    registerPluginDisableEvent();
                }
            }
        }
    }

    /**
     * Unregister every listener to an event and try to unregister its bukkit {@link Listener}.
     *
     * @param event The event's class.
     * @throws IllegalStateException If the {@link EventManager} is disabled. See {@link EventManager#isEnabled()}.
     * @throws IllegalArgumentException If event class is null.
     */
    public void unregisterEvent(@NotNull Class<? extends Event> event) throws IllegalStateException, IllegalArgumentException {
        checkInitialisation();
        Preconditions.checkNotNull(event, "Event class cannot be null.");
        synchronized (events) {
            EventGroup<? extends Event> el = events.remove(event);
            if (el != null) {
                el.unregisterBukkitListener();
                if (event == PluginDisableEvent.class) {
                    registerPluginDisableEvent();
                }
            }
        }
    }

    /**
     * Unregister every registered bukkit {@link Listener} and disable the event manager.
     */
    public void disable() {
        if (!enabled.compareAndSet(true, false)) {
            return;
        }
        synchronized (events) {
            for (EventGroup<? extends Event> el : events.values()) {
                el.unregisterBukkitListener();
            }
            events.clear();
        }
    }

    /**
     * Gets the plugin which is used to register the listeners.
     *
     * @return The plugin which owns this {@link EventManager}.
     */
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns if the event manager is enabled and can register or listen to events.
     * <p>To disable the event manager and unregister every registered listener see {@link EventManager#disable()}.
     *
     * @return true if the event manager is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    private void registerPluginDisableEvent() {
        register(INTERNAL_LISTENER, PluginDisableEvent.class, EventPriority.MONITOR, e -> {
            if (e.getPlugin() == plugin) {
                enabled.set(false);
                synchronized (events) {
                    events.clear();
                }
            }
        });
    }

    private void checkInitialisation() throws IllegalStateException {
        if (!enabled.get())
            throw new IllegalStateException("EventManager is disabled. Cannot perform any action.");
    }

    private final class EventGroup<E extends Event> {

        private final EventListener<E>[] eventListeners = new EventListener[priorities];

        @NotNull
        public synchronized EventListener<E> getListener(@NotNull EventPriority priority, @NotNull Class<E> event) {
            EventListener<E> l = eventListeners[priority.getSlot()];
            if (l == null) {
                return eventListeners[priority.getSlot()] = new EventListener<>(event, priority);
            }
            return l;
        }

        public synchronized void unregisterListener(@NotNull Object listener) {
            for (int i = 0; i < eventListeners.length; i++) {
                EventListener<E> l = eventListeners[i];
                if (l != null)
                    l.unregisterListener(listener);
            }
        }

        public synchronized void clearListeners() {
            for (int i = 0; i < eventListeners.length; i++) {
                EventListener<E> l = eventListeners[i];
                if (l != null)
                    l.clearListeners();
            }
        }

        public synchronized void unregisterBukkitListener() {
            for (int i = 0; i < eventListeners.length; i++) {
                EventListener<E> l = eventListeners[i];
                if (l != null) {
                    l.unregisterBukkitListener();
                    eventListeners[i] = null;
                }
            }
        }

    }

    private final class EventListener<E extends Event> implements Listener {

        private final Map<Object, List<Consumer<E>>> map = new HashMap<>();
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Class<E> clazz;

        public EventListener(@NotNull Class<E> clazz, @NotNull EventPriority priority) {
            this.clazz = Objects.requireNonNull(clazz, "Event class is null.");
            Bukkit.getPluginManager().registerEvent(clazz, this, priority, (listener, event) -> call(event), plugin);
        }

        public void register(@NotNull Object listener, @NotNull Consumer<E> consumer) {
            Preconditions.checkNotNull(listener, "Listener is null.");
            Preconditions.checkNotNull(consumer, "Consumer is null.");
            readWriteLock.writeLock().lock();
            try {
                List<Consumer<E>> l = map.computeIfAbsent(listener, k -> new LinkedList<>());
                l.add(consumer);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

        public void unregisterListener(@NotNull Object listener) {
            readWriteLock.writeLock().lock();
            try {
                map.remove(listener);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

        public void clearListeners() {
            readWriteLock.writeLock().lock();
            try {
                map.clear();
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

        public void unregisterBukkitListener() {
            readWriteLock.writeLock().lock();
            try {
                HandlerList.unregisterAll(this);
                map.clear();
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

        public void call(@NotNull Event e) {
            Preconditions.checkNotNull(e, "Event cannot be null.");
            if (e.getClass() != clazz) {
                return;
            }
            E ev = (E) e;
            readWriteLock.readLock().lock();
            try {
                for (Entry<Object, List<Consumer<E>>> l : map.entrySet()) {
                    Object instance = l.getKey();
                    for (Consumer<E> m : l.getValue()) {
                        try {
                            m.accept(ev);
                        } catch (Throwable t) {
                            System.err.println("Event " + clazz.getSimpleName() + " in " + instance.getClass().getSimpleName() + " has thrown an error:");
                            t.printStackTrace();
                        }
                    }
                }
            } finally {
                readWriteLock.readLock().unlock();
            }
        }

    }

}
