package com.hjbalan.volleyexample.toolbox;


public class ApiError {

    public int errorCode;

    public String errorMsg;

    public ApiError(int errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
