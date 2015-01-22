package com.hjbalan.volleyexample.toolbox;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.hjbalan.volleyexample.Config;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * used to request api with multi part,the method is post
 *
 * @author alan
 */
public abstract class BaseMultiApiRequest<Params, Result> implements
        Listener<String>, ErrorListener {

    public static final String TAG = "BaseMultiApiRequest";

    //TODO:use different time when network changed such as 3G or wifi
    public static final int TIME_OUT = 120 * 1000;

    protected ApiResponseListener.ResultListener<Result> mListener;

    protected ApiResponseListener.ApiErrorListener mErrorListener;

    protected Context ctx;

    protected StringMultipartRequest mRequest;

    protected Params mParams;

    protected String mTag;

    public BaseMultiApiRequest(Context ctx) {
        this(ctx, null, null);
    }

    public BaseMultiApiRequest(Context ctx, ApiResponseListener.ResultListener<Result> listener,
            ApiResponseListener.ApiErrorListener errorListener) {
        this.ctx = ctx;
        String address = buildAddress();
        mRequest = new StringMultipartRequest(address, this, this);
        mRequest.setRetryPolicy(new DefaultRetryPolicy(TIME_OUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mListener = listener;
        mErrorListener = errorListener;

    }

//    TODO:support switch host
    private String buildAddress() {
        return Config.HOST + bindApiPath();
    }

    /**
     * 拼接API接口地址
     */
    protected abstract String bindApiPath();

    @Override
    public void onErrorResponse(VolleyError error) {
        handleErrorResponse(HandleErrorHelper.handleVolleyError(ctx, error));
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
                handleErrorResponse(HandleErrorHelper.handleApiError(code,
                        errorMessage));
            }
        } catch (JSONException e) {
            handleErrorResponse(HandleErrorHelper.handleApiError(2005, ""));
            e.printStackTrace();
        }
    }

    /**
     * handle jsonResponse when return code is 0
     */
    protected void handleCorrectResponse(JSONObject jsonResponse) {
        Result result = parseJsonToResult(jsonResponse);
        if (mListener != null) {
            mListener.onResult(result);
        }
    }

    /**
     * parse JSONObject response to result
     */
    protected abstract Result parseJsonToResult(JSONObject jsonResponse);

    /**
     * handle error when onErrorResponse is called or return code is not 0
     */
    protected void handleErrorResponse(ApiError error) {
        if (mErrorListener != null) {
            mErrorListener.onError(mTag, error);
        }
    }

    /**
     * post params
     */
    protected abstract void postParams(Map<String, String> params);

    /**
     * set tag for request
     */
    public BaseMultiApiRequest<Params, Result> setRequestTag(String tag) {
        mTag = tag;
        mRequest.setTag(tag);
        return this;
    }

    /**
     * return the request tag
     */
    public String getRequestTag() {
        Object tag = mRequest.getTag();
        return tag == null ? null : String.valueOf(tag);
    }

    /**
     * add current request to queue
     */
    public void execute() {
        addMultiPartParams();
        //TODO:add to queue
    }

    /**
     * build params for request
     */
    public BaseMultiApiRequest<Params, Result> buildParams(Params params) {
        this.mParams = params;
        return this;
    }

    private void addMultiPartParams() {
        Map<String, String> params = getParams();
        for (String key : params.keySet()) {
            mRequest.addMultipartParam(key, "text/plain", params.get(key));
        }
    }

    //TODO:get access token
    private Map<String, String> getParams() {
        Map<String, String> params = new TreeMap<String, String>();

        postParams(params);
        return params;
    }

    /**
     * add file to upload
     */
    public BaseMultiApiRequest<Params, Result> buildFileParams(String fileName,
            String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            mRequest.addFile(fileName, filePath);
        }
        return this;
    }

}
