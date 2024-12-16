package me.andidroid.artemis.opentelemetry.client;

import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import java.io.Serializable;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 * Tracing decorator for JMS {@code jakarta.jms.JMSContext}.
 */
public class TracingJMSContext implements JMSContext {

    private final JMSContext jmsContext;
    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public TracingJMSContext(JMSContext jmsContext, OpenTelemetry openTelemetry, Tracer tracer) {
        this.jmsContext = jmsContext;
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return new TracingJMSContext(jmsContext.createContext(sessionMode), openTelemetry, tracer);
    }

    @Override
    public JMSProducer createProducer() {
        return new TracingJMSProducer(jmsContext.createProducer(), jmsContext, openTelemetry, tracer);
    }

    @Override
    public String getClientID() {
        return jmsContext.getClientID();
    }

    @Override
    public void setClientID(String clientID) {
        jmsContext.setClientID(clientID);
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return jmsContext.getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() {
        return jmsContext.getExceptionListener();
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) {
        jmsContext.setExceptionListener(listener);
    }

    @Override
    public void start() {
        jmsContext.start();
    }

    @Override
    public void stop() {
        jmsContext.stop();
    }

    @Override
    public boolean getAutoStart() {
        return jmsContext.getAutoStart();
    }

    @Override
    public void setAutoStart(boolean autoStart) {
        jmsContext.setAutoStart(autoStart);
    }

    @Override
    public void close() {
        jmsContext.close();
    }

    @Override
    public BytesMessage createBytesMessage() {
        return jmsContext.createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() {
        return jmsContext.createMapMessage();
    }

    @Override
    public Message createMessage() {
        return jmsContext.createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() {
        return jmsContext.createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) {
        return jmsContext.createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() {
        return jmsContext.createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() {
        return jmsContext.createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) {
        return jmsContext.createTextMessage(text);
    }

    @Override
    public boolean getTransacted() {
        return jmsContext.getTransacted();
    }

    @Override
    public int getSessionMode() {
        return jmsContext.getSessionMode();
    }

    @Override
    public void commit() {
        jmsContext.commit();
    }

    @Override
    public void rollback() {
        jmsContext.rollback();
    }

    @Override
    public void recover() {
        jmsContext.recover();
    }

    @Override
    public JMSConsumer createConsumer(Destination destination) {
        return new TracingJMSConsumer(jmsContext.createConsumer(destination), openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) {
        return new TracingJMSConsumer(jmsContext.createConsumer(destination, messageSelector), openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
        return new TracingJMSConsumer(jmsContext.createConsumer(destination, messageSelector, noLocal), openTelemetry,
                tracer);
    }

    @Override
    public Queue createQueue(String queueName) {
        return jmsContext.createQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) {
        return jmsContext.createTopic(topicName);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) {
        return new TracingJMSConsumer(jmsContext.createDurableConsumer(topic, name), openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
        return new TracingJMSConsumer(jmsContext.createDurableConsumer(topic, name, messageSelector, noLocal),
                openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
        return new TracingJMSConsumer(jmsContext.createSharedDurableConsumer(topic, name), openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
        return new TracingJMSConsumer(jmsContext.createSharedDurableConsumer(topic, name, messageSelector),
                openTelemetry, tracer);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
        return new TracingJMSConsumer(jmsContext.createSharedConsumer(topic, sharedSubscriptionName), openTelemetry,
                tracer);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
        return new TracingJMSConsumer(jmsContext.createSharedConsumer(topic, sharedSubscriptionName, messageSelector),
                openTelemetry, tracer);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) {
        return jmsContext.createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) {
        return jmsContext.createBrowser(queue, messageSelector);
    }

    @Override
    public TemporaryQueue createTemporaryQueue() {
        return jmsContext.createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() {
        return jmsContext.createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) {
        jmsContext.unsubscribe(name);
    }

    @Override
    public void acknowledge() {
        jmsContext.acknowledge();
    }
}