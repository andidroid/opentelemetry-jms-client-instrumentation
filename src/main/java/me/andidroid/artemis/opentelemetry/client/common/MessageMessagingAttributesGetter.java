package me.andidroid.artemis.opentelemetry.client.common;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesGetter;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

final class MessageMessagingAttributesGetter
        implements MessagingAttributesGetter<Message, Message> {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(MessageMessagingAttributesGetter.class);

    // @Override
    // public Long getBatchMessageCount(Message arg0, Message arg1) {
    // return null;
    // }

    // @Override
    // public String getClientId(Message message) {
    // try {
    // return message.getStringProperty("clientId");
    // } catch (JMSException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // return null;
    // }

    @Override
    public String getConversationId(Message message) {
        try {
            return message.getJMSCorrelationID();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDestination(Message message) {
        try {
            return Objects.toString(message.getJMSDestination(), null);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    // @Override
    // public String getDestinationTemplate(Message message) {
    // try {
    // return Objects.toString(message.getJMSDestination(), null);
    // } catch (JMSException e) {
    // throw new RuntimeException(e);
    // }
    // }

    // @Override
    // public Long getMessageBodySize(Message message) {
    // try {
    // return (long) message.getBody(String.class).length();
    // } catch (JMSException e) {
    // return null;
    // }
    // }

    // @Override
    // public Long getMessageEnvelopeSize(Message arg0) {
    // return null;
    // }

    @Override
    public String getMessageId(Message message, Message response) {
        try {
            return message.getJMSMessageID();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSystem(Message arg0) {
        return "jms";
    }

    // @Override
    // public boolean isAnonymousDestination(Message arg0) {
    // return false;
    // }

    @Override
    public boolean isTemporaryDestination(Message arg0) {
        return false;
    }

    @Override
    public List<String> getMessageHeader(Message request, String name) {
        LOGGER.debug("getMessageHeader {}", name);
        try {
            String stringProperty = request.getStringProperty(name);
            if (stringProperty != null) {
                return Collections.singletonList(stringProperty);
            }
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public Long getMessagePayloadCompressedSize(Message arg0) {
        return null;
    }

    @Override
    public Long getMessagePayloadSize(Message arg0) {
        return null;
    }
}