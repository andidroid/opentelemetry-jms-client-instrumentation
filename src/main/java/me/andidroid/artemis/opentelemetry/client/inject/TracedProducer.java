package me.andidroid.artemis.opentelemetry.client.inject;

import io.opentelemetry.api.trace.Tracer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import me.andidroid.artemis.opentelemetry.client.TracingJMSContext;

@ApplicationScoped
public class TracedProducer {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(TracedProducer.class);

    @PostConstruct
    public void initialize() {
        LOGGER.info("TracedProducer.initialize()");
    }

    @Inject
    private JMSContext jmsContext;
    @Inject
    private Tracer tracer;

    @Produces
    @Traced
    public JMSContext createJMSContext() {
        LOGGER.info("TracedProducer.createJMSContext()");
        return new TracingJMSContext(jmsContext, tracer);
    }
}
