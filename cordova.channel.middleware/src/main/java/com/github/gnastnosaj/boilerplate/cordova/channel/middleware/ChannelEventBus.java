package com.github.gnastnosaj.boilerplate.cordova.channel.middleware;

/**
 * Created by jasontsang on 1/17/18.
 */

public interface ChannelEventBus {
    void send(ChannelEvent event);

    void post(String tag, ChannelEvent event);
}
