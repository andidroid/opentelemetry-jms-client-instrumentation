package me.andidroid.artemis.opentelemetry.client;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.jms.CompletionListener;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;

/**
 * Tracing decorator for JMS {@code javax.jms.ConnectionFactory}.
 */
public class TracingConnectionFactory implements ConnectionFactory {

    private final ConnectionFactory connectionFactory;
    private final Tracer tracer;
    private final Meter meter;
    private final OpenTelemetry openTelemetry;

    public TracingConnectionFactory(ConnectionFactory connectionFactory, OpenTelemetry openTelemetry, Tracer tracer,
            Meter meter) {
        this.connectionFactory = connectionFactory;
        this.tracer = tracer;
        this.meter = meter;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return new TracingConnection(connectionFactory.createConnection(), openTelemetry, tracer, meter);
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        return new TracingConnection(connectionFactory.createConnection(userName, password), openTelemetry, tracer, meter);
    }

    @Override
    public JMSContext createContext() {
        return new TracingJMSContext(connectionFactory.createContext(), openTelemetry, tracer, meter);
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        return new TracingJMSContext(connectionFactory.createContext(userName, password), openTelemetry, tracer, meter);
    }

    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        return new TracingJMSContext(connectionFactory.createContext(userName, password, sessionMode), openTelemetry,
                tracer, meter);
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return new TracingJMSContext(connectionFactory.createContext(sessionMode), openTelemetry, tracer, meter);
    }
}
