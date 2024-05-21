package me.andidroid.artemis.opentelemetry.client.common;

import io.opentelemetry.context.propagation.TextMapSetter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public class MessageTextMapSetter implements TextMapSetter<Message> {
    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(MessageTextMapSetter.class);

    @Override
    public void set(Message message, String key, String value) {
        LOGGER.debug("MessageTextMapSetter.set: {}", key);
        try {
            message.setStringProperty(key, value);
        } catch (JMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
