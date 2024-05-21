package me.andidroid.artemis.opentelemetry.client;

import org.slf4j.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import me.andidroid.artemis.opentelemetry.client.common.MessageTextMapGetter;
import me.andidroid.artemis.opentelemetry.client.common.MessageTextMapSetter;
import me.andidroid.artemis.opentelemetry.client.common.OpenTelemetryJMSClientUtils;

/**
 * Tracing decorator for JMS MessageListener
 */
public class TracingMessageListener implements MessageListener {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingMessageListener.class);

    private final MessageListener messageListener;
    private final Tracer tracer;
    private final boolean traceInLog;
    private final Instrumenter<Message, Message> instrumenter = OpenTelemetryJMSClientUtils.getConsumerInstrumenter();

    public TracingMessageListener(MessageListener messageListener, Tracer tracer) {
        this(messageListener, tracer, false);
    }

    public TracingMessageListener(MessageListener messageListener, Tracer tracer,
            boolean traceInLog) {
        this.messageListener = messageListener;
        this.tracer = tracer;
        this.traceInLog = traceInLog;
    }

    @Override
    public void onMessage(Message message) {

        LOGGER.debug("TracingMessageListener.onMessage: " + message);

        Context context = Context.current();
        boolean shouldStart = instrumenter.shouldStart(context, message);

        if (shouldStart) {

            context = instrumenter.start(context, message);
            context.makeCurrent();
        } else {
            return;
        }

        // Span span = TracingMessageUtils.startListenerSpan(message, tracer);
        // if (traceInLog) {
        // MDC.put("spanId", span.getSpanContext().getSpanId());
        // MDC.put("traceId", span.getSpanContext().getTraceId());
        // }

        try {
            // span.makeCurrent();
            if (messageListener != null) {
                messageListener.onMessage(message);
            }
        } finally {
            // span.end();
            instrumenter.end(context, message, message, null);
            // if (traceInLog) {
            // MDC.remove("spanId");
            // MDC.remove("traceId");
            // }
        }

    }
}