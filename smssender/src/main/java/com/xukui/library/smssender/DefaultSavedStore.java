package com.xukui.library.smssender;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class DefaultSavedStore implements SavedStore {

    private static final String SMS_CODE_TIMESTAMP = "_SMS_CODE_TIMESTAMP";//短信验证码倒计时的时间戳

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public DefaultSavedStore(Context context) {
        mSharedPreferences = context.getSharedPreferences("DefaultSmsSenderView", Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
    }

    @Override
    public long getTimestamp(@Nullable String event, @Nullable String mobile) {
        String key = getTimestampKey(event, mobile);
        return mSharedPreferences.getLong(key, 0);
    }

    @Override
    public void saveTimestamp(@Nullable String event, @Nullable String mobile, long timestamp) {
        String key = getTimestampKey(event, mobile);
        mEditor.putLong(key, timestamp);
        mEditor.commit();
    }

    @Override
    public void clearTimestamp(@Nullable String event, @Nullable String mobile) {
        String key = getTimestampKey(event, mobile);
        mEditor.remove(key);
        mEditor.commit();
    }

    private String getTimestampKey(@Nullable String event, @Nullable String mobile) {
        return String.format("%s_%s%s", event == null ? "NULL" : event, mobile == null ? "NULL" : mobile, SMS_CODE_TIMESTAMP);
    }

}
