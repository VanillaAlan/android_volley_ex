package com.hjbalan.volleyexample.toolbox;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.hjbalan.volleyexample.R;

import android.content.Context;

/**
 * handle api error and volley error,return #{@link ApiError} contains errorCode
 * and errorMsg
 *
 * @author alan
 */
public class HandleErrorHelper {

    public static final int ERROR_CONNECTION_TIMEOUT = 2001;
    public static final int ERROR_SERVER_DOWN = 2002;
    public static final int ERROR_NO_NETWORK = 2003;
    public static final int ERROR_JSON_EXCEPTION = 2004;
    public static final int ERROR_UNKNOWN = 2005;
    
    
    public static ApiError handleApiError(int errorCode, String errorMessage) {
        return new ApiError(errorCode, errorMessage);
    }

    /**
     * Returns appropriate message which is to be displayed to the user against
     * the specified error object.
     */
    public static ApiError handleVolleyError(Context ctx, VolleyError error) {
        if (error instanceof TimeoutError) {
            return new ApiError(ERROR_CONNECTION_TIMEOUT,
                    ctx.getString(R.string.error_connection_timeout));
        } else if (isServerProblem(error)) {
            return new ApiError(ERROR_SERVER_DOWN, ctx.getString(R.string.error_server_down));
        } else if (isNetworkProblem(error)) {
            return new ApiError(ERROR_NO_NETWORK, ctx.getString(R.string.error_no_network));
        }
        return new ApiError(ERROR_UNKNOWN, ctx.getString(R.string.error_unknown));
    }

    /**
     * Determines whether the error is related to network
     */
    private static boolean isNetworkProblem(VolleyError error) {
        return (error instanceof NetworkError)
                || (error instanceof NoConnectionError);
    }

    /**
     * Determines whether the error is related to server
     */
    private static boolean isServerProblem(VolleyError error) {
        return (error instanceof ServerError)
                || (error instanceof AuthFailureError);
    }
}
