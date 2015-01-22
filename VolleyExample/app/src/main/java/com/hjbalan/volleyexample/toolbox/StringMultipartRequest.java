package com.hjbalan.volleyexample.toolbox;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.MultiPartRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class StringMultipartRequest extends MultiPartRequest<String> {

    private Map<String, String> mHeaders;
    
    public StringMultipartRequest(String url, Response.Listener<String> listener,
            Response.ErrorListener errorListener) {
        super(Method.POST, url, listener, errorListener);
    }
    
    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders == null ? super.getHeaders() : mHeaders;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}