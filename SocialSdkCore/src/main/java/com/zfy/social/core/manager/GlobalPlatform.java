package com.zfy.social.core.manager;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.zfy.social.core.SocialSdk;
import com.zfy.social.core.common.Target;
import com.zfy.social.core.platform.IPlatform;
import com.zfy.social.core.platform.PlatformFactory;
import com.zfy.social.core.uikit.BaseActionActivity;

/**
 * CreateAt : 2017/5/19
 * Describe : 静态持有 platform, 在流程结束后会回收所有资源
 *
 * @author chendong
 */
public class GlobalPlatform {

    public static final int INVALID_PARAM = -1;

    public static final int ACTION_TYPE_LOGIN = 0;
    public static final int ACTION_TYPE_SHARE = 1;

    public static final String KEY_SHARE_MEDIA_OBJ = "KEY_SHARE_MEDIA_OBJ"; // media obj key
    public static final String KEY_ACTION_TYPE = "KEY_ACTION_TYPE"; // action type

    public static final String KEY_SHARE_TARGET = "KEY_SHARE_TARGET"; // share target
    public static final String KEY_LOGIN_TARGET = "KEY_LOGIN_TARGET"; // login target

    private static IPlatform sIPlatform;

    static @NonNull
    IPlatform makePlatform(Context context, int target) {
        if (SocialSdk.opts() == null) {
            throw new IllegalArgumentException(Target.toDesc(target) + " SocialSdk.init() request");
        }
        IPlatform platform = getPlatform(context, target);
        if (platform == null) {
            throw new IllegalArgumentException(Target.toDesc(target) + "  创建platform失败，请检查参数 " + SocialSdk.opts().toString());
        }
        sIPlatform = platform;
        return platform;
    }

    // 获取当前持有的 platform
    public static IPlatform getPlatform() {
        return sIPlatform;
    }


    private static IPlatform getPlatform(Context context, int target) {
        PlatformFactory platformFactory = null;
        SparseArray<PlatformFactory> factories = SocialSdk.getPlatformFactories();
        for (int i = 0; i < factories.size(); i++) {
            PlatformFactory factory = factories.valueAt(i);
            if (factory.checkLoginTarget(target) || factory.checkShareTarget(target)) {
                platformFactory = factory;
                break;
            }
        }
        if (platformFactory != null) {
            return platformFactory.create(context, target);
        }
        return null;
    }

    // 释放资源
    public static void release(Activity activity) {
        if (sIPlatform != null) {
            sIPlatform.recycle();
            sIPlatform = null;
        }
        if (activity != null) {
            if (activity instanceof BaseActionActivity) {
                ((BaseActionActivity) activity).checkFinish(false);
            }
        }
    }

    // 触发操作
    public static void action(Activity activity, int actionType) {
        if (actionType == -1) {
            return;
        }
        switch (actionType) {
            case GlobalPlatform.ACTION_TYPE_LOGIN:
                LoginManager._actionLogin(activity);
                break;
            case GlobalPlatform.ACTION_TYPE_SHARE:
                ShareManager._actionShare(activity);
                break;
        }
    }

    public static void onUIFinished() {
        ShareManager.postFinishEvent();
    }
}
