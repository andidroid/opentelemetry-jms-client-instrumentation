package me.andidroid.artemis.opentelemetry.client.common;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

@Deprecated
final class MessageSpanNameExtractor implements SpanNameExtractor<Message> {

    public MessageSpanNameExtractor() {
    }

    @Override
    public String extract(Message message) {
        try {
            // return message.getStringProperty("spanName");

            return message.getJMSMessageID();
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }
}