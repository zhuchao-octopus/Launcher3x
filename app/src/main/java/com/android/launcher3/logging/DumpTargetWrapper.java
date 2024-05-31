/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.LauncherDumpProto;
import com.android.launcher3.model.LauncherDumpProto.ContainerType;
import com.android.launcher3.model.LauncherDumpProto.DumpTarget;
import com.android.launcher3.model.LauncherDumpProto.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used when proto definition doesn't support nesting.
 */
public class DumpTargetWrapper {
    Context mContext;
    ///DumpTarget node;
    DumpTarget.Builder builder;
    ArrayList<DumpTargetWrapper> children;

    public DumpTargetWrapper() {
        children = new ArrayList<>();
    }

    public DumpTargetWrapper(int containerType, int id) {
        this();
        ///node = newContainerTarget(containerType, id);
        builder = newContainerTargetBuilder(containerType, id);
    }

    public DumpTargetWrapper(ItemInfo info) {
        this();
        ///node = newItemTarget(info);
        builder = newItemTargetBuilder(info);
    }

    public DumpTarget getDumpTarget() {
        return builder.build();
    }

    public void add(DumpTargetWrapper child) {
        children.add(child);
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public List<DumpTarget> getFlattenedList() {
        ArrayList<DumpTarget> list = new ArrayList<>();
        list.add(builder.build());
        if (!children.isEmpty()) {
            for (DumpTargetWrapper t : children) {
                list.addAll(t.getFlattenedList());
            }
            list.add(builder.build()); // add a delimiter empty object
        }
        return list;
    }

    public DumpTarget.Builder newItemTargetBuilder(ItemInfo info) {
        ///DumpTarget dt = new DumpTarget();
        ///dt.type = DumpTarget.Type.ITEM;
        DumpTarget.Builder builder = DumpTarget.newBuilder();
        builder.setType(DumpTarget.Type.ITEM);

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                ///dt.itemType = ItemType.APP_ICON;
                builder.setItemType(ItemType.APP_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                ///dt.itemType = ItemType.UNKNOWN_ITEMTYPE;
                builder.setItemType(ItemType.UNKNOWN_ITEMTYPE);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                ///dt.itemType = ItemType.WIDGET;
                builder.setItemType(ItemType.WIDGET);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                ///dt.itemType = ItemType.SHORTCUT;
                builder.setItemType(ItemType.SHORTCUT);
                break;
        }
        return builder;
    }

    public DumpTarget newItemTarget(ItemInfo info) {
        ///DumpTarget dt = new DumpTarget();
        ///dt.type = DumpTarget.Type.ITEM;
        DumpTarget.Builder builder = DumpTarget.newBuilder();
        builder.setType(DumpTarget.Type.ITEM);

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                ///dt.itemType = ItemType.APP_ICON;
                builder.setItemType(ItemType.APP_ICON);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                ///dt.itemType = ItemType.UNKNOWN_ITEMTYPE;
                builder.setItemType(ItemType.UNKNOWN_ITEMTYPE);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                ///dt.itemType = ItemType.WIDGET;
                builder.setItemType(ItemType.WIDGET);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                ///dt.itemType = ItemType.SHORTCUT;
                builder.setItemType(ItemType.SHORTCUT);
                break;
        }
        return builder.build();
    }

    public DumpTarget newContainerTarget(int type, int id) {
        ///DumpTarget dt = new DumpTarget();
        ///dt.type = DumpTarget.Type.CONTAINER;
        ///dt.containerType = type;
        ///dt.pageId = id;
        ///return dt;
        DumpTarget.Builder builder = DumpTarget.newBuilder();
        builder.setType(DumpTarget.Type.CONTAINER);
        builder.setContainerType(ContainerType.forNumber(type));
        builder.setPageId(id);
        return builder.build();
    }

    public DumpTarget.Builder newContainerTargetBuilder(int type, int id) {
        ///DumpTarget dt = new DumpTarget();
        ///dt.type = DumpTarget.Type.CONTAINER;
        ///dt.containerType = type;
        ///dt.pageId = id;
        ///return dt;
        DumpTarget.Builder builder = DumpTarget.newBuilder();
        builder.setType(DumpTarget.Type.CONTAINER);
        builder.setContainerType(ContainerType.forNumber(type));
        builder.setPageId(id);
        return builder;
    }

    public static String getDumpTargetStr(DumpTarget t) {
        if (t == null) {
            return "";
        }
        switch (t.getType()) {
            case ITEM:
                return getItemStr(t);
            case CONTAINER:
                String str = LoggerUtils.getFieldName(t.getContainerType().getNumber(), ContainerType.class);
                if (t.getContainerType() == ContainerType.WORKSPACE) {
                    str += " id=" + t.getPageId();
                } else if (t.getContainerType() == ContainerType.FOLDER) {
                    str += " grid(" + t.getGridX() + "," + t.getGridY() + ")";
                }
                return str;
        }
        return "UNKNOWN TARGET TYPE";
    }

    private static String getItemStr(DumpTarget t) {
        String typeStr = LoggerUtils.getFieldName(t.getItemType().getNumber(), ItemType.class);
        if (!TextUtils.isEmpty(t.getPackageName())) {
            typeStr += ", package=" + t.getPackageName();
        }
        if (!TextUtils.isEmpty(t.getComponent())) {
            typeStr += ", component=" + t.getComponent();
        }
        return typeStr + ", grid(" + t.getGridX() + "," + t.getGridY() + "), span(" + t.getSpanX() + "," + t.getSpanY() + "), pageIdx=" + t.getPageId() + " user=" + t.getUserType();
    }

    public DumpTarget writeToDumpTarget(ItemInfo info) {
        /*node.component = info.getTargetComponent() == null? "":
                info.getTargetComponent().flattenToString();
        node.packageName = info.getTargetComponent() == null? "":
                info.getTargetComponent().getPackageName();
        if (info instanceof LauncherAppWidgetInfo) {
            node.component = ((LauncherAppWidgetInfo) info).providerName.flattenToString();
            node.packageName = ((LauncherAppWidgetInfo) info).providerName.getPackageName();
        }

        node.gridX = info.cellX;
        node.gridY = info.cellY;
        node.spanX = info.spanX;
        node.spanY = info.spanY;
        node.userType = (info.user.equals(Process.myUserHandle()))? UserType.DEFAULT : UserType.WORK;
        return node;*/

        builder.setComponent(info.getTargetComponent() == null ? "" : info.getTargetComponent().flattenToString());
        builder.setPackageName(info.getTargetComponent() == null ? "" : info.getTargetComponent().getPackageName());
        if (info instanceof LauncherAppWidgetInfo) {
            builder.setComponent(((LauncherAppWidgetInfo) info).providerName.flattenToString());
            builder.setPackageName(((LauncherAppWidgetInfo) info).providerName.getPackageName());
        }
        builder.setGridX(info.cellX);builder.setGridY(info.cellY);
        builder.setSpanX(info.spanX);
        builder.setSpanY(info.spanY);
        if(mContext != null) {
            UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            // Get the serial number of the current user
            long currentUserSerial = userManager.getSerialNumberForUser(android.os.Process.myUserHandle());
            // Get the serial number of info.user
            long infoUserSerial = userManager.getSerialNumberForUser(info.user);
        }
        builder.setUserType((info.user.equals(android.os.Process.myUserHandle()))? LauncherDumpProto.UserType.DEFAULT : LauncherDumpProto.UserType.WORK);
        return builder.build();
    }
}
