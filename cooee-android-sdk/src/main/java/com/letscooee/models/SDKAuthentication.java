package com.letscooee.models;

/**
 * @author Abhishek Taparia
 * The SDKAuthentication class will help in storing sdkToken received from the server when app is opened first time.
 */
public class SDKAuthentication {

    private String sdkToken;

    public String getSdkToken() {
        return sdkToken;
    }

    public void setSdkToken(String sdkToken) {
        this.sdkToken = sdkToken;
    }
}
