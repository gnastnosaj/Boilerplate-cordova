package com.github.gnastnosaj.boilerplate.cordova.channel.middleware.sample;

import android.content.Context;

import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelEventBus;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddleware;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddlewareCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jasontsang on 1/23/18.
 */

public class SampleMiddleware implements ChannelMiddleware {
    private ChannelEventBus eventBus;

    @Override
    public void initialize(Context context, ChannelEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public boolean accept(String scheme) {
        return scheme.equals("com.github.gnastnosaj.boilerplate.cordova.channel.middleware.sample");
    }

    @Override
    public void exec(String scheme, JSONArray data, ChannelMiddlewareCallback callback) {
        JSONObject a = new JSONObject();
        try {
            a.put("result", scheme);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callback.perform(a);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            JSONObject b = new JSONObject();
            try {
                b.put("result", data.getString(0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callback.perform(b);
            callback.end();
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            eventBus.post("sample", () -> {
                JSONObject c = new JSONObject();
                try {
                    c.put("event", "sample channel event");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return c;
            });

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            eventBus.post("sample", () -> {
                JSONObject c = new JSONObject();
                try {
                    c.put("event", "sample channel event 2");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return c;
            });

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            eventBus.post("sample", () -> {
                JSONObject c = new JSONObject();
                try {
                    c.put("event", "sample channel event 3");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return c;
            });
        }).start();
    }
}
