/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.logging;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;

import com.android.launcher3.ButtonDropTarget;
import com.android.launcher3.DeleteDropTarget;
import com.android.launcher3.InfoDropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.UninstallDropTarget;
import com.android.launcher3.userevent.LauncherLogProto.Action;
import com.android.launcher3.userevent.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.LauncherLogProto.ControlType;
import com.android.launcher3.userevent.LauncherLogProto.ItemType;
import com.android.launcher3.userevent.LauncherLogProto.LauncherEvent;
import com.android.launcher3.userevent.LauncherLogProto.Target;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Helper methods for logging.
 * enum Touch {
 * TAP = 0;
 * LONGPRESS = 1;
 * DRAGDROP = 2;
 * SWIPE = 3;
 * FLING = 4;
 * PINCH = 5;
 * }
 */

public class LoggerUtils {
    private static final ArrayMap<Class, SparseArray<String>> sNameCache = new ArrayMap<>();
    private static final int Action_Touch_TAP = 0;
    private static final int Action_Touch_LONGPRESS = 1;
    private static final int Action_Touch_DRAGDROP = 2;
    private static final int Action_Touch_SWIPE = 3;
    private static final int Action_Touch_FLING = 4;
    private static final int Action_Touch_PINCH = 5;

    private static final String UNKNOWN = "UNKNOWN";

    public Target.Builder TargetBuilder() {
        return Target.newBuilder();
    }

