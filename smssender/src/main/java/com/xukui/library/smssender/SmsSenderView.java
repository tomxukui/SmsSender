package com.xukui.library.smssender;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SmsSenderView extends FrameLayout {

    public static final int IDLE = 0;
    public static final int WAIT = 1;
    public static final int COUNTING = 2;

    @IntDef({IDLE, WAIT, COUNTING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
    }

    private TextView contentTextView;
    private ProgressBar timerProgressBar;

    private String mText;
    private String mHint;
    private float mTextSize;
    private int mTextColor;
    private int mTextColorHint;
    private int mGravity;
    private long mDuration;
    private String mRetryText;
    private boolean mAuto;
    @Nullable
    private String mMobile;
    @Nullable
    private String mEvent;

    @Status
    private int mStatus;
    private boolean mFirst;
    private SavedStore mSavedStore;
    @Nullable
    private OnSendListener mOnSendListener;

    private CountDownTimer mTimer;

    public SmsSenderView(@NonNull Context context) {
        super(context);
        initData(context, null, 0);
        initView(context);
        setView();
    }

    public SmsSenderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initData(context, attrs, 0);
        initView(context);
        setView();
    }

    public SmsSenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context, attrs, defStyleAttr);
        initView(context);
        setView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopCountDown();
    }

    private void initData(Context context, AttributeSet attrs, int defStyleAttr) {
        mStatus = IDLE;
        mFirst = true;
        mTextSize = sp2px(context, 16);
        mTextColor = Color.parseColor("#333333");
        mTextColorHint = Color.parseColor("#999999");
        mGravity = Gravity.CENTER;
        mDuration = 60L;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SmsSenderView, defStyleAttr, 0);

            mText = ta.getString(R.styleable.SmsSenderView_android_text);
            mHint = ta.getString(R.styleable.SmsSenderView_android_hint);
            mTextSize = ta.getDimension(R.styleable.SmsSenderView_android_textSize, mTextSize);
            mTextColor = ta.getColor(R.styleable.SmsSenderView_android_textColor, mTextColor);
            mTextColorHint = ta.getColor(R.styleable.SmsSenderView_android_textColorHint, mTextColorHint);
            mGravity = ta.getInt(R.styleable.SmsSenderView_android_gravity, mGravity);

            String durationStr = ta.getString(R.styleable.SmsSenderView_ssv_duration);
            long duration = (TextUtils.isEmpty(durationStr) ? mDuration : Long.parseLong(durationStr));
            mDuration = (duration <= 0 ? mDuration : duration);

            mRetryText = ta.getString(R.styleable.SmsSenderView_ssv_retryText);
            mAuto = ta.getBoolean(R.styleable.SmsSenderView_ssv_auto, false);
            mMobile = ta.getString(R.styleable.SmsSenderView_ssv_mobile);
            mEvent = ta.getString(R.styleable.SmsSenderView_ssv_event);

            ta.recycle();
        }

        if (mText == null) {
            mText = "获取验证码";
        }
        if (mHint == null) {
            mHint = "重新获取(%ds)";
        }
        if (mRetryText == null) {
            mRetryText = "获取验证码";
        }
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_sms_sender, this);

        contentTextView = findViewById(R.id.content_textView);
        timerProgressBar = findViewById(R.id.timer_progressBar);
    }

    private void setView() {
        setGravity(mGravity);
        setTextSize(mTextSize);
        contentTextView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mStatus == IDLE) {
                    attemptSend();
                }
            }

        });
        setStatusView(IDLE);
    }

    private void setStatusView(@Status int status) {
        this.mStatus = status;

        switch (mStatus) {

            case WAIT: {
                contentTextView.setVisibility(View.INVISIBLE);
                timerProgressBar.setVisibility(View.VISIBLE);
            }
            break;

            case COUNTING: {
                contentTextView.setVisibility(View.VISIBLE);
                contentTextView.setTextColor(mTextColorHint);

                timerProgressBar.setVisibility(View.INVISIBLE);
            }
            break;

            case IDLE:
            default: {
                contentTextView.setVisibility(View.VISIBLE);
                contentTextView.setTextColor(mTextColor);
                contentTextView.setText(mFirst ? mText : mRetryText);

                timerProgressBar.setVisibility(View.INVISIBLE);
            }
            break;

        }
    }

    public void setGravity(int gravity) {
        mGravity = gravity;

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            FrameLayout.LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            layoutParams.gravity = mGravity;
        }

        requestLayout();
    }

    public void setTextSize(float textSize) {
        this.mTextSize = textSize;

        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    public void setAuto(boolean auto) {
        this.mAuto = auto;
    }

    public void setMobile(@Nullable String mobile) {
        this.mMobile = mobile;
    }

    public void setEvent(@Nullable String event) {
        this.mEvent = event;
    }

    /**
     * 企图发送验证码并且倒计时
     */
    private void attemptSend() {
        setStatusView(WAIT);

        if (mOnSendListener == null) {
            setSendResult(true);

        } else {
            mOnSendListener.onPrepared();
        }
    }

    /**
     * 开始执行
     */
    public void start() {
        if (mStatus != IDLE) {
            return;
        }

        if (!TextUtils.isEmpty(mEvent)) {
            long lastTime = getSavedStore().getTimestamp(mEvent, mMobile);
            long nowTime = System.currentTimeMillis();

            if (nowTime >= lastTime) {
                long len = mDuration - (nowTime - lastTime);

                if (len > 0) {
                    startCountDown(len);
                    return;
                }
            }
        }

        if (mAuto) {
            attemptSend();
        }
    }

    /**
     * sp转px
     */
    protected int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 开始倒计时
     */
    private void startCountDown(long duration) {
        stopCountDown();

        mTimer = new CountDownTimer((duration / 1000) * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                setStatusView(COUNTING);
                contentTextView.setText(String.format(mHint, millisUntilFinished / 1000L));
            }

            @Override
            public void onFinish() {
                mFirst = false;
                setStatusView(IDLE);
            }

        };
        mTimer.start();
    }

    /**
     * 结束倒计时
     */
    private void stopCountDown() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /**
     * 保存开始倒计时的记录
     */
    private void saveRecord() {
        if (!TextUtils.isEmpty(mEvent)) {
            getSavedStore().saveTimestamp(mEvent, mMobile, System.currentTimeMillis());
        }
    }

    /**
     * 设置发送结果
     */
    public void setSendResult(boolean success) {
        if (success) {
            saveRecord();
            startCountDown(mDuration);

        } else {
            stopCountDown();
            setStatusView(IDLE);
        }
    }

    /**
     * 清除事件记录
     */
    public void clearRecord() {
        if (!TextUtils.isEmpty(mEvent)) {
            getSavedStore().clearTimestamp(mEvent, mMobile);
        }
    }

    public void setOnSendListener(OnSendListener listener) {
        this.mOnSendListener = listener;
    }

    public void setSavedStore(SavedStore savedStore) {
        this.mSavedStore = savedStore;
    }

    public SavedStore getSavedStore() {
        if (this.mSavedStore == null) {
            this.mSavedStore = new DefaultSavedStore(getContext());
        }

        return this.mSavedStore;
    }

}