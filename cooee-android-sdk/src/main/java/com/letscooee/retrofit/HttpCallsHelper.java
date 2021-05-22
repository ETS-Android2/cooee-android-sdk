package com.letscooee.retrofit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.letscooee.init.ActivityLifecycleCallback;
import com.letscooee.init.PostLaunchActivity;
import com.letscooee.models.Event;
import com.letscooee.room.CooeeDatabase;
import com.letscooee.room.postoperations.entity.PendingTask;
import com.letscooee.room.postoperations.enums.EventType;
import com.letscooee.utils.Closure;
import com.letscooee.utils.CooeeSDKConstants;
import com.letscooee.utils.LocalStorageHelper;
import com.letscooee.utils.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HttpCallsHelper will be used to create http calls to the server
 *
 * @author Abhishek Taparia
 */
public final class HttpCallsHelper {

    static ServerAPIService serverAPIService = APIClient.getServerAPIService();
    static Gson gson = new Gson();


    public static void sendEvent(Context context, Event event, Closure closure) {
        CooeeDatabase db = CooeeDatabase.getInstance(context);

        SessionManager sessionManager = SessionManager.getInstance(context);
        event.setSessionID(sessionManager.getCurrentSessionId());

        sendEventWithoutSDKState(context, event, db, closure);

    }

    public static void sendEventWithoutSDKState(Context context, Event event, CooeeDatabase db, Closure closure) {
        event.setScreenName(ActivityLifecycleCallback.getCurrentScreen());
        event.setSessionNumber(PostLaunchActivity.currentSessionNumber);

        ArrayList<HashMap<String, String>> allTriggers = LocalStorageHelper.getList(context, CooeeSDKConstants.STORAGE_ACTIVE_TRIGGERS);

        ArrayList<HashMap<String, String>> activeTriggerList = new ArrayList<>();

        for (HashMap<String, String> map : allTriggers) {
            long time = Long.parseLong(map.get("duration"));
            long currentTime = new Date().getTime();
            if (time > currentTime) {
                activeTriggerList.add(map);
            }
        }

        event.setActiveTriggers(activeTriggerList);

        Date currentDate = new Date();
        event.setOccurred(currentDate);
        LocalStorageHelper.putListImmediately(context, CooeeSDKConstants.STORAGE_ACTIVE_TRIGGERS, activeTriggerList);

        PendingTask task = new PendingTask();
        task.attempts = 0;
        task.data = gson.toJson(event);
        task.type = EventType.EVENT;
        task.dateCreated = currentDate.getTime();
        db.pendingTaskDAO().insertAll(task);


    }

