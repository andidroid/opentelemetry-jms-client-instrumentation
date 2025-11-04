package me.andidroid.artemis.opentelemetry.client;

import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import me.andidroid.artemis.opentelemetry.client.common.MessageTextMapGetter;
import me.andidroid.artemis.opentelemetry.client.common.OpenTelemetryJMSClientUtils;
import me.andidroid.artemis.opentelemetry.client.common.TracingJMSContextProducer;

import java.io.Serializable;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/**
 * Tracing decorator for JMS {@code javax.jms.JMSConsumer}.
 */
public class TracingJMSConsumer implements JMSConsumer {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingJMSConsumer.class);

    private final Instrumenter<Message, Message> instrumenter;// =
                                                              // OpenTelemetryJMSClientUtils.getConsumerInstrumenter();

    private final JMSConsumer jmsConsumer;
    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;
    private final Meter meter;
    private final LongCounter counter;

    public TracingJMSConsumer(JMSConsumer jmsConsumer, OpenTelemetry openTelemetry, Tracer tracer, Meter meter) {
        this.jmsConsumer = jmsConsumer;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
        this.instrumenter = OpenTelemetryJMSClientUtils.getConsumerInstrumenter(openTelemetry);
        this.meter = meter;

        counter = meter.counterBuilder("jms-receive").setDescription("reveived messages").build();
    }

    @Override
    public String getMessageSelector() {
        return jmsConsumer.getMessageSelector();
    }

    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {
        return jmsConsumer.getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSRuntimeException {
        if (listener instanceof TracingMessageConsumer) {
            jmsConsumer.setMessageListener(listener);
        } else {
            jmsConsumer.setMessageListener(new TracingMessageListener(listener, openTelemetry, tracer));
        }
    }

    @Override
    public Message receive() {
        Message message = jmsConsumer.receive();
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public Message receive(long timeout) {
        Message message = jmsConsumer.receive(timeout);
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public Message receiveNoWait() {
        Message message = jmsConsumer.receiveNoWait();
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public void close() {
        jmsConsumer.close();
    }

    @Override
    public <T> T receiveBody(Class<T> c) {
        return jmsConsumer.receiveBody(c);
    }

    @Override
    public <T> T receiveBody(Class<T> c, long timeout) {
        return jmsConsumer.receiveBody(c, timeout);
    }

    @Override
    public <T> T receiveBodyNoWait(Class<T> c) {
        return jmsConsumer.receiveBodyNoWait(c);
    }

    private void startAndFinishConsumerSpan(Message message) {
        LOGGER.debug("TracingJMSConsumer.startAndFinishConsumerSpan");

        Context context = Context.current();
        String queueName = "";
        try {
            queueName = message.getJMSDestination().toString();
        } catch (Exception e) {
            // TODO: handle exception
        }

        boolean shouldStart = instrumenter.shouldStart(context, message);
        // tracer.spanBuilder("consume message").setParent()
        Scope scope = null;
        LOGGER.trace("shouldStart {}", shouldStart);
        if (shouldStart) {
            context = instrumenter.start(context, message);
            scope = context.makeCurrent();
            try {
                Span.fromContext(context).addEvent("messageReceive").setAttribute("messageId",
                        message.getJMSMessageID()).setAttribute("messageRedelivered",
                                message.getJMSRedelivered())
                        .setAttribute("queue", queueName).setAttribute("selector", jmsConsumer.getMessageSelector());
            } catch (JMSException e) {
                LOGGER.error("error getting message meta data", e);
            }
        } else {
            return;
        }

        instrumenter.end(context, message, message, null);
        if (scope != null) {
            scope.close();
        }

        // String name = annotation.name();
        // String description = annotation.description();
        // String unit = annotation.unit();
        Attributes args = Attributes.builder().put("queue", queueName)
                .put("selector", jmsConsumer.getMessageSelector()).build();
        // method.getDeclaringClass().getSimpleName() + "." +
        // method.getName()
        // .setUnit(unit)
        // .setDescription(description)

        // String messageSelector = getMessageSelector();

        counter.add(1, args);

    }
}