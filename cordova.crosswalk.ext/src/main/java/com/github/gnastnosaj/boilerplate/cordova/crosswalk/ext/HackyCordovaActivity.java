package com.github.gnastnosaj.boilerplate.cordova.crosswalk.ext;

import org.crosswalk.engine.XWalkCordovaView;
import org.crosswalk.engine.XWalkWebViewEngine;

/**
 * Created by jasontsang on 1/22/18.
 */

public abstract class HackyCordovaActivity extends com.github.gnastnosaj.boilerplate.cordova.ext.HackyCordovaActivity {
    @Override
    public Object onMessage(String id, Object data) {
        if (id.equals("onXWalkReady")) {
            for (HackyXWalkWorkaround workaround : HackyXWalkWorkaround.WORKAROUND) {
                workaround.ready((XWalkCordovaView) appView.getView());
            }
        }

        return super.onMessage(id, data);
    }

    @Override
    public void loadUrl(String url) {
        whenLoadUrl(url);
        super.loadUrl(url);
    }

    private void whenLoadUrl(String url) {
        if (((XWalkWebViewEngine) appView.getEngine()).isXWalkReady()) {
            findViewById(android.R.id.content).post(() -> {
                for (HackyXWalkWorkaround workaround : HackyXWalkWorkaround.WORKAROUND) {
                    workaround.when((XWalkCordovaView) appView.getView(), url);
                }
            });
        } else {
            findViewById(android.R.id.content).postDelayed(() -> whenLoadUrl(url), 500);
        }
    }
}
