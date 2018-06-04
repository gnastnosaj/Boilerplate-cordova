package com.github.gnastnosaj.boilerplate.cordova.sample;

import android.app.Application;

import com.github.gnastnosaj.boilerplate.cordova.channel.Channel;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.sample.SampleMiddleware;

/**
 * Created by jasontsang on 1/23/18.
 */

public class Sample extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Channel.middlewares.add(new SampleMiddleware());
    }
}
