package com.github.gnastnosaj.boilerplate.cordova.channel.middleware;

import org.json.JSONObject;

/**
 * Created by jasontsang on 1/17/18.
 */

public interface ChannelEvent {
    JSONObject toJSON();
}