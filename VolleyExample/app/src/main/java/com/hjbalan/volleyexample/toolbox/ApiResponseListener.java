package com.hjbalan.volleyexample.toolbox;

public class ApiResponseListener {

    public interface ResultListener<T> {

        public abstract void onResult(T result);
    }

    public interface ApiErrorListener {

        public abstract void onError(String tag, ApiError error);
    }

}
