package com.letscooee.trigger.adapters;

import androidx.annotation.RestrictTo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.letscooee.enums.trigger.PushType;

import java.lang.reflect.Type;

/**
 * A Gson deserializer to deserialize the {@link com.letscooee.models.trigger.push.PushNotificationTrigger} elements.
 * * This checks the {@code pt} key in json and then identifies
 * * based on {@link PushType}.
 *
 * @author Ashish Gaikwad 20/05/22
 * @since 1.3.11
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PushTypeDeserializer implements JsonDeserializer<PushType> {
    @Override
    public PushType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        int pushType = json.getAsInt();

        switch (pushType) {
            case 0:
            case 1:
                return PushType.NORMAL;
            case 2:
                return PushType.LARGE;
            case 20:
                return PushType.SMALL;
            default:
                return null;
        }
    }
}
