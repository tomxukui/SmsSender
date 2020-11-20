package com.xukui.demo.smssender;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.xukui.library.smssender.OnSendListener;
import com.xukui.library.smssender.SmsSenderView;

public class MainActivity extends AppCompatActivity {

    private SmsSenderView senderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setView();
    }

    private void initView() {
        senderView = findViewById(R.id.senderView);
    }

    private void setView() {
        senderView.setOnSendListener(new OnSendListener() {

            @Override
            public void onPrepared() {
                senderView.setSendResult(true);
            }

        });
        senderView.start();
    }

}