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

package org.bukkit.plugin;

import lombok.SneakyThrows;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginManagerImpl implements PluginManager {

    private final Map<Class<?>, List<EventExecutor>> map = new HashMap<>();

    public void registerEvent(Class<? extends Event> clazz, Listener listener, EventPriority eventPriority, EventExecutor eventExecutor, Plugin plugin) {
        List<EventExecutor> l = map.computeIfAbsent(clazz, c -> new LinkedList<>());
        l.add(eventExecutor);
    }

    @SneakyThrows
    @Override
    public void callEvent(@NotNull Event event) throws IllegalStateException {
        List<EventExecutor> l = map.get(event.getClass());
        if (l != null) {
            for (EventExecutor e : l) {
                e.execute(new Listener() {
                }, event);
            }
            if (event instanceof PluginDisableEvent) {
                map.clear();
            }
        }
    }

    @Override
    public void registerInterface(@NotNull Class<? extends PluginLoader> aClass) throws IllegalArgumentException {

    }

    @Nullable
    @Override
    public Plugin getPlugin(@NotNull String s) {
        return null;
    }

    @NotNull
    @Override
    public Plugin[] getPlugins() {
        return new Plugin[0];
    }

    @Override
    public boolean isPluginEnabled(@NotNull String s) {
        return false;
    }

    @Override
    public boolean isPluginEnabled(@Nullable Plugin plugin) {
        return false;
    }

    @Nullable
    @Override
    public Plugin loadPlugin(@NotNull File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        return null;
    }

    @NotNull
    @Override
    public Plugin[] loadPlugins(@NotNull File file) {
        return new Plugin[0];
    }

    @Override
    public void disablePlugins() {

    }

    @Override
    public void clearPlugins() {

    }

    @Override
    public void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin) {

    }

    @Override
    public void registerEvent(@NotNull Class<? extends Event> aClass, @NotNull Listener listener, @NotNull EventPriority eventPriority, @NotNull EventExecutor eventExecutor, @NotNull Plugin plugin, boolean b) {

    }

    @Override
    public void enablePlugin(@NotNull Plugin plugin) {

    }

    @Override
    public void disablePlugin(@NotNull Plugin plugin) {

    }

    @Nullable
    @Override
    public Permission getPermission(@NotNull String s) {
        return null;
    }

    @Override
    public void addPermission(@NotNull Permission permission) {

    }

    @Override
    public void removePermission(@NotNull Permission permission) {

    }

    @Override
    public void removePermission(@NotNull String s) {

    }

    @NotNull
    @Override
    public Set<Permission> getDefaultPermissions(boolean b) {
        return null;
    }

    @Override
    public void recalculatePermissionDefaults(@NotNull Permission permission) {

    }

    @Override
    public void subscribeToPermission(@NotNull String s, @NotNull Permissible permissible) {

    }

    @Override
    public void unsubscribeFromPermission(@NotNull String s, @NotNull Permissible permissible) {

    }

    @NotNull
    @Override
    public Set<Permissible> getPermissionSubscriptions(@NotNull String s) {
        return null;
    }

    @Override
    public void subscribeToDefaultPerms(boolean b, @NotNull Permissible permissible) {

    }

    @Override
    public void unsubscribeFromDefaultPerms(boolean b, @NotNull Permissible permissible) {

    }

    @NotNull
    @Override
    public Set<Permissible> getDefaultPermSubscriptions(boolean b) {
        return null;
    }

    @NotNull
    @Override
    public Set<Permission> getPermissions() {
        return null;
    }

    @Override
    public boolean useTimings() {
        return false;
    }
}