    public static String getFieldName(int value, Class c) {
        SparseArray<String> cache;
        synchronized (sNameCache) {
            cache = sNameCache.get(c);
            if (cache == null) {
                cache = new SparseArray<>();
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == int.class && Modifier.isStatic(f.getModifiers())) {
                        try {
                            f.setAccessible(true);
                            cache.put(f.getInt(null), f.getName());
                        } catch (IllegalAccessException e) {
                            // Ignore
                        }
                    }
                }
                sNameCache.put(c, cache);
            }
        }
        String result = cache.get(value);
        return result != null ? result : UNKNOWN;
    }

    public static String getActionStr(Action action) {
        switch (action.getType()) {
            case TOUCH:
                return getFieldName(action.getTouch().getNumber(), Action.Touch.class);
            case COMMAND:
                return getFieldName(action.getCommand().getNumber(), Action.Command.class);
            default:
                return UNKNOWN;
        }
    }

    public static String getTargetStr(Target t) {
        if (t == null) {
            return "";
        }
        switch (t.getType()) {
            case ITEM:
                return getItemStr(t);
            case CONTROL:
                return getFieldName(t.getControlType().getNumber(), ControlType.class);
            case CONTAINER:
                String str = getFieldName(t.getContainerType().getNumber(), ContainerType.class);
                if (t.getContainerType() == ContainerType.WORKSPACE) {
                    str += " id=" + t.getPageIndex();
                } else if (t.getContainerType() == ContainerType.FOLDER) {
                    str += " grid(" + t.getGridX() + "," + t.getGridY() + ")";
                }
                return str;
            default:
                return "UNKNOWN TARGET TYPE";
        }
    }

    private static String getItemStr(Target t) {
        String typeStr = getFieldName(t.getItemType().getNumber(), ItemType.class);
        if (t.getPackageNameHash() != 0) {
            typeStr += ", packageHash=" + t.getPackageNameHash();
        }
        if (t.getComponentHash() != 0) {
            typeStr += ", componentHash=" + t.getComponentHash();
        }
        if (t.getIntentHash() != 0) {
            typeStr += ", intentHash=" + t.getIntentHash();
        }
        return typeStr + ", grid(" + t.getGridX() + "," + t.getGridY() + "), span(" + t.getSpanX() + "," + t.getSpanY() + "), pageIdx=" + t.getPageIndex();
    }

    public static Target.Builder newParentTargetBuild(int type) {
        Target.Builder srcParentBuilder = Target.newBuilder();
        srcParentBuilder.setType(Target.Type.forNumber(type));
        return srcParentBuilder;
    }

    public static Target newItemTarget(View v) {
        return (v.getTag() instanceof ItemInfo) ? newItemTarget((ItemInfo) v.getTag()) : newTarget(Target.Type.ITEM.getNumber());
    }

    public static Target newItemTarget(ItemInfo info) {
        ///Target t = newTarget(Target.Type.ITEM.getNumber());
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.ITEM);
        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                //t.itemType = ItemType.APP_ICON;
                builder.setItemType(ItemType.APP_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                //t.itemType = ItemType.SHORTCUT;
                builder.setItemType(ItemType.SHORTCUT);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                //t.itemType = ItemType.FOLDER_ICON;
                builder.setItemType(ItemType.FOLDER_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                //t.itemType = ItemType.WIDGET;
                builder.setItemType(ItemType.WIDGET);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                //t.itemType = ItemType.DEEPSHORTCUT;
                builder.setItemType(ItemType.DEEPSHORTCUT);
                break;
        }
        return builder.build();
    }

    public static Target.Builder newItemTargetBuilder(View v) {
        return (v.getTag() instanceof ItemInfo) ? newItemTargetBuilder((ItemInfo) v.getTag()) : newTargetBuilder(Target.Type.ITEM.getNumber());
    }

    public static Target.Builder newItemTargetBuilder(ItemInfo info) {
        ///Target t = newTarget(Target.Type.ITEM.getNumber());
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.ITEM);
        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                //t.itemType = ItemType.APP_ICON;
                builder.setItemType(ItemType.APP_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                //t.itemType = ItemType.SHORTCUT;
                builder.setItemType(ItemType.SHORTCUT);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                //t.itemType = ItemType.FOLDER_ICON;
                builder.setItemType(ItemType.FOLDER_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                //t.itemType = ItemType.WIDGET;
                builder.setItemType(ItemType.WIDGET);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                //t.itemType = ItemType.DEEPSHORTCUT;
                builder.setItemType(ItemType.DEEPSHORTCUT);
                break;
        }
        return builder;
    }

    public static Target newDropTarget(View v) {
        Target.Builder builder = Target.newBuilder();
        if (!(v instanceof ButtonDropTarget)) {
            ///return newTarget(Target.Type.CONTAINER);
            builder.setType(Target.Type.CONTAINER);
            return builder.build();
        }
        ///Target t = newTarget(Target.Type.CONTROL);
        builder.setType(Target.Type.CONTROL);
        if (v instanceof InfoDropTarget) {
            ///t.controlType = ControlType.APPINFO_TARGET;
            builder.setControlType(ControlType.APPINFO_TARGET);
        } else if (v instanceof UninstallDropTarget) {
            ///t.controlType = ControlType.UNINSTALL_TARGET;
            builder.setControlType(ControlType.UNINSTALL_TARGET);
        } else if (v instanceof DeleteDropTarget) {
            ///t.controlType = ControlType.REMOVE_TARGET;
            builder.setControlType(ControlType.REMOVE_TARGET);
        }
        return builder.build();
    }

    public static Target.Builder newDropTargetBuilder(View v) {
        if (!(v instanceof ButtonDropTarget)) {
            ///return newTarget(Target.Type.CONTAINER);
            return newTargetBuilder(Target.Type.CONTAINER.getNumber());
        }

        Target.Builder builder = Target.newBuilder();
        ///Target t = newTarget(Target.Type.CONTROL);
        builder.setType(Target.Type.CONTROL);
        if (v instanceof InfoDropTarget) {
            ///t.controlType = ControlType.APPINFO_TARGET;
            builder.setControlType(ControlType.APPINFO_TARGET);
        } else if (v instanceof UninstallDropTarget) {
            ///t.controlType = ControlType.UNINSTALL_TARGET;
            builder.setControlType(ControlType.UNINSTALL_TARGET);
        } else if (v instanceof DeleteDropTarget) {
            ///t.controlType = ControlType.REMOVE_TARGET;
            builder.setControlType(ControlType.REMOVE_TARGET);
        }
        return builder;
    }

    public static Target newTarget(int targetType) {
        ///Target t = new Target();
        ///t.type = targetType;
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.forNumber(targetType));
        return builder.build();
    }

    public static Target.Builder newTargetBuilder(int targetType) {
        ///Target t = new Target();
        ///t.type = targetType;
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.forNumber(targetType));
        return builder;
    }

    public static Target newContainerTarget(int containerType) {
        ///Target t = newTarget(Target.Type.CONTAINER.getNumber());
        ///t.containerType = containerType;
        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.CONTAINER);
        builder.setContainerType(ContainerType.forNumber(containerType));
        return builder.build();
    }

    public static Target.Builder newContainerTargetBuilder(int containerType) {
        ///Target t = newTarget(Target.Type.CONTAINER.getNumber());
        ///t.containerType = containerType;

        Target.Builder builder = Target.newBuilder();
        builder.setType(Target.Type.CONTAINER);
        builder.setContainerType(ContainerType.forNumber(containerType));
        return builder;
    }

    public static Action newAction(int type) {
        ///Action a = new Action();
        ///a.type = type;
        ///return a;
        return Action.newBuilder().setType(Action.Type.forNumber(type)).build();
    }

    public static Action newCommandAction(int command) {
        ///Action a = newAction(Action.Type.COMMAND);
        ///a.command = command;
        ///return a;
        return Action.newBuilder().setCommand(Action.Command.forNumber(command)).build();
    }

    public static Action newTouchAction(int touch) {
        ///Action a = newAction(Action.Type.TOUCH.getNumber());
        ///a.touch = touch;
        ///return a;
        return Action.newBuilder().setTouch(Action.Touch.forNumber(touch)).build();
    }

    public static Action.Builder newTouchActionBuilder(int touch) {
        ///Action a = newAction(Action.Type.TOUCH.getNumber());
        ///a.touch = touch;
        ///return a;
        return Action.newBuilder().setTouch(Action.Touch.forNumber(touch));
    }

    ///public static LauncherEvent newLauncherEvent(Action action, Target... srcTargets) {
    /// LauncherEvent event = new LauncherEvent();
    /// event.srcTarget = srcTargets;
    /// event.action = action;
    /// return event;
    ///}
    public static LauncherEvent newLauncherEvent(Action action, Target... srcTargets) {
        // 创建一个新的 LauncherEvent 对象
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        // 设置 action 字段
        eventBuilder.setAction(action);
        // 添加 srcTargets 到 srcTarget 字段中
        for (Target srcTarget : srcTargets) {
            eventBuilder.addSrcTarget(srcTarget);
        }
        // 构建最终的 LauncherEvent 对象并返回
        return eventBuilder.build();
    }

    public static LauncherEvent.Builder newLauncherEventBuild(Action action, Target... srcTargets) {
        // 创建一个新的 LauncherEvent 对象
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        // 设置 action 字段
        eventBuilder.setAction(action);
        // 添加 srcTargets 到 srcTarget 字段中
        for (Target srcTarget : srcTargets) {
            eventBuilder.addSrcTarget(srcTarget);
        }
        // 构建最终的 LauncherEvent 对象并返回
        return eventBuilder;
    }
}
