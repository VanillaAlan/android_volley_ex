package com.hjbalan.volleyexample.toolbox;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;

/**
 * Created by alan on 15/1/22.
 */
public class StringParamsRequest extends StringRequest {
    
    private Map<String, String> mParams;
    
    private Map<String, String> mHeaders;

    public StringParamsRequest(int method, String url, Response.Listener<String> listener,
            Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    public void setHeaders(Map<String, String> header) {
        mHeaders = header;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders == null ? super.getHeaders() : mHeaders;
    }
    
    public void setParams(Map<String, String> params) {
        mParams = params;
    }

    @Override
    public Map<String, String> getParams() {
        return mParams == null ? null : mParams;
    }
}
