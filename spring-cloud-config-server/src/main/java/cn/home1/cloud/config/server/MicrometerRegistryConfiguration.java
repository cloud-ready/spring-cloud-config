package cn.home1.cloud.config.server;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerRegistryConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            // .namingConvention(this.prefixConvention())
            .commonTags("app.name", this.applicationName);
    }

    @Bean
    TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    // @Bean
    // io.micrometer.graphite.GraphiteNamingConvention prefixConvention() {
    //     // see: [Root namespace pollution](https://blog.frankel.ch/metrics-spring-boot-2/1/)
    //     return new io.micrometer.graphite.GraphiteNamingConvention() {
    //
    //         @Override
    //         public String name(
    //         String name,
    //         io.micrometer.core.instrument.Meter.Type type,
    //         @io.micrometer.core.lang.Nullable String baseUnit) {
    //             return MicrometerRegistryConfiguration.this.applicationName + "." + super.name(name, type, baseUnit);
    //         }
    //
    //     };
    // }
}
