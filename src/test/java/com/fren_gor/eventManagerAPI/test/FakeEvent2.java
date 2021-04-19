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

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class FakeEvent2 extends Event {
    private static final HandlerList handlers = new Wrapper();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static class Wrapper extends HandlerList {

        @Getter
        private int unregisterCount = 0;

        @Override
        public synchronized void unregister(@NotNull Listener listener) {
            super.unregister(listener);
            unregisterCount++;
        }
    }

}
