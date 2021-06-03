package com.letscooee.room.task.processor;

import android.content.Context;
import com.letscooee.exceptions.HttpRequestFailedException;
import com.letscooee.models.Event;
import com.letscooee.room.task.PendingTask;
import com.letscooee.room.task.PendingTaskType;
import org.jetbrains.annotations.NotNull;

/**
 * Process a {@link PendingTask} which is related to pushing an {@link Event} to API.
 *
 * @author Shashank Agarwal
 * @since 0.3.0
 */
public class EventTaskProcessor extends HttpTaskProcessor<Event> {

    public EventTaskProcessor(Context context) {
        super(context);
    }

    protected void doHTTP(Event event) throws HttpRequestFailedException {
        this.baseHTTPService.sendEvent(event);
    }

    public boolean canProcess(@NotNull PendingTask task) {
        return task.type == PendingTaskType.API_SEND_EVENT;
    }
}
