package com.github.gnastnosaj.boilerplate.cordova.channel;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelEvent;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelEventBus;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddleware;
import com.github.gnastnosaj.boilerplate.cordova.channel.middleware.ChannelMiddlewareCallback;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
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
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jasontsang on 1/23/18.
 */

public class Channel extends CordovaPlugin {
    public final static String TAG = Channel.class.getName();

    public final static List<ChannelMiddleware> middlewares = new ArrayList<>();

    public final static Map<CallbackContext, Disposable> disposables = new ConcurrentHashMap<>();

    public final static Map<CallbackContext, Observable> observables = new ConcurrentHashMap<>();

    public final static Map<String, CallbackContext> callbacks = new ConcurrentHashMap<>();

    private CordovaInterface cordova;
    private Context context;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        this.cordova = cordova;
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
        cordova.getThreadPool().execute(() -> {
            try {
                switch (action) {
                    case "exec":
                        String scheme = args.getString(0);
                        JSONArray data = args.getJSONArray(1);
                        String execTask = args.optString(2);
                        execTask = TextUtils.isEmpty(execTask) ? "exec_" + System.currentTimeMillis() : execTask;
                        Log.d(TAG, "exec task: " + execTask);
                        exec(scheme, data, execTask, callbackContext);
                        break;
                    case "cancel":
                        String cancelTask = args.optString(0);
                        if (cancelTask != null) {
                            Log.d(TAG, "cancel task: " + cancelTask);
                            dispose(cancelTask);
                        } else {
                            Object[] keys = callbacks.keySet().toArray();
                            for (int i = 0; i < keys.length; i++) {
                                String key = (String) keys[i];
                                if (key.startsWith("exec_")) {
                                    Log.d(TAG, "cancel task: " + key);
                                    dispose(key);
                                }
                            }
                        }
                        break;
                    case "subscribe":
                        String subscribeTask = args.getString(0);
                        Log.d(TAG, "subscribe task: " + subscribeTask);
                        subscribe(subscribeTask, callbackContext);
                        break;
                    case "dispose":
                        String disposeTask = args.getString(0);
                        Log.d(TAG, "dispose task: " + disposeTask);
                        dispose(disposeTask);
                        break;
                    case "register":
                        String registerTag = args.getString(0);
                        String registerTask = args.getString(1);
                        Log.d(TAG, "register task: " + registerTask);
                        register(registerTask, registerTag, callbackContext);
                        break;
                    case "unregister":
                        String unregisterTag = args.getString(0);
                        String unregisterTask = args.getString(1);
                        Log.d(TAG, "unregister task: " + unregisterTask);
                        unregister(unregisterTask, unregisterTag);
                        break;
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        });
        return true;
    }

    public void exec(String scheme, JSONArray data, String task, CallbackContext callbackContext) {
        callbacks.put(task, callbackContext);

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
                                        if (throwable == null) {
                                            emitter.onError(new Throwable("Unknown Exception"));
                                        } else {
                                            emitter.onError(throwable);
                                        }
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
            dispose(task);
        } else {
            Disposable disposable = Observable.zip(observables, outcomes -> outcomes.length)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            count -> {
                            },
                            throwable -> {
                                callbackContext.error(throwable.getMessage());
                                dispose(task);
                            },
                            () -> {
                                JSONObject result = new JSONObject();
                                result.put("__channel__keep__", false);
                                callbackContext.success(result);
                                dispose(task);
                            }
                    );
            disposables.put(callbackContext, disposable);
        }
    }

    public void subscribe(String task, CallbackContext callbackContext) {
        callbacks.put(task, callbackContext);

        Disposable disposable = RxBus.getInstance().toObserverable()
                .subscribeOn(Schedulers.io())
                .subscribe(event -> {
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
        disposables.put(callbackContext, disposable);
    }

    public CallbackContext dispose(String task) {
        CallbackContext callbackContext = callbacks.remove(task);
        if (callbackContext != null) {
            Log.d(TAG, "remove callback of task: " + task);
            Disposable disposable = disposables.remove(callbackContext);
            if (disposable != null) {
                Log.d(TAG, "remove disposable of task: " + task);
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }
        }
        return callbackContext;
    }

    public void register(String task, String tag, CallbackContext callbackContext) {
        callbacks.put(task, callbackContext);

        Observable<ChannelEvent> observable = RxBus.getInstance().register(tag, ChannelEvent.class);
        observables.put(callbackContext, observable);

        Disposable disposable = observable
                .subscribeOn(Schedulers.io()).subscribe(event -> {
                    try {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, event.toJSON());
                        pluginResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(pluginResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                        unregister(task, tag);
                    }
                }, throwable -> callbackContext.error(throwable.getMessage()));
        disposables.put(callbackContext, disposable);
    }

    public void unregister(String task, String tag) {
        CallbackContext callbackContext = dispose(task);

        if (callbackContext != null) {
            Observable observable = observables.get(callbackContext);
            RxBus.getInstance().unregister(tag, observable);
            observables.remove(callbackContext);
        }
    }
}
