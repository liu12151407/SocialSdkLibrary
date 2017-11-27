package com.march.socialsdk.platform.tencent;

import android.app.Activity;
import android.content.Intent;

import com.march.socialsdk.exception.SocialException;
import com.march.socialsdk.helper.AuthTokenKeeper;
import com.march.socialsdk.helper.JsonHelper;
import com.march.socialsdk.helper.PlatformLog;
import com.march.socialsdk.listener.OnLoginListener;
import com.march.socialsdk.model.LoginResult;
import com.march.socialsdk.model.token.QQAccessToken;
import com.march.socialsdk.model.user.QQUser;
import com.march.socialsdk.platform.Target;
import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONObject;

/**
 * CreateAt : 2016/12/6
 * Describe : qq 登录辅助
 *
 * @author chendong
 */

public class QQLoginHelper {

    public static final String TAG = QQLoginHelper.class.getSimpleName();

    private int             loginType;
    private Tencent         mTencentApi;
    private Activity        activity;
    private OnLoginListener onLoginListener;
    private LoginUiListener loginUiListener;


    QQLoginHelper(Activity activity, Tencent mTencentApi, OnLoginListener onQQLoginListener) {
        this.activity = activity;
        this.mTencentApi = mTencentApi;
        this.onLoginListener = onQQLoginListener;
        this.loginType = Target.LOGIN_QQ;
    }


    // 接受登录结果
    void handleResultData(Intent data) {
        Tencent.handleResultData(data, this.loginUiListener);
    }

    // 登录
    public void login() {
        QQAccessToken qqToken = AuthTokenKeeper.getQQToken(activity);
        if (qqToken != null) {
            mTencentApi.setAccessToken(qqToken.getAccess_token(), qqToken.getExpires_in() + "");
            mTencentApi.setOpenId(qqToken.getOpenid());
            if (mTencentApi.isSessionValid()) {
                getUserInfo(qqToken);
            } else {
                loginUiListener = new LoginUiListener();
                mTencentApi.login(activity, "all", loginUiListener);
            }
        } else {
            loginUiListener = new LoginUiListener();
            mTencentApi.login(activity, "all", loginUiListener);
        }
    }

    // 登录监听包装类
    private class LoginUiListener implements IUiListener {
        @Override
        public void onComplete(Object o) {
            JSONObject jsonResponse = (JSONObject) o;
            QQAccessToken qqToken = JsonHelper.getObject(jsonResponse.toString(), QQAccessToken.class);
            PlatformLog.e(TAG, "获取到 qq token = " + qqToken.toString());
            // 保存token
            AuthTokenKeeper.saveQQToken(activity, qqToken);

            mTencentApi.setAccessToken(qqToken.getAccess_token(), qqToken.getExpires_in() + "");
            mTencentApi.setOpenId(qqToken.getOpenid());

            getUserInfo(qqToken);
        }


        @Override
        public void onError(UiError e) {
            onLoginListener.onFailure(new SocialException("qq,获取用户信息失败", e));
        }

        @Override
        public void onCancel() {
            onLoginListener.onCancel();
        }
    }

    // 获取用户信息
    private void getUserInfo(final QQAccessToken qqToken) {
        UserInfo info = new UserInfo(activity, mTencentApi.getQQToken());
        info.getUserInfo(new IUiListener() {
            @Override
            public void onComplete(Object object) {
                PlatformLog.e(TAG, "qq 获取到用户信息 = " + object);
                QQUser qqUserInfo = JsonHelper.getObject(object.toString(), QQUser.class);
                qqUserInfo.setOpenId(mTencentApi.getOpenId());
                if (onLoginListener != null) {
                    onLoginListener.onLoginSucceed(new LoginResult(loginType, qqUserInfo, qqToken));
                }
            }

            @Override
            public void onError(UiError e) {
                onLoginListener.onFailure(new SocialException("qq,获取用户信息失败", e));
            }

            @Override
            public void onCancel() {
                onLoginListener.onCancel();
            }

        });
    }
}
