package com.letscooee.utils;

import android.content.Context;

import androidx.annotation.RestrictTo;

import com.letscooee.BuildConfig;
import com.letscooee.CooeeFactory;
import com.letscooee.device.AppInfo;
import com.letscooee.device.DeviceInfo;
import com.letscooee.init.DefaultUserPropertiesCollector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collect all the debug information.
 *
 * @author Ashish Gaikwad 01/09/21
 * @since 1.0.0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DebugInfoCollector {

    private final AppInfo appInfo;
    private final Context context;
    private final DeviceInfo deviceInfo;
    private final Map<String, Map<String, Object>> debugInfo;
    private final DefaultUserPropertiesCollector otherInfo;

    public DebugInfoCollector(Context context) {
        this.context = context;
        debugInfo = new TreeMap<>();
        appInfo = CooeeFactory.getAppInfo();
        deviceInfo = CooeeFactory.getDeviceInfo();
        otherInfo = new DefaultUserPropertiesCollector(context);
        init();
    }

    /**
     * Collects device info and user info
     */
    private void init() {
        collectDeviceInfo();
        collectUserInfo();
    }

    /**
     * Collect all User information and add it to {@link #debugInfo}
     */
    private void collectUserInfo() {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("User ID", LocalStorageHelper.getString(context,
                Constants.STORAGE_USER_ID, ""));
        debugInfo.put("User Info", userInfo);
    }

    /**
     * Collect all Device information and add it to {@link #debugInfo}
     */
    private void collectDeviceInfo() {
        Map<String, Object> deviceInformation = new LinkedHashMap<>();
        deviceInformation.put("Device Name", deviceInfo.getDeviceName());
        deviceInformation.put("SDK Version", BuildConfig.VERSION_NAME + "+" + BuildConfig.VERSION_CODE);
        deviceInformation.put("App Version", appInfo.getVersion());
        deviceInformation.put("Bundle ID", appInfo.getPackageName());
        deviceInformation.put("Install Date", appInfo.getFirstInstallTime());
        deviceInformation.put("Build Date", appInfo.getLasBuildTime());
        String firebaseToken = LocalStorageHelper.getString(context,
                Constants.STORAGE_FB_TOKEN, "");
        deviceInformation.put("FB Token", firebaseToken);
        deviceInformation.put("Device ID", LocalStorageHelper.getString(context,
                Constants.STORAGE_DEVICE_ID, ""));
        deviceInformation.put("Resolution", otherInfo.getScreenResolution());
        debugInfo.put("Device & App Info", deviceInformation);
    }

    public Map<String, Map<String, Object>> getDebugInfo() {
        return debugInfo;
    }
}
