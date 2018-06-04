package com.github.gnastnosaj.boilerplate.cordova.crosswalk.ext;

import org.crosswalk.engine.XWalkCordovaView;

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
}
