package me.andidroid.artemis.opentelemetry.client;

import jakarta.jms.BytesMessage;
import jakarta.jms.CompletionListener;
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
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import me.andidroid.artemis.opentelemetry.client.common.MessageTextMapSetter;
import me.andidroid.artemis.opentelemetry.client.common.OpenTelemetryJMSClientUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;

/**
 * Tracing decorator for JMS JMSProducer
 */
public class TracingJMSProducer implements JMSProducer {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingJMSProducer.class);

    private final JMSProducer jmsProducer;
    private JMSContext jmsContext = null;
    private Session jmsSession = null;
    private final Tracer tracer;
    private OpenTelemetry openTelemetry;
    private Instrumenter<Message, Message> instrumenter; // = OpenTelemetryJMSClientUtils.getProducerInstrumenter();

    public TracingJMSProducer(JMSProducer jmsProducer, JMSContext jmsContext, OpenTelemetry openTelemetry,
            Tracer tracer) {
        this.jmsProducer = jmsProducer;
        this.jmsContext = jmsContext;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
        this.instrumenter = OpenTelemetryJMSClientUtils.getProducerInstrumenter(openTelemetry);
    }

    public TracingJMSProducer(JMSProducer jmsProducer, Session jmsSession, OpenTelemetry openTelemetry, Tracer tracer) {
        this.jmsProducer = jmsProducer;
        this.jmsSession = jmsSession;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
        this.instrumenter = OpenTelemetryJMSClientUtils.getProducerInstrumenter(openTelemetry);
    }

    @Override
    public JMSProducer clearProperties() {
        jmsProducer.clearProperties();
        return this;
    }

    @Override
    public CompletionListener getAsync() {
        return jmsProducer.getAsync();
    }

    @Override
    public boolean getBooleanProperty(String arg0) {
        return jmsProducer.getBooleanProperty(arg0);
    }

    @Override
    public byte getByteProperty(String arg0) {
        return jmsProducer.getByteProperty(arg0);
    }

    @Override
    public long getDeliveryDelay() {
        return jmsProducer.getDeliveryDelay();
    }

    @Override
    public int getDeliveryMode() {
        return jmsProducer.getDeliveryMode();
    }

    @Override
    public boolean getDisableMessageID() {
        return jmsProducer.getDisableMessageID();
    }

    @Override
    public boolean getDisableMessageTimestamp() {
        return jmsProducer.getDisableMessageTimestamp();
    }

    @Override
    public double getDoubleProperty(String arg0) {
        return jmsProducer.getDoubleProperty(arg0);
    }

    @Override
    public float getFloatProperty(String arg0) {
        return jmsProducer.getFloatProperty(arg0);
    }

    @Override
    public int getIntProperty(String arg0) {
        return jmsProducer.getIntProperty(arg0);
    }

    @Override
    public String getJMSCorrelationID() {
        return jmsProducer.getJMSCorrelationID();
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        return jmsProducer.getJMSCorrelationIDAsBytes();
    }

    @Override
    public Destination getJMSReplyTo() {
        return jmsProducer.getJMSReplyTo();
    }

    @Override
    public String getJMSType() {
        return jmsProducer.getJMSType();
    }

    @Override
    public long getLongProperty(String arg0) {
        return jmsProducer.getLongProperty(arg0);
    }

    @Override
    public Object getObjectProperty(String arg0) {
        return jmsProducer.getObjectProperty(arg0);
    }

    @Override
    public int getPriority() {
        return jmsProducer.getPriority();
    }

    @Override
    public Set<String> getPropertyNames() {
        return jmsProducer.getPropertyNames();
    }

    @Override
    public short getShortProperty(String arg0) {
        return jmsProducer.getShortProperty(arg0);
    }

    @Override
    public String getStringProperty(String arg0) {
        return jmsProducer.getStringProperty(arg0);
    }

    @Override
    public long getTimeToLive() {
        return jmsProducer.getTimeToLive();
    }

    @Override
    public boolean propertyExists(String arg0) {
        return jmsProducer.propertyExists(arg0);
    }

    @Override
    public JMSProducer send(Destination destination, Message message) {
        Context context = Context.current();

        try {
            message.setJMSDestination(destination);
            message.setStringProperty("clientid", this.jmsContext.getClientID());
        } catch (JMSException e) {
            e.printStackTrace();
        }

        boolean shouldStart = this.instrumenter.shouldStart(context, message);
        Scope scope = null;
        LOGGER.debug("TracingJMSProducer.send: shouldStart={}", shouldStart);
        if (shouldStart) {
            context = this.instrumenter.start(context, message);
            scope = context.makeCurrent();
            LOGGER.debug("TracingJMSProducer.send: started {}", message);
        }

        SpanBuilder spanBuilder = openTelemetry.getTracer("TracingJMSProducer")
                .spanBuilder("TracingJMSProducer.send")
                .setParent(context)
                .setAttribute("message", message.toString())
                .setSpanKind(SpanKind.CLIENT);
        Span span = spanBuilder.startSpan();

        boolean shouldEnd = true;
        try {

            // InstrumenterUtil.startAndEnd();

            if (jmsProducer.getAsync() instanceof TracingCompletionListener) {
                shouldEnd = false;
                ((TracingCompletionListener) jmsProducer.getAsync()).setContext(context);
                ((TracingCompletionListener) jmsProducer.getAsync()).setInstrumenter(instrumenter);
                ((TracingCompletionListener) jmsProducer.getAsync()).setScope(scope);
            }

            jmsProducer.send(destination, message);
            span.addEvent("message send");
            if (shouldEnd) {
                // span.end();
                this.instrumenter.end(context, message, null, null);
                if (scope != null) {
                    scope.close();
                }
            }
        } catch (Throwable e) {
            this.instrumenter.end(context, message, null, e);
            // SpanJmsDecorator.onError(e, span);
            throw e;
        } finally {
            span.end();
            // span.end();
        }
        return this;
    }

    @Override
    public JMSProducer send(Destination destination, String message) {
        TextMessage textMsg;
        try {
            textMsg = getTextMessage();
        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, message);
            return this;
        }
        if (textMsg == null) {
            // if textMsg is null, conversion failed
            jmsProducer.send(destination, message);
            return this;
        }

        try {
            textMsg.setText(message);
        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, message);
            return this;
        }
        return send(destination, textMsg);
    }

    private TextMessage getTextMessage() throws JMSException {
        TextMessage textMsg = null;
        if (jmsContext != null) {
            textMsg = jmsContext.createTextMessage();
        } else if (jmsSession != null) {
            textMsg = jmsSession.createTextMessage();
        }
        return textMsg;
    }

    @Override
    public JMSProducer send(Destination destination, Map<String, Object> arg1) {
        MapMessage mapMsg;
        try {
            mapMsg = getMapMessage();
        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, arg1);
            return this;
        }
        // if mapMsg is null, conversion failed
        if (mapMsg == null) {
            jmsProducer.send(destination, arg1);
            return this;
        }

        for (Map.Entry<String, Object> entry : arg1.entrySet()) {
            try {
                mapMsg.setObject(entry.getKey(), entry.getValue());
            } catch (JMSException e) {
                e.printStackTrace();
                jmsProducer.send(destination, arg1);
                return this;
            }
        }
        return send(destination, mapMsg);
    }

    private MapMessage getMapMessage() throws JMSException {
        MapMessage mapMsg = null;
        if (jmsContext != null) {
            mapMsg = jmsContext.createMapMessage();
        } else if (jmsSession != null) {
            mapMsg = jmsSession.createMapMessage();
        }
        return mapMsg;
    }

    @Override
    public JMSProducer send(Destination destination, byte[] arg1) {
        BytesMessage bytesMsg;
        try {
            bytesMsg = getBytesMessage();

        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, arg1);
            return this;
        }

        if (bytesMsg == null) {
            // if bytesMsg is null, conversion failed
            jmsProducer.send(destination, arg1);
            return this;
        }

        try {
            bytesMsg.writeBytes(arg1);
        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, arg1);
            return this;
        }

        return send(destination, bytesMsg);
    }

    private BytesMessage getBytesMessage() throws JMSException {
        BytesMessage bytesMsg = null;
        if (jmsContext != null) {
            bytesMsg = jmsContext.createBytesMessage();
        } else if (jmsSession != null) {
            bytesMsg = jmsSession.createBytesMessage();
        }
        return bytesMsg;
    }

    @Override
    public JMSProducer send(Destination destination, Serializable obj) {
        Message message;
        try {
            message = createJMSMessage(obj);
        } catch (JMSException e) {
            e.printStackTrace();
            jmsProducer.send(destination, obj);
            return this;
        }

        if (message == null) {
            // if message is null, conversion failed
            jmsProducer.send(destination, obj);
            return this;
        }

        return send(destination, message);
    }

    private Message createJMSMessage(Serializable obj) throws JMSException {
        if (obj instanceof String) {
            TextMessage textMsg = getTextMessage();
            // if textMsg is null, conversion failed
            if (textMsg == null) {
                return null;
            }
            textMsg.setText((String) obj);
            return textMsg;
        } else {
            ObjectMessage objMsg = getObjectMessage();
            if (objMsg == null) {
                return null;
            }
            objMsg.setObject(obj);
            return objMsg;
        }
    }

    private ObjectMessage getObjectMessage() throws JMSException {
        ObjectMessage objectMsg = null;
        if (jmsContext != null) {
            objectMsg = jmsContext.createObjectMessage();
        } else if (jmsSession != null) {
            objectMsg = jmsSession.createObjectMessage();
        }
        return objectMsg;
    }

    @Override
    public JMSProducer setAsync(CompletionListener arg0) {

        jmsProducer.setAsync(new TracingCompletionListener(openTelemetry, instrumenter, null, arg0));
        return this;
    }

    @Override
    public JMSProducer setDeliveryDelay(long arg0) {
        jmsProducer.setDeliveryDelay(arg0);
        return this;
    }

    @Override
    public JMSProducer setDeliveryMode(int arg0) {
        jmsProducer.setDeliveryMode(arg0);
        return this;
    }

    @Override
    public JMSProducer setDisableMessageID(boolean arg0) {
        jmsProducer.setDisableMessageID(arg0);
        return this;
    }

    @Override
    public JMSProducer setDisableMessageTimestamp(boolean arg0) {
        jmsProducer.setDisableMessageTimestamp(arg0);
        return this;
    }

    @Override
    public JMSProducer setJMSCorrelationID(String arg0) {
        jmsProducer.setJMSCorrelationID(arg0);
        return this;
    }

    @Override
    public JMSProducer setJMSCorrelationIDAsBytes(byte[] arg0) {
        jmsProducer.setJMSCorrelationIDAsBytes(arg0);
        return this;
    }

    @Override
    public JMSProducer setJMSReplyTo(Destination destination) {
        jmsProducer.setJMSReplyTo(destination);
        return this;
    }

    @Override
    public JMSProducer setJMSType(String arg0) {
        jmsProducer.setJMSType(arg0);
        return this;
    }

    @Override
    public JMSProducer setPriority(int arg0) {
        jmsProducer.setPriority(arg0);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, boolean arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, byte arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, short arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, int arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, long arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, float arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, double arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, String arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setProperty(String arg0, Object arg1) {
        jmsProducer.setProperty(arg0, arg1);
        return this;
    }

    @Override
    public JMSProducer setTimeToLive(long arg0) {
        jmsProducer.setTimeToLive(arg0);
        return this;
    }

}