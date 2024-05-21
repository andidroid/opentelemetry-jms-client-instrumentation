package me.andidroid.artemis.opentelemetry.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import me.andidroid.artemis.opentelemetry.client.common.MessageTextMapGetter;
import me.andidroid.artemis.opentelemetry.client.common.OpenTelemetryJMSClientUtils;

/**
 * Tracing decorator for JMS MessageConsumer
 */
public class TracingMessageConsumer implements MessageConsumer {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingMessageConsumer.class);

    private final Instrumenter<Message, Message> instrumenter = OpenTelemetryJMSClientUtils.getConsumerInstrumenter();

    private final MessageConsumer messageConsumer;
    private final Tracer tracer;
    private final boolean traceInLog;

    public TracingMessageConsumer(MessageConsumer messageConsumer, Tracer tracer) {
        this(messageConsumer, tracer, false);
    }

    public TracingMessageConsumer(MessageConsumer messageConsumer, Tracer tracer,
            boolean traceInLog) {
        this.messageConsumer = messageConsumer;
        this.tracer = tracer;
        this.traceInLog = traceInLog;
    }

    @Override
    public String getMessageSelector() throws JMSException {
        return messageConsumer.getMessageSelector();
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return messageConsumer.getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        if (listener instanceof TracingMessageConsumer) {
            messageConsumer.setMessageListener(listener);
        } else {
            messageConsumer.setMessageListener(new TracingMessageListener(listener, tracer, traceInLog));
        }
    }

    @Override
    public Message receive() throws JMSException {
        Message message = messageConsumer.receive();
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public Message receive(long timeout) throws JMSException {
        Message message = messageConsumer.receive(timeout);
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public Message receiveNoWait() throws JMSException {
        Message message = messageConsumer.receiveNoWait();
        startAndFinishConsumerSpan(message);
        return message;
    }

    @Override
    public void close() throws JMSException {
        messageConsumer.close();
    }

    private void startAndFinishConsumerSpan(Message message) {
        LOGGER.debug("TracingMessageConsumer.startAndFinishConsumerSpan");
        Context context = Context.current();
        boolean shouldStart = instrumenter.shouldStart(context, message);
        LOGGER.debug("shouldStart {}", shouldStart);
        if (shouldStart) {
            context = instrumenter.start(context, message);
            Scope scope = context.makeCurrent();
            scope.close();
        } else {
            return;
        }

        instrumenter.end(context, message, message, null);

    }

}