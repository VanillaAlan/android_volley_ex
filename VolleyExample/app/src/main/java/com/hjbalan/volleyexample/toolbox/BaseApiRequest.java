package com.hjbalan.volleyexample.toolbox;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.hjbalan.volleyexample.Config;
import com.hjbalan.volleyexample.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import java.util.Map;
import java.util.TreeMap;

public abstract class BaseApiRequest<Params, T> implements Listener<String>,
        ErrorListener {

    private static final String TAG = BaseApiRequest.class.getSimpleName();

    //TODO:use different time when network changed such as 3G or wifi
    private static final int TIME_OUT = 60 * 1000;

    protected ApiResponseListener.ResultListener<T> mListener;

    protected ApiResponseListener.ApiErrorListener mErrorListener;

    protected Context mContext;

    protected Params mParams;

    protected StringParamsRequest mRequest;

    private String mTag;

    public BaseApiRequest(Context ctx) {
        this(ctx, null, null);
    }

    public BaseApiRequest(Context ctx, ApiResponseListener.ResultListener<T> listener,
            ApiResponseListener.ApiErrorListener errorListener) {
        this.mContext = ctx;
        this.mListener = listener;
        this.mErrorListener = errorListener;

        String addr = buildAddress();

        mRequest = new StringParamsRequest(Method.POST, addr, this, this);
        mRequest.setRetryPolicy(new DefaultRetryPolicy(TIME_OUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    private String buildAddress() {
        return Config.HOST + bindApiPath();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        handleErrorResponse(HandleErrorHelper.handleVolleyError(mContext, error));
    }

    @Override
    public void onResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            int code = jsonResponse.optInt("ret", -1);
            if (code == 0) {
                handleCorrectResponse(jsonResponse);
            } else {
                String errorMessage = jsonResponse.optString("error");
                handleErrorResponse(HandleErrorHelper.handleApiError(code, errorMessage));
            }
        } catch (JSONException e) {
            handleErrorResponse(
                    HandleErrorHelper.handleApiError(HandleErrorHelper.ERROR_JSON_EXCEPTION,
                            mContext.getString(R.string.error_json_exception)));
            e.printStackTrace();
        }
    }

    /**
     * 拼接API接口地址
     */
    protected abstract String bindApiPath();

    /**
     * post params
     */
    protected abstract void postParams(Map<String, String> params);

    public BaseApiRequest<Params, T> setRequestTag(String tag) {
        mTag = tag;
        mRequest.setTag(tag);
        return this;
    }

    /**
     * add current request to queue
     */
    public void execute() {
        mRequest.setParams(getParams());
//        TODO: add request queue
    }

    /**
     * build params for request
     */
    public BaseApiRequest<Params, T> buildParams(Params params) {
        this.mParams = params;
        return this;
    }

    /**
     * handle jsonResponse when return code is 0
     */
    protected void handleCorrectResponse(JSONObject response) {
        T result = parseJsonToResult(response);
        if (mListener != null) {
            mListener.onResult(result);
        }
    }

    /**
     * handle error when onErrorResponse is called or return code is not 0
     */
    protected void handleErrorResponse(ApiError error) {
        if (mErrorListener != null) {
            mErrorListener.onError(mTag, error);
        }
    }

    /**
     * parse JSONObject response to result
     */
    protected abstract T parseJsonToResult(JSONObject jsonResponse);

    private Map<String, String> getParams() {
        Map<String, String> params = new TreeMap<String, String>();
        postParams(params);
        return params;
    }
}
