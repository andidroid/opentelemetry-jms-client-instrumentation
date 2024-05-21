package me.andidroid.artemis.opentelemetry.client;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import jakarta.jms.CompletionListener;
import jakarta.jms.Message;
import me.andidroid.artemis.opentelemetry.client.common.OpenTelemetryJMSClientUtils;
import me.andidroid.artemis.opentelemetry.client.common.TracingJMSContextProducer;

/**
 * Listener for sending messages.
 * <p>
 * If sending of the message is complete then method
 * {@code onCompletion(Message)} is called. <br>
 * If sending of the message fails then method {@code onException(Exception)} is
 * called.
 */
public class TracingCompletionListener implements CompletionListener {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingCompletionListener.class);

    private Span span;
    private final CompletionListener completionListener;
    private Instrumenter<Message, Message> instrumenter;
    private Context context;
    private Scope scope = null;

    public TracingCompletionListener(CompletionListener completionListener) {
        this.completionListener = completionListener;
    }

    public TracingCompletionListener(Span span, CompletionListener completionListener) {
        this.span = span;
        this.completionListener = completionListener;
    }

    public TracingCompletionListener(Instrumenter<Message, Message> instrumenter, Context context,
            CompletionListener completionListener) {
        this.instrumenter = instrumenter;
        this.completionListener = completionListener;
        this.context = context;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    @Override
    public void onCompletion(Message message) {

        SpanBuilder spanBuilder = OpenTelemetryJMSClientUtils.getOpenTelemetry().getTracer("TracingCompletionListener")
                .spanBuilder("TracingCompletionListener.onCompletion")
                .setParent(context)
                .setAttribute("message", message.toString())
                .setSpanKind(SpanKind.CLIENT);
        Span span = spanBuilder.startSpan();

        try {
            completionListener.onCompletion(message);
            span.addEvent("message completed");
        } finally {
            span.end();
            // span.end();
            LOGGER.debug("TracingCompletionListener.onCompletion: {}", message);
            if (scope != null) {
                scope.close();
            }
            this.instrumenter.end(context, message, message, null);
        }
    }

    @Override
    public void onException(Message message, Exception exception) {
        try {
            completionListener.onException(message, exception);
        } finally {
            // SpanJmsDecorator.onError(exception, span);
            // span.end();
            if (scope != null) {
                scope.close();
            }
            this.instrumenter.end(context, message, message, exception);
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * @param instrumenter the instrumenter to set
     */
    public void setInstrumenter(Instrumenter<Message, Message> instrumenter) {
        this.instrumenter = instrumenter;
    }

    /**
     * @param scope the scope to set
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }
}