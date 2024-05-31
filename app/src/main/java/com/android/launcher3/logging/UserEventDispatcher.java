/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.launcher3.logging.LoggerUtils.newCommandAction;
import static com.android.launcher3.logging.LoggerUtils.newContainerTargetBuilder;
import static com.android.launcher3.logging.LoggerUtils.newDropTargetBuilder;
import static com.android.launcher3.logging.LoggerUtils.newItemTarget;
import static com.android.launcher3.logging.LoggerUtils.newItemTargetBuilder;
import static com.android.launcher3.logging.LoggerUtils.newLauncherEvent;
import static com.android.launcher3.logging.LoggerUtils.newLauncherEventBuild;
import static com.android.launcher3.logging.LoggerUtils.newParentTargetBuild;
import static com.android.launcher3.logging.LoggerUtils.newTarget;
import static com.android.launcher3.logging.LoggerUtils.newTargetBuilder;
import static com.android.launcher3.logging.LoggerUtils.newTouchAction;
import static com.android.launcher3.logging.LoggerUtils.newTouchActionBuilder;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.ProviderConfig;
import com.android.launcher3.userevent.LauncherLogProto;
import com.android.launcher3.userevent.LauncherLogProto.Action;
import com.android.launcher3.userevent.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.LauncherLogProto.LauncherEvent;
import com.android.launcher3.userevent.LauncherLogProto.Target;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.LogConfig;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Manages the creation of {@link LauncherEvent}.
 * To debug this class, execute following command before side loading a new apk.
 * <p>
 * $ adb shell setprop log.tag.UserEvent VERBOSE
 */
public class UserEventDispatcher {

    private final static int MAXIMUM_VIEW_HIERARCHY_LEVEL = 5;

    private static final String TAG = "UserEvent";
    private static final boolean IS_VERBOSE = ProviderConfig.IS_DOGFOOD_BUILD && Utilities.isPropertyEnabled(LogConfig.USEREVENT);

    public static UserEventDispatcher newInstance(Context context, boolean isInLandscapeMode, boolean isInMultiWindowMode) {
        UserEventDispatcher ued = Utilities.getOverrideObject(UserEventDispatcher.class, context.getApplicationContext(), R.string.user_event_dispatcher_class);
        ued.mIsInLandscapeMode = isInLandscapeMode;
        ued.mIsInMultiWindowMode = isInMultiWindowMode;
        return ued;
    }

    /**
     * Implemented by containers to provide a container source for a given child.
     */
    public interface LogContainerProvider {

        /**
         * Copies data from the source to the destination proto.
         *
         * @param v            source of the data
         * @param info         source of the data
         * @param target       dest of the data
         * @param targetParent dest of the data
         */
        void fillInLogContainerData(LauncherEvent.Builder eventBuilder, Target.Builder srcTargetBuilder, Target.Builder srcParentBuilder, View v, ItemInfo info);
    }

    /**
     * Recursively finds the parent of the given child which implements IconLogInfoProvider
     */
    public static LogContainerProvider getLaunchProviderRecursive(View v) {
        ViewParent parent;
        if (v != null) {
            parent = v.getParent();
        } else {
            return null;
        }

        // Optimization to only check up to 5 parents.
        int count = MAXIMUM_VIEW_HIERARCHY_LEVEL;
        while (parent != null && count-- > 0) {
            if (parent instanceof LogContainerProvider) {
                return (LogContainerProvider) parent;
            } else {
                parent = parent.getParent();
            }
        }
        return null;
    }

    private long mElapsedContainerMillis;
    private long mElapsedSessionMillis;
    private long mActionDurationMillis;
    private boolean mIsInMultiWindowMode;
    private boolean mIsInLandscapeMode;

    // Used for filling in predictedRank on {@link Target}s.
    private List<ComponentKey> mPredictedApps;

