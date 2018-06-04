package com.github.gnastnosaj.boilerplate.cordova.crosswalk.ext;

import org.crosswalk.engine.XWalkCordovaView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontsang on 1/22/18.
 */

public interface HackyXWalkWorkaround {
    List<HackyXWalkWorkaround> WORKAROUND = new ArrayList<>();

    void ready(XWalkCordovaView webView);

    void when(XWalkCordovaView webView, String url);
}