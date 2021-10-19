/*
 * This file is a part of project QuickShop, the name is ReloadManager.java
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.util.reload;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/**
 * ReloadManager controls modules to reloading while needed
 * <p>
 * Register order is reloading order preventing unexpected behavior.
 */
public class ReloadManager {
    private final List<ReloadableContainer> registry = new ArrayList<>();

    /**
     * Register a reloadable module into reloading registery
     *
     * @param reloadable Reloadable module
     */
    public void register(@NotNull Reloadable reloadable) {
        unregister(reloadable);
        this.registry.add(new ReloadableContainer(new WeakReference<>(reloadable), null));
    }

    /**
     * Register a reloadable module into reloading registery
     *
     * @param reloadMethod Reloadable module
     */
    public void register(@NotNull Method reloadMethod) {
        unregister(reloadMethod);
        this.registry.add(new ReloadableContainer(null, new WeakReference<>(reloadMethod)));
    }

    /**
     * Register a reloadable module into reloading registery
     *
     * @param reloadMethod Reloadable module
     */
    public void unregister(@NotNull Method reloadMethod) {
        this.registry.removeIf(reloadableContainer -> {
            if (reloadableContainer.getReloadableMethod() != null) {
                Method method = reloadableContainer.getReloadableMethod().get();
                return reloadMethod.equals(method);
            } else {
                return false;
            }
        });
    }


    /**
     * Unregister a reloadable module from reloading registry
     *
     * @param reloadable Reloadable module
     */
    public void unregister(@NotNull Reloadable reloadable) {
        this.registry.removeIf(reloadableContainer -> reloadableContainer != null && Objects.equals(reloadableContainer.getReloadable(), reloadable));
    }

    /**
     * Unregister all reloadable modules that same with specific class from reloading registry
     *
     * @param clazz Class that impl reloadable
     */
    public void unregister(@NotNull Class<Reloadable> clazz) {
        this.registry.removeIf(reloadable -> {
            if (reloadable.getReloadable() != null) {
                return clazz.equals(reloadable.getReloadable().getClass());
            }
            if (reloadable.getReloadableMethod() != null) {
                Method method = reloadable.getReloadableMethod().get();
                if (method != null)
                    return clazz.equals(method.getDeclaringClass());
            }
            return false;
        });
    }

    /**
     * Reload all reloadable modules
     *
     * @return Reloading results
     */
    @NotNull
    public Map<ReloadableContainer, ReloadResult> reload() {
        return reload(null);
    }

    /**
     * Reload all reloadable modules that equals specific class
     *
     * @param clazz The class that impl reloadable
     * @return Reloading results
     */
    @NotNull
    public Map<ReloadableContainer, ReloadResult> reload(@Nullable Class<Reloadable> clazz) {
        Map<ReloadableContainer, ReloadResult> reloadResultMap = new HashMap<>();
        Iterator<ReloadableContainer> iterator = this.registry.iterator();
        while (iterator.hasNext()) {
            ReloadableContainer reloadable = iterator.next();
            if (clazz != null) {
                if (reloadable.getReloadable() != null) {
                    if (!clazz.equals(reloadable.getReloadable().getClass())) {
                        continue;
                    }
                }
                if (reloadable.getReloadableMethod() != null) {
                    Method method = reloadable.getReloadableMethod().get();
                    if (method != null) {
                        if (!clazz.equals(method.getDeclaringClass())) {
                            continue;
                        }
                    }
                }
            }
            ReloadResult reloadResult;
            try {
                if (reloadable.getReloadable() != null) {
                    Reloadable reloadObj = reloadable.getReloadable().get();
                    if (reloadObj != null) {
                        reloadResult = reloadObj.reloadModule();
                    } else {
                        iterator.remove();
                        reloadResult = new ReloadResult(ReloadStatus.OUTDATED, "Object has been invalid", null);
                    }
                } else if (reloadable.getReloadableMethod() != null) {
                    Method method = reloadable.getReloadableMethod().get();
                    if (method != null) {
                        reloadResult = (ReloadResult) method.invoke(null);
                    } else {
                        iterator.remove();
                        reloadResult = new ReloadResult(ReloadStatus.OUTDATED, "Method has been invalid", null);
                    }
                } else {
                    reloadResult = new ReloadResult(ReloadStatus.EXCEPTION, "Both reloadable and method not exists", null);
                }
            } catch (Exception exception) {
                reloadResult = new ReloadResult(ReloadStatus.EXCEPTION, "Reloading failed", exception);
            }
            reloadResultMap.put(reloadable, reloadResult);
        }
        return reloadResultMap;
    }
}