    //                      APP_ICON    SHORTCUT    WIDGET
    // --------------------------------------------------------------
    // packageNameHash      required    optional    required
    // componentNameHash    required                required
    // intentHash                       required
    // --------------------------------------------------------------
    protected LauncherEvent createLauncherEvent0(View v, int intentHashCode, ComponentName cn) {
        LauncherEvent event = newLauncherEvent(newTouchAction(Action.Touch.TAP.getNumber()), newItemTarget(v), newTarget(Target.Type.CONTAINER.getNumber()));
        // TODO: make idx percolate up the view hierarchy if needed.
        int idx = 0;
        /*if (fillInLogContainerData(event, v)) {
            ItemInfo itemInfo = (ItemInfo) v.getTag();
            event.srcTarget[idx].intentHash = intentHashCode;
            if (cn != null) {
                event.srcTarget[idx].packageNameHash = cn.getPackageName().hashCode();
                event.srcTarget[idx].componentHash = cn.hashCode();
                if (mPredictedApps != null) {
                    event.srcTarget[idx].predictedRank = mPredictedApps.indexOf(
                            new ComponentKey(cn, itemInfo.user));
                }
            }
        }*/
        return event;
    }

    protected LauncherEvent.Builder createLauncherEventBuilder(View v, int intentHashCode, ComponentName cn) {
        // 创建一个新的 LauncherEvent.Builder 对象
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        // 创建并填充 Target 对象
        Target.Builder srcTargetBuilder = newItemTargetBuilder(v);
        Target.Builder srcParentBuilder = newParentTargetBuild(Target.Type.CONTAINER.getNumber());

        // 设置 Target 的其他字段
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        srcTargetBuilder.setIntentHash(intentHashCode);
        if (cn != null) {
            srcTargetBuilder.setPackageNameHash(cn.getPackageName().hashCode());
            srcTargetBuilder.setComponentHash(cn.hashCode());
            if (mPredictedApps != null) {
                srcTargetBuilder.setPredictedRank(mPredictedApps.indexOf(new ComponentKey(cn, itemInfo.user)));
            }
        }
        // 创建 Action 对象
        Action action = newTouchAction(Action.Touch.TAP.getNumber());
        eventBuilder.setAction(action);

        // 创建并填充 Target 对象
        fillInLogContainerData(eventBuilder, srcTargetBuilder, srcParentBuilder, v);
        // 添加到 eventBuilder
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        eventBuilder.addSrcTarget(srcParentBuilder.build());
        // 构建最终的 LauncherEvent 对象
        return eventBuilder;
    }

    public boolean fillInLogContainerData(LauncherEvent.Builder eventBuilder, Target.Builder srcTargetBuilder, Target.Builder srcParentBuilder, View v) {
        // Fill in grid(x,y), pageIndex of the child and container type of the parent
        LogContainerProvider provider = getLaunchProviderRecursive(v);
        if (v == null || !(v.getTag() instanceof ItemInfo) || provider == null) {
            return false;
        }
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        provider.fillInLogContainerData(eventBuilder, srcTargetBuilder, srcParentBuilder, v, itemInfo);
        return true;
    }

    public void logAppLaunch(View v, Intent intent) {
        LauncherEvent.Builder builder = createLauncherEventBuilder(v, intent.hashCode(), intent.getComponent());
        if (builder == null) {
            return;
        }
        dispatchUserEvent(builder, intent);
    }

    public void logNotificationLaunch(View v, PendingIntent intent) {
        ComponentName dummyComponent = new ComponentName(Objects.requireNonNull(intent.getCreatorPackage()), "--dummy--");
        LauncherEvent.Builder builder = createLauncherEventBuilder(v, intent.hashCode(), dummyComponent);
        if (builder == null) {
            return;
        }
        dispatchUserEvent(builder, null);
    }

    public void logActionCommand(int command, int containerType) {
        logActionCommand(command, containerType, 0);
    }

