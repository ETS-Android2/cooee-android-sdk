package com.letscooee.trigger.inapp;

import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.letscooee.models.trigger.TriggerData;
import com.letscooee.utils.Closure;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple data holder class shared across different renderers.
 *
 * @author Shashank Agrawal
 * @since 1.0.0
 */
public class TriggerContext {

    private final Map<String, Object> closedEventProps = new HashMap<>();

    private Bitmap bitmapForBlurry;
    private ViewGroup viewGroupForBlurry;
    private Closure<Map<String, Object>> callback;
    private TriggerData triggerData;
    private RelativeLayout triggerParentLayout;
    private double scalingFactor;

    public Map<String, Object> getClosedEventProps() {
        return closedEventProps;
    }

    public void onExit(@NonNull Closure<Map<String, Object>> callback) {
        this.callback = callback;
    }

    public void closeInApp(String closeBehaviour) {
        closedEventProps.put("closeBehaviour", closeBehaviour);

        if (callback != null) {
            callback.call(null);
        }
    }

    public Bitmap getBitmapForBlurry() {
        return bitmapForBlurry;
    }

    public void setBitmapForBlurry(Bitmap bitmapForBlurry) {
        this.bitmapForBlurry = bitmapForBlurry;
    }

    public ViewGroup getViewGroupForBlurry() {
        return viewGroupForBlurry;
    }

    public void setViewGroupForBlurry(ViewGroup viewGroupForBlurry) {
        this.viewGroupForBlurry = viewGroupForBlurry;
    }

    public TriggerData getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(TriggerData triggerData) {
        this.triggerData = triggerData;
    }

    public RelativeLayout getTriggerParentLayout() {
        return triggerParentLayout;
    }

    public void setTriggerParentLayout(RelativeLayout triggerParentLayout) {
        this.triggerParentLayout = triggerParentLayout;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = Math.min(scalingFactor, 1);
    }
}
