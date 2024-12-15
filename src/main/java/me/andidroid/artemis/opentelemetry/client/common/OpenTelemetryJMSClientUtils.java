package me.andidroid.artemis.opentelemetry.client.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractorBuilder;
// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
// import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.ContextCustomizer;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import io.smallrye.opentelemetry.implementation.exporters.traces.VertxSpanExporterProvider;
import jakarta.jms.Message;

public class OpenTelemetryJMSClientUtils {

    /**
     * Logging via slf4j api
     */
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(OpenTelemetryJMSClientUtils.class);

    private static final OpenTelemetry openTelemetry = createOpenTelemetry();

    public static final MessagingAttributesGetter<Message, Message> getter = new MessageMessagingAttributesGetter();

    private static InstrumenterBuilder<Message, Message> messageReceiveBuilder = createBuilder(
            MessageOperation.RECEIVE);
    private static InstrumenterBuilder<Message, Message> messagePublishBuilder = createBuilder(
            MessageOperation.PUBLISH);
    private static InstrumenterBuilder<Message, Message> messageProcessBuilder = createBuilder(
            MessageOperation.PROCESS);

    public OpenTelemetryJMSClientUtils() {

    }

    private static InstrumenterBuilder<Message, Message> createBuilder(
            MessageOperation messageOperation) {

        return createBuilder(getOpenTelemetry(), messageOperation);
    }

    private static InstrumenterBuilder<Message, Message> createBuilder(OpenTelemetry openTelemetry,
            MessageOperation messageOperation) {

        // GlobalOpenTelemetry.set();GlobalOpenTelemetry.get()

        String serviceName = "jms-client";
        // TODO: name???
        serviceName = "artemis_opentelemetry";

        InstrumenterBuilder<Message, Message> builder = Instrumenter.builder(openTelemetry,
                serviceName,
                MessagingSpanNameExtractor.create(getter, messageOperation));// new MessageSpanNameExtractor()

        // builder.addAttributesExtractors(new )

        builder.setSpanStatusExtractor(new SpanStatusExtractor<Message, Message>() {

            @Override
            public void extract(SpanStatusBuilder arg0, Message arg1, Message arg2, Throwable arg3) {
                if (arg3 == null) {
                    arg0.setStatus(StatusCode.OK);
                } else {
                    arg0.setStatus(StatusCode.ERROR);
                }

            }

        });

        List<String> capturedHeaders = List.of("traceid", "spanid", "clientid", "serverid", "traceflags", "tracestate");
        AttributesExtractor<Message, Message> messagingAttributesExtractor = MessagingAttributesExtractor
                .builder(getter, messageOperation).setCapturedHeaders(capturedHeaders).build();
        builder.addAttributesExtractor(messagingAttributesExtractor);

        builder.addSpanLinksExtractor(
                new PropagatorBasedSpanLinksExtractor<Message>(
                        /* GlobalOpenTelemetry.getPropagators().getTextMapPropagator() */ new MessageTextMapPropagator(),
                        new MessageTextMapGetter()));

        return builder;
    }

