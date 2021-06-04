package com.letscooee.init;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.letscooee.CooeeFactory;
import com.letscooee.models.Event;
import com.letscooee.retrofit.HttpCallsHelper;
import com.letscooee.user.NewSessionExecutor;
import com.letscooee.user.SessionManager;
import com.letscooee.utils.Constants;
import com.letscooee.utils.RuntimeData;

import java.util.HashMap;
import java.util.Map;

class AppLifecycleCallback implements DefaultLifecycleObserver {

    private final Context context;
    private final RuntimeData runtimeData;
    private final SessionManager sessionManager;

    private Handler handler = new Handler();
    private Runnable runnable;

    AppLifecycleCallback(Context context) {
        this.context = context;
        this.runtimeData = CooeeFactory.getRuntimeData();
        this.sessionManager = CooeeFactory.getSessionManager();
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        // TODO: 03/06/21 When this will be called?
        runtimeData.setInForeground();

        keepSessionAlive();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        // TODO: 03/06/21 When this will be called?

        if (runtimeData.isFirstForeground()) {
            return;
        }

        long backgroundDuration = runtimeData.getTimeInBackgroundInSeconds();

        if (backgroundDuration > Constants.IDLE_TIME_IN_SECONDS) {
            sessionManager.conclude();

            new NewSessionExecutor(context).execute();
            Log.d(Constants.LOG_PREFIX, "After 30 min of App Background " + "Session Concluded");
        } else {
            Map<String, Object> eventProps = new HashMap<>();
            eventProps.put("CE Duration", backgroundDuration / 1000);
            Event session = new Event("CE App Foreground", eventProps);

            HttpCallsHelper.sendEvent(context, session, null);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        // TODO: 03/06/21 When this will be called?
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // TODO: 03/06/21 When this will be called?
        runtimeData.setInBackground();

        //stop sending check message of session alive on app background
        handler.removeCallbacks(runnable);

        if (context == null) {
            return;
        }

        long duration = runtimeData.getTimeInForegroundInSeconds();

        Map<String, Object> sessionProperties = new HashMap<>();
        sessionProperties.put("CE Duration", duration);

        Event session = new Event("CE App Background", sessionProperties);
        HttpCallsHelper.sendEvent(context, session, null);
    }

    /**
     * Send server check message every 5 min that session is still alive
     */
    // TODO: 03/06/21 Move to SessionManager
    private void keepSessionAlive() {
        //send server check message every 5 min that session is still alive
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, Constants.KEEP_ALIVE_TIME_IN_MS);
            this.sessionManager.pingServerToKeepAlive();

        }, Constants.KEEP_ALIVE_TIME_IN_MS);
    }
}
