package io.nextpos.einvoice.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Duration property conversion:
 *
 * https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-conversion
 */
@Component
@ConfigurationProperties(prefix = "scheduler")
@Data
public class SchedulerConfigProperties {

    private Duration heartbeatInterval;

    private Duration processMessageInterval;

    private Duration updateInvoiceStatusInterval;

    private Duration deleteQueueInterval;
}