    public static OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    public static OpenTelemetry createOpenTelemetry() {

        LOGGER.debug("create open telemetry with text map propagator");
        OpenTelemetry openTelemetry = null;
        // OpenTelemetry openTelemetry = OpenTelemetry
        // .propagating(ContextPropagators.create(new MessageTextMapPropagator()));

        // // if (GlobalOpenTelemetry.get() instanceof OpenTelemetrySdk)
        // {
        // System.out.println("create new OpenTelemetrySdk from GlobalOpenTelemetry");
        // OpenTelemetrySdk old = (OpenTelemetrySdk) GlobalOpenTelemetry.get();

        // openTelemetry =
        // OpenTelemetrySdk.builder().setLoggerProvider(old.getSdkLoggerProvider())
        // .setMeterProvider(old.getSdkMeterProvider()).setTracerProvider(old.getSdkTracerProvider())
        // .setPropagators(ContextPropagators.create(new
        // MessageTextMapPropagator())).buildAndRegisterGlobal();

        // }

        OpenTelemetry openTelemetry2 = GlobalOpenTelemetry.get();
        if (openTelemetry2 instanceof OpenTelemetrySdk) {
            LOGGER.debug("create new OpenTelemetrySdk from GlobalOpenTelemetry");
            OpenTelemetrySdk old = (OpenTelemetrySdk) GlobalOpenTelemetry.get();

            openTelemetry = OpenTelemetrySdk.builder().setLoggerProvider(old.getSdkLoggerProvider())
                    .setMeterProvider(old.getSdkMeterProvider()).setTracerProvider(old.getSdkTracerProvider())
                    .setPropagators(ContextPropagators.create(new MessageTextMapPropagator()))
                    .buildAndRegisterGlobal();

        }

        // OpenTelemetrySdk.SetDefaultTextMapPropagator(new
        // CompositeTextMapPropagator(new TextMapPropagator[] {
        // new TraceContextPropagator(),
        // new BaggagePropagator(),
        // }));

        OpenTelemetry otel = new OpenTelemetry() {

            @Override
            public TracerProvider getTracerProvider() {
                return GlobalOpenTelemetry.get().getTracerProvider();
            }

            @Override
            public MeterProvider getMeterProvider() {
                return GlobalOpenTelemetry.get().getMeterProvider();
            }

            @Override
            public LoggerProvider getLogsBridge() {
                return GlobalOpenTelemetry.get().getLogsBridge();
            }

            @Override
            public ContextPropagators getPropagators() {
                return ContextPropagators.create(new MessageTextMapPropagator());
            }

        };
        LOGGER.debug("register otel");
        // GlobalOpenTelemetry.set(otel);

        String otelEndpoint = "http://localhost:4317";

        String serviceName = "jms-client";
        // TODO: name???
        serviceName = "artemis_opentelemetry";

        Resource resource = Resource.getDefault()
                .merge(Resource.create(
                        Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName)));

        /*
         * Tracing
         */
        LOGGER.debug("create vertx span exporter");

        ConfigProperties configs = DefaultConfigProperties.create(new HashMap<>());
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor
                        .builder(new VertxSpanExporterProvider().createExporter(configs)).build())
                .setResource(resource)
                .build();

        otel = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)

                .setPropagators(ContextPropagators
                        .create(new MessageTextMapPropagator()))
                .build();

        return otel;

        // return openTelemetry;
    }

    public static Instrumenter<Message, Message> getConsumerInstrumenter() {

        return messageReceiveBuilder.buildConsumerInstrumenter(new MessageTextMapGetter());
    }

    public static Instrumenter<Message, Message> getProducerInstrumenter() {

        return messagePublishBuilder.buildProducerInstrumenter(new MessageTextMapSetter());
    }

    public static Instrumenter<Message, Message> getProcessProducerInstrumenter() {

        return messageProcessBuilder.buildProducerInstrumenter(new MessageTextMapSetter());
    }

    public static Instrumenter<Message, Message> getConsumerInstrumenter(OpenTelemetry openTelemetry) {

        return createBuilder(openTelemetry, MessageOperation.RECEIVE)
                .buildConsumerInstrumenter(new MessageTextMapGetter());
    }

    public static Instrumenter<Message, Message> getProducerInstrumenter(OpenTelemetry openTelemetry) {

        return createBuilder(openTelemetry, MessageOperation.PUBLISH)
                .buildProducerInstrumenter(new MessageTextMapSetter());
    }

    public static Instrumenter<Message, Message> getProcessProducerInstrumenter(OpenTelemetry openTelemetry) {

        return createBuilder(openTelemetry, MessageOperation.PROCESS)
                .buildProducerInstrumenter(new MessageTextMapSetter());
    }

}
