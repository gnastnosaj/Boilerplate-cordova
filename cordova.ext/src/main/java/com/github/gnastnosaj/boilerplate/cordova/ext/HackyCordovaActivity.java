package com.github.gnastnosaj.boilerplate.cordova.ext;

import org.apache.cordova.CordovaActivity;

/**
 * Created by jasontsang on 1/21/18.
 */

public abstract class HackyCordovaActivity extends CordovaActivity {

    @Override
    protected void createViews() {
        //super.createViews();

        initViews();
    }

    protected void createDefaultViews() {
        super.createViews();
    }

    protected abstract void initViews();
}