    public static void pushEvent(Event event, Closure closure, CooeeDatabase appDatabase, PendingTask task) {
        Date currentTime = new Date();
        serverAPIService.sendEvent(event).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.i(CooeeSDKConstants.LOG_PREFIX, event.getName() + " Event Sent Code: " + response.code());

                if (closure != null) {
                    closure.call(response.body());
                }

                if (appDatabase != null) {
                    if (response.isSuccessful()) {
                        appDatabase.pendingTaskDAO().delete(task);
                    } else {
                        updateData(appDatabase, task, currentTime);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(CooeeSDKConstants.LOG_PREFIX, event.getName() + " Event Sent Error Message: " + t.toString());

                if (task != null) {
                    int count = task.attempts + 1;
                    appDatabase.pendingTaskDAO().update(task.id, count, currentTime.getTime());
                }
                //Sentry.captureException(t);
            }
        });
    }

    public static void sendUserProfile(Context context, Map<String, Object> userMap, String msg, Closure closure) {
        Date currentTime = new Date();
        CooeeDatabase db = CooeeDatabase.getInstance(context);
        userMap.put("occurred", currentTime);

        SessionManager sessionManager = SessionManager.getInstance(context);
        userMap.put("sessionID", sessionManager.getCurrentSessionId());

        PendingTask task = new PendingTask();
        task.attempts = 0;
        task.data = gson.toJson(userMap);
        task.type = EventType.PROFILE;
        task.dateCreated = currentTime.getTime();
        db.pendingTaskDAO().insertAll(task);

    }

    public static void pushUserProfile(Map<String, Object> userMap, String msg, Closure closure, CooeeDatabase appDatabase, PendingTask task) {
        Date currentTime = new Date();
        serverAPIService.updateProfile(userMap).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Log.i(CooeeSDKConstants.LOG_PREFIX, msg + " User Profile Response Code : " + response.code());
                if (response.isSuccessful()) {
                    appDatabase.pendingTaskDAO().delete(task);
                } else {
                    updateData(appDatabase, task, currentTime);
                }

                if (closure == null) {          // space change
                    return;
                }

                if (response.body() != null) {          // space change
                    closure.call(response.body());
                }

            }

            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(CooeeSDKConstants.LOG_PREFIX, msg + " User Profile Error Message : " + t.toString());
                int count = task.attempts + 1;
                appDatabase.pendingTaskDAO().update(task.id, count, currentTime.getTime());
                //Sentry.captureException(t);
            }
        });
    }

    public static void sendSessionConcludedEvent(int duration, Context context) {
        Date currentTime = new Date();
        CooeeDatabase db = CooeeDatabase.getInstance(context);

        SessionManager sessionManager = SessionManager.getInstance(context);

        Map<String, Object> sessionConcludedRequest = new HashMap<>();
        sessionConcludedRequest.put("sessionID", sessionManager.getCurrentSessionId());
        sessionConcludedRequest.put("duration", duration);
        sessionConcludedRequest.put("occurred", currentTime);

        PendingTask task = new PendingTask();
        task.attempts = 0;
        task.data = gson.toJson(sessionConcludedRequest);
        task.type = EventType.SESSION_CONCLUDED;
        task.dateCreated = currentTime.getTime();
        db.pendingTaskDAO().insertAll(task);
        sessionManager.destroySession();

    }

    public static void pushSessionConcluded(Map<String, Object> sessionConcludedRequest, CooeeDatabase appDatabase, PendingTask task) {
        Date currentTime = new Date();
        serverAPIService.concludeSession(sessionConcludedRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(CooeeSDKConstants.LOG_PREFIX, "Session Concluded Event Sent Code : " + response.code());
                if (response.isSuccessful()) {
                    appDatabase.pendingTaskDAO().delete(task);
                } else {
                    updateData(appDatabase, task, currentTime);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(CooeeSDKConstants.LOG_PREFIX, "Session Concluded Event Sent Error Message" + t.toString());
                int count = task.attempts + 1;
                appDatabase.pendingTaskDAO().update(task.id, count, currentTime.getTime());
                //Sentry.captureException(t);
            }
        });
    }

    public static void keepAlive(Context context) {
        Date currentTime = new Date();
        CooeeDatabase db = CooeeDatabase.getInstance(context);
        SessionManager sessionManager = SessionManager.getInstance(context);

        Map<String, Object> keepAliveRequest = new HashMap<>();
        keepAliveRequest.put("sessionID", sessionManager.getCurrentSessionId());
        keepAliveRequest.put("occurred", currentTime);

        PendingTask task = new PendingTask();
        task.attempts = 0;
        task.data = gson.toJson(keepAliveRequest);
        task.type = EventType.KEEP_ALIVE;
        task.dateCreated = currentTime.getTime();
        db.pendingTaskDAO().insertAll(task);

    }

    public static void pushKeepAlive(Map<String, Object> keepAliveRequest, CooeeDatabase appDatabase, PendingTask task) {
        Date currentTime = new Date();
        serverAPIService.keepAlive(keepAliveRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(CooeeSDKConstants.LOG_PREFIX, "Session Alive Response Code : " + response.code());
                if (response.isSuccessful()) {
                    appDatabase.pendingTaskDAO().delete(task);
                } else {
                    updateData(appDatabase, task, currentTime);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(CooeeSDKConstants.LOG_PREFIX, "Session Alive Response Error Message" + t.toString());
                //Sentry.captureException(t);
                int count = task.attempts + 1;
                appDatabase.pendingTaskDAO().update(task.id, count, currentTime.getTime());
            }
        });
    }

    public static void setFirebaseToken(String firebaseToken, Context context) {
        Date currentTime = new Date();
        CooeeDatabase db = CooeeDatabase.getInstance(context);
        SessionManager sessionManager = SessionManager.getInstance(context);

        Map<String, Object> tokenRequest = new HashMap<>();
        tokenRequest.put("sessionID", sessionManager.getCurrentSessionId());
        tokenRequest.put("firebaseToken", firebaseToken);
        tokenRequest.put("occurred", currentTime);

        PendingTask task = new PendingTask();
        task.attempts = 0;
        task.data = gson.toJson(tokenRequest);
        task.type = EventType.FB_TOKEN;
        task.dateCreated = currentTime.getTime();
        db.pendingTaskDAO().insertAll(task);

    }

    public static void pushFirebaseToken(Map<String, Object> tokenRequest, CooeeDatabase appDatabase, PendingTask task) {
        Date currentTime = new Date();
        serverAPIService.setFirebaseToken(tokenRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(CooeeSDKConstants.LOG_PREFIX, "Firebase Token Response Code : " + response.code());
                if (response.isSuccessful()) {
                    appDatabase.pendingTaskDAO().delete(task);
                } else {
                    updateData(appDatabase, task, currentTime);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(CooeeSDKConstants.LOG_PREFIX, "Firebase Token Response Error Message" + t.toString());
                //Sentry.captureException(t);
                updateData(appDatabase, task, currentTime);
            }
        });
    }

    private static void updateData(CooeeDatabase appDatabase, PendingTask task, Date currentTime) {
        int count = task.attempts + 1;
        task.attempts = count;
        task.lastAttempted = currentTime.getTime();
        appDatabase.pendingTaskDAO().updateByObject(task);
    }
}
