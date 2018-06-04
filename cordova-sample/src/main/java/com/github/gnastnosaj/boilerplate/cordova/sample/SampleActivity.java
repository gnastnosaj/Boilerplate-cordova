package com.github.gnastnosaj.boilerplate.cordova.sample;

import android.os.Bundle;

import com.github.gnastnosaj.boilerplate.cordova.crosswalk.ext.HackyXWalkWorkaround;
import com.github.gnastnosaj.boilerplate.cordova.crosswalk.ext.HackyCordovaActivity;

import org.crosswalk.engine.XWalkCordovaView;
import org.xwalk.core.XWalkSettings;

/**
 * Created by jasontsang on 1/19/18.
 */

public class SampleActivity extends HackyCordovaActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HackyXWalkWorkaround.WORKAROUND.add(new HackyXWalkWorkaround() {
            @Override
            public void ready(XWalkCordovaView webView) {
                webView.setOnLongClickListener(v -> true);
            }

            @Override
            public void when(XWalkCordovaView webView, String url) {
                webView.setInitialScale(100);

                XWalkSettings webSettings = webView.getSettings();
                webSettings.setBuiltInZoomControls(false);
                webSettings.setSupportZoom(false);
            }
        });

        super.init();

        loadUrl(launchUrl);
    }

    @Override
    public void onReceivedError(int errorCode, String description, String failingUrl) {
        //super.onReceivedError(errorCode, description, failingUrl);
    }

    @Override
    protected void initViews() {
        createDefaultViews();
    }
}