    public void logActionCommand(int command, int containerType, int pageIndex) {
        ///LauncherEvent event = newLauncherEvent(newCommandAction(command), newContainerTarget(containerType));
        ///event.srcTarget[0].pageIndex = pageIndex;
        ///dispatchUserEvent(event, null);

        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        eventBuilder.setAction(newCommandAction(command));
        // 创建并填充 Target 对象
        Target.Builder srcTargetBuilder = newContainerTargetBuilder(containerType);
        srcTargetBuilder.setType(Target.Type.forNumber(containerType));
        srcTargetBuilder.setPageIndex(pageIndex);
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    /**
     * TODO: Make this function work when a container view is passed as the 2nd param.
     */
    public void logActionCommand(int command, View itemView, int containerType) {
        ///LauncherEvent event = newLauncherEvent(newCommandAction(command), newItemTarget(itemView), newTarget(Target.Type.CONTAINER));
        // 创建一个新的 LauncherEvent.Builder 对象
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        // 创建并填充 Target 对象
        Target.Builder srcTargetBuilder = newItemTargetBuilder(itemView);
        Target.Builder srcParentBuilder = newParentTargetBuild(Target.Type.CONTAINER.getNumber());

        if (fillInLogContainerData(eventBuilder, srcTargetBuilder, srcParentBuilder, itemView)) {
            // TODO: Remove the following two lines once fillInLogContainerData can take in a
            // container view.
            ///event.srcTarget[0].type = Target.Type.CONTAINER;
            ///event.srcTarget[0].containerType = containerType;
            srcTargetBuilder.setType(Target.Type.CONTAINER);
            srcTargetBuilder.setContainerType(ContainerType.forNumber(containerType));
            eventBuilder.addSrcTarget(srcTargetBuilder.build());
            eventBuilder.addSrcTarget(srcParentBuilder.build());
        }
        dispatchUserEvent(eventBuilder, null);
    }

    public void logActionOnControl(int action, int controlType) {
        ///LauncherEvent event = newLauncherEvent(newTouchAction(action), newTarget(Target.Type.CONTROL));
        ///event.srcTarget[0].controlType = controlType;
        ///dispatchUserEvent(event, null);
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();// 创建并填充 Target 对象
        Target.Builder srcTargetBuilder = newTargetBuilder(Target.Type.CONTROL.getNumber());
        srcTargetBuilder.setType(Target.Type.CONTAINER);
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    public void logActionTapOutside(Target target) {
        ///LauncherEvent event = newLauncherEvent(newTouchAction(Action.Type.TOUCH), target);
        ///event.action.isOutside = true;
        ///dispatchUserEvent(event, null);
        LauncherEvent.Builder eventBuilder = newLauncherEventBuild(newTouchAction(Action.Type.TOUCH.getNumber()), target);// 创建并填充 Target 对象
        Action.Builder actionBuilder = newTouchActionBuilder(Action.Type.TOUCH.getNumber());
        actionBuilder.setIsOutside(true);
        eventBuilder.setAction(actionBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    public void logActionOnContainer(int action, int dir, int containerType) {
        logActionOnContainer(action, dir, containerType, 0);
    }

    public void logActionOnContainer(int action, int dir, int containerType, int pageIndex) {
        ///LauncherEvent event = newLauncherEvent(newTouchAction(action), newContainerTarget(containerType));
        ///event.action.dir = dir;
        ///event.srcTarget[0].pageIndex = pageIndex;
        ///dispatchUserEvent(event, null);
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        Action.Builder actionBuilder = newTouchActionBuilder(action);
        actionBuilder.setDir(Action.Direction.forNumber(dir));
        eventBuilder.setAction(actionBuilder.build());
        Target.Builder srcTargetBuilder = newContainerTargetBuilder(containerType);
        srcTargetBuilder.setPageIndex(pageIndex);
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    public void logActionOnItem(int action, int dir, int itemType) {
        ///Target itemTarget = newTarget(Target.Type.ITEM);
        ///itemTarget.itemType = itemType;
        ///LauncherEvent event = newLauncherEvent(newTouchAction(action), itemTarget);
        ///event.action.dir = dir;
        ///dispatchUserEvent(event, null);
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder();
        Action.Builder actionBuilder = newTouchActionBuilder(action);
        actionBuilder.setDir(Action.Direction.forNumber(dir));
        eventBuilder.setAction(actionBuilder.build());
        Target.Builder srcTargetBuilder = newTargetBuilder(Target.Type.ITEM.getNumber());
        srcTargetBuilder.setItemType(LauncherLogProto.ItemType.forNumber(itemType));
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    public void logDeepShortcutsOpen(View icon) {
        LogContainerProvider provider = getLaunchProviderRecursive(icon);
        if (icon == null || !(icon.getTag() instanceof ItemInfo)) {
            return;
        }
        ItemInfo info = (ItemInfo) icon.getTag();
        ///LauncherEvent event = newLauncherEvent(newTouchAction(Action.Touch.LONGPRESS), newItemTarget(info), newTarget(Target.Type.CONTAINER));
        ///provider.fillInLogContainerData(icon, info, event.srcTarget[0], event.srcTarget[1]);
        ///dispatchUserEvent(event, null);

        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder(); // 创建并填充 Target 对象
        Action.Builder actionBuilder = newTouchActionBuilder(Action.Touch.LONGPRESS.getNumber());
        Target.Builder srcTargetBuilder = newItemTargetBuilder(info);
        Target.Builder srcParentBuilder = newParentTargetBuild(Target.Type.CONTAINER.getNumber());

        provider.fillInLogContainerData(eventBuilder, srcTargetBuilder, srcParentBuilder, icon, info);

        eventBuilder.setAction(actionBuilder.build());
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        eventBuilder.addSrcTarget(srcParentBuilder.build());
        dispatchUserEvent(eventBuilder, null);
        resetElapsedContainerMillis();
    }

    public void setPredictedApps(List<ComponentKey> predictedApps) {
        mPredictedApps = predictedApps;
    }

    /* Currently we are only interested in whether this event happens or not and don't
     * care about which screen moves to where. */
    public void logOverviewReorder() {
        ///LauncherEvent event = newLauncherEvent(newTouchAction(Action.Touch.DRAGDROP), newContainerTarget(ContainerType.WORKSPACE), newContainerTarget(ContainerType.OVERVIEW));
        ///dispatchUserEvent(event, null);
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder(); // 创建并填充 Target 对象
        Action.Builder actionBuilder = newTouchActionBuilder(Action.Touch.DRAGDROP.getNumber());
        Target.Builder srcTargetBuilder = newContainerTargetBuilder(ContainerType.WORKSPACE.getNumber());
        Target.Builder srcParentBuilder = newContainerTargetBuilder(ContainerType.OVERVIEW.getNumber());
        eventBuilder.setAction(actionBuilder.build());
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        eventBuilder.addSrcTarget(srcParentBuilder.build());
        dispatchUserEvent(eventBuilder, null);
    }

    public void logDragNDrop(DropTarget.DragObject dragObj, View dropTargetAsView) {
        /*LauncherEvent event = newLauncherEvent(newTouchAction(Action.Touch.DRAGDROP), newItemTarget(dragObj.originalDragInfo), newTarget(Target.Type.CONTAINER));
        event.destTarget = new Target[]{
                newItemTarget(dragObj.originalDragInfo), newDropTarget(dropTargetAsView)
        };

        dragObj.dragSource.fillInLogContainerData(null, dragObj.originalDragInfo, event.srcTarget[0], event.srcTarget[1]);

        if (dropTargetAsView instanceof LogContainerProvider) {
            ((LogContainerProvider) dropTargetAsView).fillInLogContainerData(null, dragObj.dragInfo, event.destTarget[0], event.destTarget[1]);

        }
        event.actionDurationMillis = SystemClock.uptimeMillis() - mActionDurationMillis;
        dispatchUserEvent(event, null);*/
        LauncherEvent.Builder eventBuilder = LauncherEvent.newBuilder(); // 创建并填充 Target 对象
        Action.Builder actionBuilder = newTouchActionBuilder(Action.Touch.DRAGDROP.getNumber());
        Target.Builder srcTargetBuilder = newItemTargetBuilder(dragObj.originalDragInfo);
        Target.Builder srcParentBuilder = newTargetBuilder(Target.Type.CONTAINER.getNumber());
        Target.Builder desTargetBuilder = newItemTargetBuilder(dragObj.originalDragInfo);
        Target.Builder desParentBuilder = newDropTargetBuilder(dropTargetAsView);

        eventBuilder.setAction(actionBuilder.build());
        if (dropTargetAsView instanceof LogContainerProvider) {
            ((LogContainerProvider) dropTargetAsView).fillInLogContainerData(eventBuilder, desTargetBuilder, desParentBuilder, null, dragObj.dragInfo);
        }
        eventBuilder.addSrcTarget(srcTargetBuilder.build());
        eventBuilder.addSrcTarget(srcParentBuilder.build());
        eventBuilder.addDestTarget(desTargetBuilder.build());
        eventBuilder.addDestTarget(desParentBuilder.build());
        eventBuilder.setActionDurationMillis(SystemClock.uptimeMillis() - mActionDurationMillis);
        dispatchUserEvent(eventBuilder, null);
    }

    /**
     * Currently logs following containers: workspace, allapps, widget tray.
     */
    public final void resetElapsedContainerMillis() {
        mElapsedContainerMillis = SystemClock.uptimeMillis();
    }

    public final void resetElapsedSessionMillis() {
        mElapsedSessionMillis = SystemClock.uptimeMillis();
        mElapsedContainerMillis = SystemClock.uptimeMillis();
    }

    public final void resetActionDurationMillis() {
        mActionDurationMillis = SystemClock.uptimeMillis();
    }

    public void dispatchUserEvent(LauncherEvent.Builder eventBuilder, Intent intent) {
        ///ev.isInLandscapeMode = mIsInLandscapeMode;
        ///ev.isInMultiWindowMode = mIsInMultiWindowMode;
        ///ev.elapsedContainerMillis = SystemClock.uptimeMillis() - mElapsedContainerMillis;
        ///ev.elapsedSessionMillis = SystemClock.uptimeMillis() - mElapsedSessionMillis;
        eventBuilder.setIsInLandscapeMode(mIsInLandscapeMode);
        eventBuilder.setIsInMultiWindowMode(mIsInMultiWindowMode);
        eventBuilder.setElapsedContainerMillis(SystemClock.uptimeMillis() - mElapsedContainerMillis);
        eventBuilder.setElapsedSessionMillis(SystemClock.uptimeMillis() - mElapsedSessionMillis);
        LauncherEvent launcherEvent = eventBuilder.build();
        if (!IS_VERBOSE) {
            return;
        }
        ///String log = "action:" + LoggerUtils.getActionStr(ev.action);
        ///if (launcherEvent.getSrcTarget().srcTarget != null && ev.srcTarget.length > 0) {
        ///    log += "\n Source " + getTargetsStr(ev.srcTarget);
        ///}
        ///if (ev.destTarget != null && ev.destTarget.length > 0) {
        ///    log += "\n Destination " + getTargetsStr(ev.destTarget);
        ///}
        ///log += String.format(Locale.US, "\n Elapsed container %d ms session %d ms action %d ms", ev.elapsedContainerMillis, ev.elapsedSessionMillis, ev.actionDurationMillis);
        ///log += "\n isInLandscapeMode " + ev.isInLandscapeMode;
        ///log += "\n isInMultiWindowMode " + ev.isInMultiWindowMode;
        ///Log.d(TAG, log);
        // 构建日志字符串
        StringBuilder log = new StringBuilder("action:" + LoggerUtils.getActionStr(launcherEvent.getAction()));

        // 检查并添加 srcTarget 日志
        if (launcherEvent.getSrcTargetList() != null && !launcherEvent.getSrcTargetList().isEmpty()) {
            log.append("\n Source ").append(getTargetsStr(launcherEvent.getSrcTargetList()));
        }

        // 检查并添加 destTarget 日志
        if (launcherEvent.getDestTargetList() != null && !launcherEvent.getDestTargetList().isEmpty()) {
            log.append("\n Destination ").append(getTargetsStr(launcherEvent.getDestTargetList()));
        }

        // 添加其他信息到日志
        log.append(String.format(Locale.US, "\n Elapsed container %d ms session %d ms action %d ms", launcherEvent.getElapsedContainerMillis(), launcherEvent.getElapsedSessionMillis(), launcherEvent.getActionDurationMillis()));
        log.append("\n isInLandscapeMode ").append(launcherEvent.getIsInLandscapeMode());
        log.append("\n isInMultiWindowMode ").append(launcherEvent.getIsInMultiWindowMode());

        // 输出日志
        Log.d(TAG, log.toString());
    }

    private static String getTargetsStr(Target[] targets) {
        return "child:" + LoggerUtils.getTargetStr(targets[0]) + (targets.length > 1 ? "\tparent:" + LoggerUtils.getTargetStr(targets[1]) : "");
    }

    private static String getTargetsStr(List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return "No targets";
        }

        StringBuilder result = new StringBuilder("child:").append(LoggerUtils.getTargetStr(targets.get(0)));
        if (targets.size() > 1) {
            result.append("\tparent:").append(LoggerUtils.getTargetStr(targets.get(1)));
        }
        return result.toString();
    }
}
