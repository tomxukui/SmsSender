package com.xukui.library.smssender;

import androidx.annotation.Nullable;

public interface SavedStore {

    /**
     * 获取短信验证码倒计时的时间戳
     */
    long getTimestamp(@Nullable String event, @Nullable String mobile);

    /**
     * 记录短信验证码倒计时的时间戳
     */
    void saveTimestamp(@Nullable String event, @Nullable String mobile, long timestamp);

    /**
     * 清除短信验证码倒计时的时间戳
     */
    void clearTimestamp(@Nullable String event, @Nullable String mobile);

}
