package com.github.gnastnosaj.boilerplate.cordova.channel.middleware;

import android.content.Context;

import org.json.JSONArray;

/**
 * Created by jasontsang on 1/17/18.
 */

public interface ChannelMiddleware {
    void initialize(Context context, ChannelEventBus eventBus);

    boolean accept(String scheme);

    void exec(String scheme, JSONArray data, ChannelMiddlewareCallback callback) throws Throwable;
}
