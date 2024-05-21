package me.andidroid.artemis.opentelemetry.client;

import io.opentelemetry.api.trace.Tracer;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionConsumer;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.ServerSessionPool;
import jakarta.jms.Session;
import jakarta.jms.Topic;

public class TracingConnection implements Connection {
    private final Connection connection;
    private final Tracer tracer;
    private final boolean traceInLog;

    public TracingConnection(Connection connection, Tracer tracer) {
        this(connection, tracer, false);
    }

    public TracingConnection(Connection connection, Tracer tracer, boolean traceInLog) {
        this.connection = connection;
        this.tracer = tracer;
        this.traceInLog = traceInLog;
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return new TracingSession(connection.createSession(transacted, acknowledgeMode), tracer,
                traceInLog);
    }

    @Override
    public Session createSession(int sessionMode) throws JMSException {
        return new TracingSession(connection.createSession(sessionMode), tracer, traceInLog);
    }

    @Override
    public Session createSession() throws JMSException {
        return new TracingSession(connection.createSession(), tracer, traceInLog);
    }

    @Override
    public String getClientID() throws JMSException {
        return connection.getClientID();
    }

    @Override
    public void setClientID(String clientID) throws JMSException {
        connection.setClientID(clientID);
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return connection.getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return connection.getExceptionListener();
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        connection.setExceptionListener(listener);
    }

    @Override
    public void start() throws JMSException {
        connection.start();
    }

    @Override
    public void stop() throws JMSException {
        connection.stop();
    }

    @Override
    public void close() throws JMSException {
        connection.close();
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination,
            String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return connection.createConnectionConsumer(destination, messageSelector, sessionPool,
                maxMessages);
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName,
            String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return connection.createSharedConnectionConsumer(topic, subscriptionName, messageSelector,
                sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
            String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return connection.createDurableConnectionConsumer(topic, subscriptionName, messageSelector,
                sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic,
            String subscriptionName, String messageSelector, ServerSessionPool sessionPool,
            int maxMessages) throws JMSException {
        return connection.createSharedDurableConnectionConsumer(topic, subscriptionName,
                messageSelector, sessionPool, maxMessages);
    }

}