package me.andidroid.artemis.opentelemetry.client.common;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public class MessageTextMapGetter implements TextMapGetter<Message> {
    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(MessageTextMapGetter.class);

    @Override
    public String get(Message message, String key) {
        try {
            LOGGER.debug("MessageTextMapGetter.get: {}", key);
            return message.getStringProperty(key);
        } catch (JMSException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<String> keys(Message message) {
        LOGGER.debug("MessageTextMapGetter.keys()");
        Set<String> keys = new HashSet<>();
        try {
            Enumeration<String> enumeration = message.getPropertyNames();
            while (enumeration.hasMoreElements()) {
                keys.add(enumeration.nextElement());
            }
            return keys;
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
