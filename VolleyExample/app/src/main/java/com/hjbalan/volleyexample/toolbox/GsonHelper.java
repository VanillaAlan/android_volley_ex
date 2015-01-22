package com.hjbalan.volleyexample.toolbox;

import com.google.gson.Gson;

/**
 * Created by alan on 15/1/22.
 */
public class GsonHelper {

    private static Gson sGson;
    
    private GsonHelper() {
    }
    
    public static synchronized Gson getGsonInstance() {
        if (sGson == null) {
            sGson = new Gson();
        }
        return sGson;
    }
    
}
