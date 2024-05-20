package fr.fezlight.eventsystem.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.modulith.events.Externalized;

import java.util.Objects;
import java.util.StringJoiner;

@Externalized
public class EventWrapper<T> {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T event;
    private final String handlerName;
    private Integer retryLeft;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EventWrapper(@JsonProperty("event") T event,
                        @JsonProperty("handlerName") String handlerName,
                        @JsonProperty("retryLeft") Integer retryLeft) {
        this.event = Objects.requireNonNull(event, "event cannot be null");
        this.handlerName = Objects.requireNonNull(handlerName, "handlerName cannot be null");
        this.retryLeft = retryLeft;
    }

    public static <T> EventWrapperBuilder<T> builder() {
        return new EventWrapperBuilder<>();
    }

    public T getEvent() {
        return this.event;
    }

    public String getHandlerName() {
        return this.handlerName;
    }

    public void setRetryLeft(Integer retryLeft) {
        this.retryLeft = retryLeft;
    }

    public Integer getRetryLeft() {
        return this.retryLeft;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventWrapper.class.getSimpleName() + "[", "]")
                .add("event=" + event)
                .add("handlerName='" + handlerName + "'")
                .add("retryLeft=" + retryLeft)
                .toString();
    }

    public static class EventWrapperBuilder<T> {
        private T event;
        private String handlerName;
        private Integer retryLeft;

        EventWrapperBuilder() {
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public EventWrapperBuilder<T> event(T event) {
            this.event = event;
            return this;
        }

        public EventWrapperBuilder<T> handlerName(String handlerName) {
            this.handlerName = handlerName;
            return this;
        }

        public EventWrapperBuilder<T> retryLeft(Integer retryLeft) {
            this.retryLeft = retryLeft;
            return this;
        }

        public EventWrapper<T> build() {
            return new EventWrapper<>(this.event, this.handlerName, this.retryLeft);
        }
    }
}
