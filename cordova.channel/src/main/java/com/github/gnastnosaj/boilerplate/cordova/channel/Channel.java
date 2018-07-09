package com.github.gnastnosaj.boilerplate.cordova.channel;

import android.content.Context;

import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelEvent;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelEventBus;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddleware;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddlewareCallback;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper;
import com.github.gnastnosaj.bolierplate.cordova.plugin.R;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by jasontsang on 1/23/18.
 */

public class Channel extends CordovaPlugin {
    public final static List<ChannelMiddleware> middlewares = new ArrayList<>();

    public final static Map<CallbackContext, Disposable> subscriptions = new ConcurrentHashMap<>();

    public final static Map<CallbackContext, Observable> observables = new ConcurrentHashMap<>();

    public final static Map<String, CallbackContext> callbacks = new ConcurrentHashMap<>();

    private Context context;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        context = cordova.getContext();

        for (ChannelMiddleware middleware : middlewares) {
            middleware.initialize(context, new ChannelEventBus() {
                @Override
                public void send(ChannelEvent event) {
                    RxBus.getInstance().send(event);
                }

                @Override
                public void post(String tag, ChannelEvent event) {
                    RxBus.getInstance().post(tag, event);
                }
            });
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "exec":
                String scheme = args.getString(0);
                JSONArray data = args.getJSONArray(1);
                exec(scheme, data, callbackContext);
                break;
            case "subscribe":
                String subscribeTask = args.getString(0);
                subscribe(subscribeTask, callbackContext);
                break;
            case "dispose":
                String disposeTask = args.getString(0);
                dispose(disposeTask);
                break;
            case "register":
                String registerTag = args.getString(0);
                String registerTask = args.getString(1);
                register(registerTask, registerTag, callbackContext);
                break;
            case "unregister":
                String unregisterTag = args.getString(0);
                String unregisterTask = args.getString(1);
                unregister(unregisterTask, unregisterTag);
                break;
        }
        return true;
    }

    public void exec(String scheme, JSONArray data, CallbackContext callbackContext) {
        List<Observable<JSONObject>> observables = new ArrayList<>();

        for (ChannelMiddleware middleware : middlewares) {
            if (middleware.accept(scheme)) {
                observables.add(Observable.create(emitter -> {
                            try {
                                middleware.exec(scheme, data, new ChannelMiddlewareCallback() {
                                    @Override
                                    public void perform(JSONObject data) {
                                        if (data == null) {
                                            data = new JSONObject();
                                        }
                                        try {
                                            data.put("__channel__keep__", true);
                                        } catch (JSONException e) {
                                            emitter.onError(e);
                                        }
                                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
                                        pluginResult.setKeepCallback(true);
                                        callbackContext.sendPluginResult(pluginResult);
                                        emitter.onNext(data);
                                    }

                                    @Override
                                    public void error(Throwable throwable) {
                                        emitter.onError(throwable);
                                    }

                                    @Override
                                    public void end() {
                                        emitter.onComplete();
                                    }
                                });
                            } catch (Throwable throwable) {
                                emitter.onError(throwable);
                            }
                        }
                ));
            }
        }

        if (observables.isEmpty()) {
            callbackContext.error(context.getResources().getString(R.string.unsupported_cordova_channel_scheme));
        } else {
            Observable.zip(observables, outcomes -> outcomes.length)
                    .compose(RxHelper.rxSchedulerHelper())
                    .subscribe(
                            count -> {
                            },
                            throwable -> callbackContext.error(throwable.getMessage()),
                            () -> {
                                JSONObject result = new JSONObject();
                                result.put("__channel__keep__", false);
                                callbackContext.success(result);
                            }
                    );
        }
    }

    public void subscribe(String task, CallbackContext callbackContext) {
        callbacks.put(task, callbackContext);

        Disposable disposable = RxBus.getInstance().toObserverable().subscribe(event -> {
            if (event instanceof ChannelEvent) {
                try {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, ((ChannelEvent) event).toJSON());
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                } catch (Exception e) {
                    dispose(task);
                }
            }
        }, throwable -> callbackContext.error(throwable.getMessage()));
        subscriptions.put(callbackContext, disposable);
    }

    public void dispose(String task) {
        CallbackContext callbackContext = callbacks.get(task);
        if (callbackContext != null) {
            Disposable disposable = subscriptions.get(callbackContext);
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            subscriptions.remove(callbackContext);
        }
    }

    public void register(String task, String tag, CallbackContext callbackContext) {
        callbacks.put(task, callbackContext);

        Observable<ChannelEvent> observable = RxBus.getInstance().register(tag, ChannelEvent.class);
        observables.put(callbackContext, observable);

        Disposable disposable = observable.subscribe(event -> {
            try {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, event.toJSON());
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } catch (Exception e) {
                e.printStackTrace();
                unregister(task, tag);
            }
        }, throwable -> callbackContext.error(throwable.getMessage()));
        subscriptions.put(callbackContext, disposable);
    }

    public void unregister(String task, String tag) {
        dispose(task);

        CallbackContext callbackContext = callbacks.get(task);

        if (callbackContext != null) {
            Observable observable = observables.get(callbackContext);
            RxBus.getInstance().unregister(tag, observable);
            observables.remove(callbackContext);
        }
    }
}
