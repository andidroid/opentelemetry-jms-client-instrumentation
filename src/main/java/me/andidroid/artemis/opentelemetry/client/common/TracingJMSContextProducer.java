package me.andidroid.artemis.opentelemetry.client.common;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import me.andidroid.artemis.opentelemetry.client.TracingJMSContext;

// @ApplicationScoped
public class TracingJMSContextProducer {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracingJMSContextProducer.class);

    // FIXME: cant inject JMSContext here, because TracingJMSContext extends
    // JMSContext -> duplicate/ambiguous injection producer found
    // @Inject
    // private JMSContext jmsContext;

    @Resource(name = "jms/QueueConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Inject
    private Tracer tracer;
    @Inject
    private Meter meter;
    @Inject
    private OpenTelemetry openTelemetry;

    // @Produces
    public TracingJMSContext getTracingJMSContext() {
        LOGGER.info("produce TracingJMSContext");
        JMSContext jmsContext = connectionFactory.createContext();
        return new TracingJMSContext(jmsContext, openTelemetry, tracer, meter);
    }

}
