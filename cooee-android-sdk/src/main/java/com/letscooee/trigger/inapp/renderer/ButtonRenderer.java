package com.letscooee.trigger.inapp.renderer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.letscooee.models.trigger.elements.BaseElement;
import com.letscooee.trigger.inapp.InAppGlobalData;

/**
 * @author shashank
 */
public class ButtonRenderer extends TextRenderer {

    public ButtonRenderer(Context context, ViewGroup parentView, BaseElement element, InAppGlobalData globalData) {
        super(context, parentView, element, globalData);
    }

    @Override
    public View render() {
        Button button = new Button(context);
        this.processTextData(button);

        return newElement;
    }
}