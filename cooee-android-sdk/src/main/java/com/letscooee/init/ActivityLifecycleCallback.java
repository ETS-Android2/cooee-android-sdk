package com.letscooee.init;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.firebase.messaging.FirebaseMessaging;
import com.letscooee.CooeeFactory;
import com.letscooee.brodcast.CooeeJobSchedulerBroadcast;
import com.letscooee.models.Event;
import com.letscooee.models.TriggerData;
import com.letscooee.retrofit.HttpCallsHelper;
import com.letscooee.schedular.CooeeJobScheduler;
import com.letscooee.trigger.CooeeEmptyActivity;
import com.letscooee.trigger.EngagementTriggerHelper;
import com.letscooee.trigger.inapp.InAppTriggerActivity;
import com.letscooee.user.NewSessionExecutor;
import com.letscooee.user.SessionManager;
import com.letscooee.utils.CooeeSDKConstants;
import com.letscooee.utils.LocalStorageHelper;
import com.letscooee.utils.RuntimeData;
import com.letscooee.utils.SentryHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Register operations on different lifecycle callbacks of all Activities.
 *
 * @author Ashish Gaikwad
 * @version 0.2.9
 */
public class ActivityLifecycleCallback {

    private Handler handler = new Handler();
    private Runnable runnable;

    private final Context context;
    private final Application application;
    private final SessionManager sessionManager;
    private final RuntimeData runtimeData;

    ActivityLifecycleCallback(Application application) {
        this.application = application;
        this.context = application.getApplicationContext();

        CooeeFactory.init(this.context);

        this.sessionManager = CooeeFactory.getSessionManager();
        this.runtimeData = CooeeFactory.getRuntimeData();
    }

    /**
     * Used to register activity lifecycle
     */
    public void register() {
        this.registerActivityLifecycleCallbacks();
        this.registerProcessLifecycle();
    }

    private void registerActivityLifecycleCallbacks() {
        SentryHelper.getInstance(context);
        checkAndStartJob(application.getApplicationContext());

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                InAppTriggerActivity.captureWindowForBlurryEffect(activity);

                FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> HttpCallsHelper.setFirebaseToken(token));
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                handleTriggerDataFromActivity(activity);

                if (activity instanceof CooeeEmptyActivity) {
                    activity.finish();
                }

                handleGlassmorphismAfterLaunch(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private void registerProcessLifecycle() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onEnterForeground() {
                runtimeData.setInForeground();

                keepSessionAlive();

                if (runtimeData.isFirstForeground()) {
                    return;
                }

                long backgroundDuration = runtimeData.getTimeInBackgroundInSeconds();

                if (backgroundDuration > CooeeSDKConstants.IDLE_TIME_IN_SECONDS) {
                    sessionManager.conclude();

                    new NewSessionExecutor(context).execute();
                    Log.d(CooeeSDKConstants.LOG_PREFIX, "After 30 min of App Background " + "Session Concluded");
                } else {
                    Map<String, Object> eventProps = new HashMap<>();
                    eventProps.put("CE Duration", backgroundDuration / 1000);
                    Event session = new Event("CE App Foreground", eventProps);

                    HttpCallsHelper.sendEvent(context, session, null);
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onEnterBackground() {
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
        });
    }

    /**
     * Handles the creation of triggers
     *
     * @param activity
     */
    private void handleTriggerDataFromActivity(Activity activity) {
        Bundle bundle = activity.getIntent().getBundleExtra(CooeeSDKConstants.INTENT_BUNDLE_KEY);

        // Should not go ahead if bundle is null
        if (bundle == null) {
            return;
        }

        TriggerData triggerData = bundle.getParcelable(CooeeSDKConstants.INTENT_TRIGGER_DATA_KEY);

        // Should not go ahead if triggerData is null or triggerData's id is null
        if (triggerData == null || triggerData.getId() == null) {
            return;
        }

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        EngagementTriggerHelper.renderInAppTrigger(context, triggerData);
                        HttpCallsHelper.sendEvent(context, new Event("CE Notification Clicked", new HashMap<>()), null);
                    }
                }, 4000);
    }

    /**
     * This block handle the glassmorphism effect for the triggers
     *
     * @param activity The currently created activity.
     */
    private void handleGlassmorphismAfterLaunch(Activity activity) {
        // Do not entertain if activity is instance of InAppTriggerActivity
        if (activity instanceof InAppTriggerActivity) {
            return;
        }

        // Do not entertain if onInAppPopListener in not initialized
        if (InAppTriggerActivity.onInAppPopListener == null) {
            return;
        }

        // Do not entertain if InAppTriggerActivity's isManualClose set true
        if (InAppTriggerActivity.isManualClose) {
            return;
        }

        // TODO Why are we pulling from local storage
        String triggerString = LocalStorageHelper.getString(activity, "trigger", null);
        EngagementTriggerHelper.renderInAppTriggerFromJSONString(activity, triggerString);
    }

    /**
     * This method will check if job is currently present or not with system
     * If job is not present it will add job in a queue
     *
     * @param context will be application context
     */
    private void checkAndStartJob(Context context) {
        // TODO: 03/06/21 Do we really need to check
        // TODO: 03/06/21 Do we really need to start manually
        if (!CooeeJobSchedulerBroadcast.isJobServiceOn(context)) {
            CooeeJobScheduler.schedulePendingTaskJob(context);
        }
    }

    /**
     * Send server check message every 5 min that session is still alive
     */
    // TODO: 03/06/21 Move to SessionManager
    private void keepSessionAlive() {
        //send server check message every 5 min that session is still alive
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, CooeeSDKConstants.KEEP_ALIVE_TIME_IN_MS);
            this.sessionManager.pingServerToKeepAlive();

        }, CooeeSDKConstants.KEEP_ALIVE_TIME_IN_MS);
    }
}
